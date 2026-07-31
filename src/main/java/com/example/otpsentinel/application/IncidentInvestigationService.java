package com.example.otpsentinel.application;

import com.example.otpsentinel.agent.DuplicateToolCallException;
import com.example.otpsentinel.agent.EvidenceCollector;
import com.example.otpsentinel.agent.IncidentAnalysisAiService;
import com.example.otpsentinel.agent.IncidentAnalysisResult;
import com.example.otpsentinel.agent.KnowledgeReference;
import com.example.otpsentinel.agent.ToolBudgetExceededException;
import com.example.otpsentinel.agent.ToolBudgetGuard;
import com.example.otpsentinel.domain.AuditEvent;
import com.example.otpsentinel.domain.AuditEventRepository;
import com.example.otpsentinel.domain.AuditEventType;
import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.domain.InvestigationStatus;
import com.example.otpsentinel.domain.ValidationReport;
import com.example.otpsentinel.domain.ValidationStatus;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Drives an {@link Investigation} lifecycle. Tool selection and hypothesis generation remain
 * agentic; lifecycle transitions, repair limit, and citation validation are deterministic Java
 * code. REST and persistence wiring are intentionally deferred to M7.
 */
public final class IncidentInvestigationService {

  private final int maxRepairAttempts;
  private final ClaimValidator claimValidator = new ClaimValidator();

  public IncidentInvestigationService(int maxRepairAttempts) {
    if (maxRepairAttempts < 0 || maxRepairAttempts > 1) {
      throw new IllegalArgumentException("maxRepairAttempts must be 0 or 1");
    }
    this.maxRepairAttempts = maxRepairAttempts;
  }

  public Investigation investigate(
      InvestigationRequest request,
      Investigation investigation,
      IncidentAnalysisAiService aiService,
      ToolBudgetGuard guard,
      EvidenceCollector collector) {
    return investigate(request, investigation, aiService, guard, collector, null, null);
  }

  public Investigation investigate(
      InvestigationRequest request,
      Investigation investigation,
      IncidentAnalysisAiService aiService,
      ToolBudgetGuard guard,
      EvidenceCollector collector,
      AuditEventRepository auditEventRepository,
      String correlationId) {
    Objects.requireNonNull(request, "request must not be null");
    Objects.requireNonNull(investigation, "investigation must not be null");
    Objects.requireNonNull(aiService, "aiService must not be null");
    Objects.requireNonNull(guard, "guard must not be null");
    Objects.requireNonNull(collector, "collector must not be null");
    requireMatchingRequest(request, investigation);

    investigation.startCollectingEvidence();
    AnalysisAttempt attempt = callWithRepair(aiService, request);
    if (attempt.policyLimitReached() || guard.policyLimitReached()) {
      investigation.partial(
          InvestigationStatus.PARTIAL_ANALYSIS,
          ValidationReport.passed(List.of("tool policy limit reached; analysis is partial")));
      return investigation;
    }
    if (attempt.analysis() == null) {
      investigation.fail(
          "structured output invalid after " + maxRepairAttempts + " repair attempt(s)");
      return investigation;
    }
    audit(auditEventRepository, AuditEventType.LLM_COMPLETED, investigation, correlationId, "ok");

    IncidentAnalysisResult analysis = attempt.analysis();
    investigation.startGeneratingAnalysis();
    ValidationReport claimReport = claimValidator.validate(analysis, investigation.evidence());
    if (claimReport.status() == ValidationStatus.FAILED) {
      audit(
          auditEventRepository,
          AuditEventType.VALIDATION_FAILED,
          investigation,
          correlationId,
          claimReport.warnings().getFirst());
      investigation.fail(claimReport.warnings().getFirst());
      return investigation;
    }

    List<String> acceptedKnowledgeReferences =
        filterKnownKnowledgeReferences(analysis.knowledgeReferences(), collector);
    try {
      investigation.proposeAnalysis(
          analysis.severity(),
          analysis.hypotheses(),
          analysis.recommendedActions(),
          acceptedKnowledgeReferences,
          analysis.confidence());
      investigation.startValidating();
      finish(investigation, analysis, claimReport.warnings());
      audit(
          auditEventRepository,
          AuditEventType.VALIDATION_PASSED,
          investigation,
          correlationId,
          "ok");
    } catch (IllegalArgumentException | IllegalStateException e) {
      audit(
          auditEventRepository,
          AuditEventType.VALIDATION_FAILED,
          investigation,
          correlationId,
          "analysis rejected by deterministic validation");
      investigation.fail("analysis rejected by deterministic validation");
    }
    return investigation;
  }

  private static void audit(
      AuditEventRepository repo,
      AuditEventType type,
      Investigation investigation,
      String correlationId,
      String result) {
    if (repo == null || correlationId == null) {
      return;
    }
    repo.append(
        AuditEvent.of(
            "system",
            type,
            investigation.id(),
            null,
            correlationId,
            result,
            investigation.promptVersion()));
  }

  private AnalysisAttempt callWithRepair(
      IncidentAnalysisAiService aiService, InvestigationRequest request) {
    String timeWindow =
        request.resolvedTimeWindow().startAt() + "/" + request.resolvedTimeWindow().endAt();
    for (int attempt = 0; attempt <= maxRepairAttempts; attempt++) {
      try {
        return AnalysisAttempt.success(aiService.analyze(request.question(), timeWindow));
      } catch (RuntimeException failure) {
        if (causedByPolicyLimit(failure)) {
          return AnalysisAttempt.policyLimit();
        }
        if (attempt == maxRepairAttempts) {
          return AnalysisAttempt.invalid();
        }
      }
    }
    return AnalysisAttempt.invalid();
  }

  private static boolean causedByPolicyLimit(Throwable failure) {
    for (Throwable current = failure; current != null; current = current.getCause()) {
      if (current instanceof ToolBudgetExceededException
          || current instanceof DuplicateToolCallException) {
        return true;
      }
    }
    return false;
  }

  private static List<String> filterKnownKnowledgeReferences(
      List<KnowledgeReference> requested, EvidenceCollector collector) {
    Set<KnowledgeReference> known = new HashSet<>(collector.knownKnowledgeReferences());
    return requested.stream()
        .filter(known::contains)
        .map(KnowledgeReference::documentId)
        .distinct()
        .toList();
  }

  private static void finish(
      Investigation investigation, IncidentAnalysisResult analysis, List<String> claimWarnings) {
    if (analysis.status() == InvestigationStatus.FAILED) {
      investigation.fail("model reported failed analysis");
    } else if (analysis.status() == InvestigationStatus.PARTIAL_ANALYSIS) {
      investigation.partial(
          InvestigationStatus.PARTIAL_ANALYSIS,
          ValidationReport.passed(prepend("analysis is partial", claimWarnings)));
    } else {
      investigation.complete(analysis.status(), ValidationReport.passed(claimWarnings));
    }
  }

  private static List<String> prepend(String first, List<String> rest) {
    return Stream.concat(Stream.of(first), rest.stream()).toList();
  }

  private static void requireMatchingRequest(
      InvestigationRequest request, Investigation investigation) {
    if (!request.question().equals(investigation.question())
        || !request.resolvedTimeWindow().equals(investigation.resolvedTimeWindow())
        || !request.promptVersion().equals(investigation.promptVersion())
        || !request.schemaVersion().equals(investigation.schemaVersion())) {
      throw new IllegalArgumentException("request does not match investigation");
    }
  }

  private record AnalysisAttempt(IncidentAnalysisResult analysis, boolean policyLimitReached) {

    private static AnalysisAttempt success(IncidentAnalysisResult analysis) {
      return new AnalysisAttempt(Objects.requireNonNull(analysis), false);
    }

    private static AnalysisAttempt invalid() {
      return new AnalysisAttempt(null, false);
    }

    private static AnalysisAttempt policyLimit() {
      return new AnalysisAttempt(null, true);
    }
  }
}
