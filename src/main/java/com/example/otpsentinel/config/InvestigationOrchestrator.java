package com.example.otpsentinel.config;

import com.example.otpsentinel.adapters.persistence.JdbcAuditEventRepository;
import com.example.otpsentinel.adapters.persistence.JdbcIncidentDraftRepository;
import com.example.otpsentinel.adapters.persistence.JdbcInvestigationRepository;
import com.example.otpsentinel.agent.AgentTools;
import com.example.otpsentinel.agent.EvidenceCollector;
import com.example.otpsentinel.agent.IncidentAnalysisAiService;
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
import com.example.otpsentinel.tools.fixtures.FixtureErrorDistributionTool;
import com.example.otpsentinel.tools.fixtures.FixtureOtpMetricsTool;
import com.example.otpsentinel.tools.fixtures.FixtureProviderHealthTool;
import com.example.otpsentinel.tools.fixtures.FixtureQueueHealthTool;
import com.example.otpsentinel.tools.fixtures.FixtureRecentChangesTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
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
  private static final String PROMPT_VERSION = "v1";
  private static final String SCHEMA_VERSION = "v1";

  private final JdbcInvestigationRepository investigationRepository;
  private final JdbcIncidentDraftRepository incidentDraftRepository;
  private final JdbcAuditEventRepository auditEventRepository;
  private final Supplier<ChatModel> chatModelFactory;
  private final KnowledgeSearchPort knowledgeSearchPort;
  private final FixtureOtpMetricsTool otpMetricsTool;
  private final FixtureErrorDistributionTool errorDistributionTool;
  private final FixtureQueueHealthTool queueHealthTool;
  private final FixtureProviderHealthTool providerHealthTool;
  private final FixtureRecentChangesTool recentChangesTool;
  private final int maxToolCalls;
  private final Duration toolTimeout;
  private final int toolRetryCount;
  private final int maxRepairAttempts;

  public InvestigationOrchestrator(
      JdbcInvestigationRepository investigationRepository,
      JdbcIncidentDraftRepository incidentDraftRepository,
      JdbcAuditEventRepository auditEventRepository,
      Supplier<ChatModel> chatModelFactory,
      KnowledgeSearchPort knowledgeSearchPort,
      FixtureOtpMetricsTool otpMetricsTool,
      FixtureErrorDistributionTool errorDistributionTool,
      FixtureQueueHealthTool queueHealthTool,
      FixtureProviderHealthTool providerHealthTool,
      FixtureRecentChangesTool recentChangesTool,
      @Value("${otp-sentinel.ai.max-tool-calls:8}") int maxToolCalls,
      @Value("${otp-sentinel.tool.timeout-millis:2000}") long toolTimeoutMillis,
      @Value("${otp-sentinel.tool.retry-count:1}") int toolRetryCount,
      @Value("${otp-sentinel.ai.max-repair-attempts:1}") int maxRepairAttempts) {
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
    this.toolTimeout = Duration.ofMillis(toolTimeoutMillis);
    this.toolRetryCount = toolRetryCount;
    this.maxRepairAttempts = maxRepairAttempts;
  }

  public Investigation runInvestigation(
      String question, TimeWindow resolvedTimeWindow, String correlationId) {
    Investigation investigation =
        Investigation.receive(question, resolvedTimeWindow, PROMPT_VERSION, SCHEMA_VERSION);
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

    ToolBudgetGuard guard = new ToolBudgetGuard(maxToolCalls, toolTimeout, toolRetryCount);
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
            collector);
    ChatModel chatModel = chatModelFactory.get();
    IncidentAnalysisAiService aiService =
        AiServices.builder(IncidentAnalysisAiService.class)
            .chatModel(chatModel)
            .tools(tools)
            .build();

    Investigation outcome =
        new IncidentInvestigationService(maxRepairAttempts)
            .investigate(
                new InvestigationRequest(
                    question, resolvedTimeWindow, PROMPT_VERSION, SCHEMA_VERSION),
                investigation,
                aiService,
                guard,
                collector,
                auditEventRepository,
                correlationId);
    investigationRepository.save(outcome);
    return outcome;
  }

  public Optional<Investigation> findInvestigation(InvestigationId id) {
    return investigationRepository.findById(id);
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
