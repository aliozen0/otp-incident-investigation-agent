package com.example.otpsentinel.config;

import com.example.otpsentinel.adapters.persistence.JdbcAuditEventRepository;
import com.example.otpsentinel.adapters.persistence.JdbcIncidentDraftRepository;
import com.example.otpsentinel.adapters.persistence.JdbcInvestigationRepository;
import com.example.otpsentinel.agent.AgentTools;
import com.example.otpsentinel.agent.EvidenceCollector;
import com.example.otpsentinel.agent.IncidentAnalysisAiService;
import com.example.otpsentinel.agent.SessionChatMemoryStore;
import com.example.otpsentinel.agent.ToolBudgetGuard;
import com.example.otpsentinel.application.IncidentInvestigationService;
import com.example.otpsentinel.application.InvestigationRequest;
import com.example.otpsentinel.domain.AuditEvent;
import com.example.otpsentinel.domain.AuditEventType;
import com.example.otpsentinel.domain.IncidentDraft;
import com.example.otpsentinel.domain.IncidentDraftId;
import com.example.otpsentinel.domain.IncidentDraftStatus;
import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.domain.InvestigationId;
import com.example.otpsentinel.domain.InvestigationPhase;
import com.example.otpsentinel.domain.Severity;
import com.example.otpsentinel.domain.TimeWindow;
import com.example.otpsentinel.domain.ValidationStatus;
import com.example.otpsentinel.rag.KnowledgeSearchPort;
import com.example.otpsentinel.tools.ErrorDistributionTool;
import com.example.otpsentinel.tools.OtpMetricsTool;
import com.example.otpsentinel.tools.ProviderHealthTool;
import com.example.otpsentinel.tools.QueueHealthTool;
import com.example.otpsentinel.tools.RecentChangesTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Composition root for one investigation run per request (docs/13 REST/approval wiring). Audits
 * {@link AuditEventType#REQUEST_ACCEPTED} and {@link AuditEventType#TIME_WINDOW_RESOLVED} itself
 * (they happen before/outside {@link IncidentInvestigationService#investigate}), which audits the
 * remaining LLM/validation events via its audit-aware overload.
 */
@Service
public class InvestigationOrchestrator {

  private static final String ACTOR = "demo-operator";
  // v2: analysis system prompt rewritten (two-phase tool discipline, Turkish output contract,
  // explicit enum/number rules, JSON-only answer). Audit rows keep the version that produced them.
  private static final String PROMPT_VERSION = "v2";
  private static final String SCHEMA_VERSION = "v1";

  private final JdbcInvestigationRepository investigationRepository;
  private final JdbcIncidentDraftRepository incidentDraftRepository;
  private final JdbcAuditEventRepository auditEventRepository;
  private final java.util.function.Function<String, ChatModel> chatModelFactory;
  private final KnowledgeSearchPort knowledgeSearchPort;
  private final OtpMetricsTool otpMetricsTool;
  private final ErrorDistributionTool errorDistributionTool;
  private final QueueHealthTool queueHealthTool;
  private final ProviderHealthTool providerHealthTool;
  private final RecentChangesTool recentChangesTool;
  private final int maxToolCalls;
  private final int quickModeMaxToolCalls;
  private final Duration toolTimeout;
  private final int toolRetryCount;
  private final int maxRepairAttempts;
  private final String fallbackModelId;
  private final SessionChatMemoryStore sessionChatMemoryStore;

  public InvestigationOrchestrator(
      JdbcInvestigationRepository investigationRepository,
      JdbcIncidentDraftRepository incidentDraftRepository,
      JdbcAuditEventRepository auditEventRepository,
      java.util.function.Function<String, ChatModel> chatModelFactory,
      KnowledgeSearchPort knowledgeSearchPort,
      OtpMetricsTool otpMetricsTool,
      ErrorDistributionTool errorDistributionTool,
      QueueHealthTool queueHealthTool,
      ProviderHealthTool providerHealthTool,
      RecentChangesTool recentChangesTool,
      @Value("${otp-sentinel.ai.max-tool-calls:8}") int maxToolCalls,
      @Value("${otp-sentinel.ai.quick-mode-max-tool-calls:5}") int quickModeMaxToolCalls,
      @Value("${otp-sentinel.tool.timeout-millis:2000}") long toolTimeoutMillis,
      @Value("${otp-sentinel.tool.retry-count:1}") int toolRetryCount,
      @Value("${otp-sentinel.ai.max-repair-attempts:1}") int maxRepairAttempts,
      @Value("${otp-sentinel.ai.chat-memory-max-messages:40}") int chatMemoryMaxMessages,
      @Value("${otp-sentinel.ai.chat-memory-max-sessions:1000}") int chatMemoryMaxSessions,
      @Value("${otp-sentinel.ai.fallback-model:meta/llama-3.1-8b-instruct}") String fallbackModelId) {
    this.investigationRepository = investigationRepository;
    this.incidentDraftRepository = incidentDraftRepository;
    this.auditEventRepository = auditEventRepository;
    this.chatModelFactory = chatModelFactory;
    this.knowledgeSearchPort = knowledgeSearchPort;
    this.otpMetricsTool = otpMetricsTool;
    this.errorDistributionTool = errorDistributionTool;
    this.queueHealthTool = queueHealthTool;
    this.providerHealthTool = providerHealthTool;
    this.recentChangesTool = recentChangesTool;
    this.maxToolCalls = maxToolCalls;
    this.quickModeMaxToolCalls = quickModeMaxToolCalls;
    this.toolTimeout = Duration.ofMillis(toolTimeoutMillis);
    this.toolRetryCount = toolRetryCount;
    this.maxRepairAttempts = maxRepairAttempts;
    this.fallbackModelId = fallbackModelId;
    this.sessionChatMemoryStore =
        new SessionChatMemoryStore(chatMemoryMaxMessages, chatMemoryMaxSessions);
  }

  public Investigation runInvestigation(
      String question,
      TimeWindow resolvedTimeWindow,
      String correlationId,
      String sessionId,
      String modelId,
      com.example.otpsentinel.agent.InvestigationMode mode) {
    Investigation outcome =
        attemptInvestigation(question, resolvedTimeWindow, correlationId, sessionId, modelId, mode, false);
    // A model that cannot hold the output schema must not cost the operator their answer: the
    // evidence sweep is deterministic, so re-running it once on the fallback model turns a
    // "structured output invalid" dead end into a real analysis. Only that failure is retried.
    if (isStructuredOutputFailure(outcome)
        && fallbackModelId != null
        && !fallbackModelId.isBlank()
        && !fallbackModelId.equals(modelId)) {
      // Fresh memory on the retry: the failed attempt left its half-finished transcript in the
      // session, and replaying that into another model makes it answer without calling any tool.
      outcome =
          attemptInvestigation(
              question, resolvedTimeWindow, correlationId, sessionId, fallbackModelId, mode, true);
    }
    investigationRepository.save(outcome);
    return outcome;
  }

  private static boolean isStructuredOutputFailure(Investigation investigation) {
    return investigation.resultStatus() == com.example.otpsentinel.domain.InvestigationStatus.FAILED
        && investigation.validationReport() != null
        && investigation.validationReport().warnings().stream()
            .anyMatch(warning -> warning.contains("structured output invalid"));
  }

  private Investigation attemptInvestigation(
      String question,
      TimeWindow resolvedTimeWindow,
      String correlationId,
      String sessionId,
      String modelId,
      com.example.otpsentinel.agent.InvestigationMode mode,
      boolean isolatedMemory) {
    Investigation investigation =
        Investigation.receive(
            question, resolvedTimeWindow, PROMPT_VERSION, SCHEMA_VERSION, sessionId);
    audit(
        AuditEventType.REQUEST_ACCEPTED,
        investigation.id(),
        null,
        correlationId,
        "question accepted");
    audit(
        AuditEventType.TIME_WINDOW_RESOLVED,
        investigation.id(),
        null,
        correlationId,
        resolvedTimeWindow.startAt() + "/" + resolvedTimeWindow.endAt());

    // Quick mode = same live-signal tools, no RAG lookup (docs/16 ADR-017 / M11 finding 3). The
    // knowledge tool short-circuits without consuming the budget, so the quick budget covers
    // exactly the five non-RAG tools and the run still finishes with a complete result.
    boolean ragEnabled = mode != com.example.otpsentinel.agent.InvestigationMode.QUICK;
    int effectiveMaxToolCalls = ragEnabled ? maxToolCalls : quickModeMaxToolCalls;
    ToolBudgetGuard guard = new ToolBudgetGuard(effectiveMaxToolCalls, toolTimeout, toolRetryCount);
    EvidenceCollector collector =
        new EvidenceCollector(investigation, auditEventRepository, correlationId);
    AgentTools tools =
        new AgentTools(
            otpMetricsTool,
            errorDistributionTool,
            queueHealthTool,
            providerHealthTool,
            recentChangesTool,
            knowledgeSearchPort,
            guard,
            collector,
            ragEnabled);
    ChatModel chatModel = chatModelFactory.apply(modelId);
    String memoryId =
        (isolatedMemory || sessionId == null || sessionId.isBlank())
            ? investigation.id().toString()
            : sessionId;
    IncidentAnalysisAiService aiService =
        AiServices.builder(IncidentAnalysisAiService.class)
            .chatModel(chatModel)
            .tools(tools)
            .chatMemoryProvider(id -> sessionChatMemoryStore.get((String) id))
            .build();

    return new IncidentInvestigationService(maxRepairAttempts)
        .investigate(
            new InvestigationRequest(question, resolvedTimeWindow, PROMPT_VERSION, SCHEMA_VERSION),
            investigation,
            aiService,
            guard,
            collector,
            auditEventRepository,
            correlationId,
            memoryId);
  }

  public Optional<Investigation> findInvestigation(InvestigationId id) {
    return investigationRepository.findById(id);
  }

  public List<Investigation> findBySessionId(String sessionId) {
    return investigationRepository.findBySessionId(sessionId);
  }

  public record IncidentDraftPreview(
      String title,
      Severity severity,
      String summary,
      int evidenceCount,
      List<String> recommendedChecks,
      boolean requiresExplicitApproval) {}

  public IncidentDraftPreview previewIncidentDraft(
      InvestigationId investigationId, String correlationId) {
    Investigation investigation =
        investigationRepository
            .findById(investigationId)
            .orElseThrow(
                () ->
                    new InvestigationNotFoundException(
                        "investigation not found: " + investigationId));
    requireReadyForDecision(investigation);
    IncidentDraftPreview preview = buildPreview(investigation);
    audit(AuditEventType.PREVIEW_GENERATED, investigationId, null, correlationId, "generated");
    return preview;
  }

  public record DecisionOutcome(
      IncidentDraftId incidentDraftId,
      String externalIncidentId,
      IncidentDraftStatus status,
      boolean idempotentReplay) {}

  public DecisionOutcome decide(
      InvestigationId investigationId,
      String decision,
      String reason,
      String idempotencyKey,
      String correlationId) {
    if (!"APPROVE".equals(decision) && !"REJECT".equals(decision)) {
      throw new IllegalArgumentException("decision must be APPROVE or REJECT");
    }
    Investigation investigation =
        investigationRepository
            .findById(investigationId)
            .orElseThrow(
                () ->
                    new InvestigationNotFoundException(
                        "investigation not found: " + investigationId));
    requireReadyForDecision(investigation);
    IncidentDraftPreview preview = buildPreview(investigation);
    IncidentDraft draft =
        IncidentDraft.preview(investigationId, renderPayload(preview), idempotencyKey);

    try {
      if ("APPROVE".equals(decision)) {
        draft.approve(ACTOR);
        draft.create(generateExternalIncidentId());
      } else {
        draft.reject(ACTOR, reason);
      }
      incidentDraftRepository.save(draft);
      audit(AuditEventType.APPROVAL_DECIDED, investigationId, draft.id(), correlationId, decision);
      if ("APPROVE".equals(decision)) {
        audit(
            AuditEventType.INCIDENT_CREATED,
            investigationId,
            draft.id(),
            correlationId,
            draft.externalIncidentId());
      }
      return new DecisionOutcome(draft.id(), draft.externalIncidentId(), draft.status(), false);
    } catch (DataIntegrityViolationException replay) {
      IncidentDraft existing =
          incidentDraftRepository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> replay);
      return new DecisionOutcome(
          existing.id(), existing.externalIncidentId(), existing.status(), true);
    }
  }

  private static void requireReadyForDecision(Investigation investigation) {
    if (investigation.phase() != InvestigationPhase.COMPLETED
        || investigation.validationReport() == null
        || investigation.validationReport().status() != ValidationStatus.PASSED) {
      throw new InvestigationNotActionableException(
          "investigation is not ready for a decision: " + investigation.id());
    }
  }

  private IncidentDraftPreview buildPreview(Investigation investigation) {
    String title = "[" + investigation.severity() + "] " + investigation.question();
    List<String> checks =
        investigation.hypotheses().stream()
            .flatMap(h -> h.verificationSteps().stream())
            .distinct()
            .toList();
    return new IncidentDraftPreview(
        title,
        investigation.severity(),
        summaryOf(investigation),
        investigation.evidence().size(),
        checks,
        true);
  }

  private static String summaryOf(Investigation investigation) {
    return investigation.resultStatus() + " severity=" + investigation.severity();
  }

  private static String renderPayload(IncidentDraftPreview preview) {
    return preview.title()
        + " | "
        + preview.summary()
        + " | evidenceCount="
        + preview.evidenceCount();
  }

  private static String generateExternalIncidentId() {
    return "DEMO-INC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  private void audit(
      AuditEventType type,
      InvestigationId investigationId,
      IncidentDraftId draftId,
      String correlationId,
      String result) {
    auditEventRepository.append(
        AuditEvent.of(
            ACTOR, type, investigationId, draftId, correlationId, result, PROMPT_VERSION));
  }
}
