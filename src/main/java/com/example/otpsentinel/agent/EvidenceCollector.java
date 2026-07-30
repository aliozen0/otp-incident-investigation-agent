package com.example.otpsentinel.agent;

import com.example.otpsentinel.domain.Evidence;
import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.rag.KnowledgeSearchResult;
import com.example.otpsentinel.tools.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Turns raw {@link ToolResult}s into application-minted {@link Evidence} (ADR-008: the model never
 * mints an id, it only cites one it was shown). One instance per {@link Investigation}.
 */
public final class EvidenceCollector {

  private final Investigation investigation;
  private final List<KnowledgeReference> knownKnowledgeReferences = new ArrayList<>();

  public EvidenceCollector(Investigation investigation) {
    this.investigation = Objects.requireNonNull(investigation, "investigation must not be null");
  }

  public <T> AgentToolResponse<T> collect(ToolResult<T> result) {
    investigation.recordToolExecution(result.executionId());
    if (result.status() != ToolStatus.SUCCESS) {
      return new AgentToolResponse<>(result.status(), null, List.of(), result.error().message());
    }
    List<String> ids = mintEvidence(result);
    return new AgentToolResponse<>(result.status(), result.data(), ids, null);
  }

  public List<KnowledgeReference> collectKnowledge(List<KnowledgeSearchResult> results) {
    List<KnowledgeReference> refs =
        results.stream().map(r -> new KnowledgeReference(r.documentId(), r.chunkId())).toList();
    knownKnowledgeReferences.addAll(refs);
    return refs;
  }

  public List<KnowledgeReference> knownKnowledgeReferences() {
    return List.copyOf(knownKnowledgeReferences);
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
    return List.of("ev-error-distribution-top");
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
