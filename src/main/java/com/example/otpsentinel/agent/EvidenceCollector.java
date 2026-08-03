package com.example.otpsentinel.agent;

import com.example.otpsentinel.domain.AuditEvent;
import com.example.otpsentinel.domain.AuditEventRepository;
import com.example.otpsentinel.domain.AuditEventType;
import com.example.otpsentinel.domain.Evidence;
import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.domain.KnowledgeCitation;
import com.example.otpsentinel.rag.ContentSanitizer;
import com.example.otpsentinel.rag.KnowledgeSearchResult;
import com.example.otpsentinel.tools.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Turns raw {@link ToolResult}s into application-minted {@link Evidence} (ADR-008: the model never
 * mints an id, it only cites one it was shown). One instance per {@link Investigation}.
 */
public final class EvidenceCollector {

  private final Investigation investigation;
  private final AuditEventRepository auditEventRepository;
  private final String correlationId;
  private final ContentSanitizer contentSanitizer = new ContentSanitizer();
  private final List<KnowledgeReference> knownKnowledgeReferences = new ArrayList<>();
  private final List<KnowledgeSearchResult> knownKnowledgeResults = new ArrayList<>();

  public EvidenceCollector(Investigation investigation) {
    this(investigation, null);
  }

  /**
   * M6: when {@code auditEventRepository} is given, retrieved knowledge content carrying
   * ContentSanitizer's instruction-pattern signal is audited as {@link
   * AuditEventType#PROMPT_INJECTION_SIGNAL} (docs/12 "Ignore embedded instruction"). This only
   * records a security signal; it never changes tool policy or the investigation outcome.
   */
  public EvidenceCollector(Investigation investigation, AuditEventRepository auditEventRepository) {
    this(investigation, auditEventRepository, null);
  }

  public EvidenceCollector(
      Investigation investigation,
      AuditEventRepository auditEventRepository,
      String correlationId) {
    this.investigation = Objects.requireNonNull(investigation, "investigation must not be null");
    this.auditEventRepository = auditEventRepository;
    this.correlationId = correlationId;
  }

  public <T> AgentToolResponse<T> collect(ToolResult<T> result) {
    investigation.recordToolExecution(result.executionId());
    auditToolCalled(result.toolName());
    if (result.status() != ToolStatus.SUCCESS) {
      auditToolOutcome(AuditEventType.TOOL_FAILED, result.toolName(), result.error().message());
      // A failed tool is itself an observation the analysis legitimately reasons about ("provider
      // health was unavailable"). Minting it as evidence gives the model a real id to cite instead
      // of inventing one, which the evidence check would reject and fail the investigation.
      String id = errorEvidenceId(result.toolName());
      investigation.addEvidence(
          new Evidence(
              id,
              "TOOL_RESULT",
              result.toolName(),
              result.toolName() + " returned no data: " + result.error().message(),
              result.observedAt(),
              null,
              null,
              null));
      return new AgentToolResponse<>(
          result.status(), null, List.of(id), result.error().message());
    }
    List<String> ids = mintEvidence(result);
    auditToolOutcome(AuditEventType.TOOL_COMPLETED, result.toolName(), "ids=" + ids);
    return new AgentToolResponse<>(result.status(), result.data(), ids, null);
  }

  private void auditToolCalled(String toolName) {
    if (auditEventRepository == null || correlationId == null) {
      return;
    }
    auditEventRepository.append(
        AuditEvent.of(
            "system",
            AuditEventType.TOOL_CALLED,
            investigation.id(),
            null,
            correlationId,
            "tool=" + toolName,
            investigation.promptVersion()));
  }

  private void auditToolOutcome(AuditEventType type, String toolName, String result) {
    if (auditEventRepository == null || correlationId == null) {
      return;
    }
    auditEventRepository.append(
        AuditEvent.of(
            "system",
            type,
            investigation.id(),
            null,
            correlationId,
            "tool=" + toolName + " " + result,
            investigation.promptVersion()));
  }

  public List<KnowledgeReference> collectKnowledge(List<KnowledgeSearchResult> results) {
    if (auditEventRepository != null) {
      results.forEach(this::auditIfInstructionPattern);
    }
    List<KnowledgeReference> refs =
        results.stream().map(r -> new KnowledgeReference(r.documentId(), r.chunkId())).toList();
    knownKnowledgeReferences.addAll(refs);
    knownKnowledgeResults.addAll(results);
    if (auditEventRepository != null && correlationId != null) {
      auditEventRepository.append(
          AuditEvent.of(
              "system",
              AuditEventType.RAG_COMPLETED,
              investigation.id(),
              null,
              correlationId,
              "results=" + refs.size(),
              investigation.promptVersion()));
    }
    return refs;
  }

  private void auditIfInstructionPattern(KnowledgeSearchResult result) {
    if (!contentSanitizer.hasInstructionPattern(result.content())) {
      return;
    }
    auditEventRepository.append(
        AuditEvent.of(
            "system",
            AuditEventType.PROMPT_INJECTION_SIGNAL,
            investigation.id(),
            null,
            result.documentId() + "#" + result.chunkId(),
            "retrieved knowledge chunk matched instruction-pattern heuristic",
            investigation.promptVersion()));
  }

  public List<KnowledgeReference> knownKnowledgeReferences() {
    return List.copyOf(knownKnowledgeReferences);
  }

  /** Resolves model-requested ids back to canonical retrieval metadata; unknown ids are dropped. */
  public List<KnowledgeCitation> canonicalKnowledgeCitations(List<KnowledgeReference> requested) {
    return requested.stream()
        .distinct()
        .map(
            reference ->
                knownKnowledgeResults.stream()
                    .filter(
                        result ->
                            result.documentId().equals(reference.documentId())
                                && result.chunkId().equals(reference.chunkId()))
                    .findFirst()
                    .map(
                        result ->
                            new KnowledgeCitation(
                                result.documentId(),
                                result.version(),
                                result.title(),
                                result.chunkId(),
                                result.similarityScore()))
                    .orElse(null))
        .filter(Objects::nonNull)
        .toList();
  }

  public List<String> knownEvidenceIds() {
    return investigation.evidence().stream().map(Evidence::id).toList();
  }

  private <T> List<String> mintEvidence(ToolResult<T> result) {
    Object data = result.data();
    Instant observedAt = result.observedAt();
    return switch (data) {
      case OtpMetricsResult m -> mintOtpMetrics(m, observedAt);
      case ErrorDistributionResult e -> mintErrorDistribution(e, observedAt);
      case QueueHealthResult q -> mintQueueHealth(q, observedAt);
      case ProviderHealthResult p -> mintProviderHealth(p, observedAt);
      case RecentChangesResult r -> mintRecentChanges(r);
      default -> List.of();
    };
  }

  private List<String> mintOtpMetrics(OtpMetricsResult m, Instant observedAt) {
    List<String> ids = new ArrayList<>();
    ids.add("ev-otp-success-rate-current");
    investigation.addEvidence(
        new Evidence(
            "ev-otp-success-rate-current",
            "TOOL_RESULT",
            "getOtpMetrics",
            "OTP success rate for current window is " + m.successRate() + "%",
            observedAt,
            "otp_success_rate",
            m.successRate(),
            "percent"));
    if (m.previousPeriod() != null) {
      ids.add("ev-otp-success-rate-previous");
      investigation.addEvidence(
          new Evidence(
              "ev-otp-success-rate-previous",
              "TOOL_RESULT",
              "getOtpMetrics",
              "OTP success rate for previous window was " + m.previousPeriod().successRate() + "%",
              observedAt,
              "otp_success_rate",
              m.previousPeriod().successRate(),
              "percent"));
    }
    return ids;
  }

  private List<String> mintErrorDistribution(ErrorDistributionResult e, Instant observedAt) {
    if (e.byErrorCode().isEmpty()) {
      return List.of();
    }
    ErrorCount top =
        e.byErrorCode().stream().max(Comparator.comparingLong(ErrorCount::count)).orElseThrow();
    List<String> ids = new ArrayList<>();
    ids.add("ev-error-distribution-top");
    investigation.addEvidence(
        new Evidence(
            "ev-error-distribution-top",
            "TOOL_RESULT",
            "getErrorDistribution",
            "Top failure cause is " + top.errorCode() + " (" + top.count() + " occurrences)",
            observedAt,
            null,
            null,
            null));
    // Per-code shares are the numbers an analyst actually quotes ("63.99% of failures were
    // PROVIDER_TIMEOUT"). Minting them as evidence makes those claims verifiable and chartable
    // instead of unsupported prose the validator has to reject.
    for (ErrorCount error : e.byErrorCode()) {
      String slug = error.errorCode().toLowerCase(Locale.ROOT).replace('_', '-');
      String shareId = "ev-error-share-" + slug;
      ids.add(shareId);
      investigation.addEvidence(
          new Evidence(
              shareId,
              "TOOL_RESULT",
              "getErrorDistribution",
              error.errorCode()
                  + " accounts for "
                  + error.count()
                  + " of "
                  + e.failedTotal()
                  + " failures",
              observedAt,
              "otp_error_share_" + slug.replace('-', '_'),
              // ErrorCount.share is already a percentage (docs/15 publishes 63.99, not 0.6399).
              round2(error.share()),
              "percent"));
    }
    return List.copyOf(ids);
  }

  /** {@code getProviderHealth} becomes {@code ev-provider-health-error}. */
  private static String errorEvidenceId(String toolName) {
    String withoutPrefix = toolName.startsWith("get") ? toolName.substring(3) : toolName;
    String kebab =
        withoutPrefix.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT);
    return "ev-" + kebab + "-error";
  }

  private static double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  private List<String> mintQueueHealth(QueueHealthResult q, Instant observedAt) {
    investigation.addEvidence(
        new Evidence(
            "ev-queue-health",
            "TOOL_RESULT",
            "getQueueHealth",
            "Queue status is " + q.status() + ", processing rate " + q.processingRateStatus(),
            observedAt,
            null,
            null,
            null));
    return List.of("ev-queue-health");
  }

  private List<String> mintProviderHealth(ProviderHealthResult p, Instant observedAt) {
    investigation.addEvidence(
        new Evidence(
            "ev-timeout-rate",
            "TOOL_RESULT",
            "getProviderHealth",
            p.provider() + " timeout rate is " + p.timeoutRate(),
            observedAt,
            "provider_timeout_rate",
            p.timeoutRate(),
            "ratio"));
    investigation.addEvidence(
        new Evidence(
            "ev-connection-capacity",
            "TOOL_RESULT",
            "getProviderHealth",
            p.provider()
                + " is using "
                + p.activeConnections()
                + "/"
                + p.maxConnections()
                + " connections",
            observedAt,
            "provider_connection_capacity_ratio",
            p.activeConnections() / (double) p.maxConnections(),
            "ratio"));
    return List.of("ev-timeout-rate", "ev-connection-capacity");
  }

  private List<String> mintRecentChanges(RecentChangesResult r) {
    List<String> ids = new ArrayList<>();
    for (ChangeEvent event : r.changes()) {
      if (!event.type().equals("CONFIG") && !event.type().equals("DEPLOY")) {
        continue;
      }
      String id = "ev-change-" + event.changeId();
      ids.add(id);
      investigation.addEvidence(
          new Evidence(
              id,
              "TOOL_RESULT",
              "getRecentChanges",
              event.description(),
              event.occurredAt(),
              null,
              null,
              null));
    }
    return ids;
  }
}
