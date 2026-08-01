package com.example.otpsentinel.agent;

import com.example.otpsentinel.rag.KnowledgeSearchPort;
import com.example.otpsentinel.tools.*;
import dev.langchain4j.agent.tool.Tool;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Binds the M2 fixture tool ports and M4's {@link KnowledgeSearchPort} as LangChain4j {@code @Tool}
 * methods. Every call is routed through {@link ToolBudgetGuard} (budget/dedup/timeout, enforced
 * outside the framework) and {@link EvidenceCollector} (application-minted evidence ids, ADR-008).
 * {@code createIncidentDraft} (T-007) is intentionally absent — docs/07: "Normal agent tool setine
 * açık değildir."
 */
public final class AgentTools {

  private final OtpMetricsTool otpMetricsTool;
  private final ErrorDistributionTool errorDistributionTool;
  private final QueueHealthTool queueHealthTool;
  private final ProviderHealthTool providerHealthTool;
  private final RecentChangesTool recentChangesTool;
  private final KnowledgeSearchPort knowledgeSearchPort;
  private final ToolBudgetGuard guard;
  private final EvidenceCollector collector;

  public AgentTools(
      OtpMetricsTool otpMetricsTool,
      ErrorDistributionTool errorDistributionTool,
      QueueHealthTool queueHealthTool,
      ProviderHealthTool providerHealthTool,
      RecentChangesTool recentChangesTool,
      KnowledgeSearchPort knowledgeSearchPort,
      ToolBudgetGuard guard,
      EvidenceCollector collector) {
    this.otpMetricsTool = Objects.requireNonNull(otpMetricsTool);
    this.errorDistributionTool = Objects.requireNonNull(errorDistributionTool);
    this.queueHealthTool = Objects.requireNonNull(queueHealthTool);
    this.providerHealthTool = Objects.requireNonNull(providerHealthTool);
    this.recentChangesTool = Objects.requireNonNull(recentChangesTool);
    this.knowledgeSearchPort = Objects.requireNonNull(knowledgeSearchPort);
    this.guard = Objects.requireNonNull(guard);
    this.collector = Objects.requireNonNull(collector);
  }

  @Tool(
      "Get OTP delivery metrics (total/delivered/failed/success rate) for a time window, optionally including the previous period for comparison")
  public AgentToolResponse<OtpMetricsResult> getOtpMetrics(
      String startAt, String endAt, String includePreviousPeriod) {
    OtpMetricsRequest request =
        new OtpMetricsRequest(
            parseInstant(startAt, "startAt"),
            parseInstant(endAt, "endAt"),
            parseBoolean(includePreviousPeriod, "includePreviousPeriod"));
    ToolResult<OtpMetricsResult> result =
        guard.execute("getOtpMetrics", request, () -> otpMetricsTool.getOtpMetrics(request));
    return collector.collect(result);
  }

  @Tool(
      "Get OTP failure breakdown by error code and provider for a time window, optionally filtered to one provider")
  public AgentToolResponse<ErrorDistributionResult> getErrorDistribution(
      String startAt, String endAt, String provider) {
    ErrorDistributionRequest request =
        new ErrorDistributionRequest(
            parseInstant(startAt, "startAt"),
            parseInstant(endAt, "endAt"),
            optionalFilter(provider));
    ToolResult<ErrorDistributionResult> result =
        guard.execute(
            "getErrorDistribution",
            request,
            () -> errorDistributionTool.getErrorDistribution(request));
    return collector.collect(result);
  }

  @Tool("Get current OTP outbound queue health (pending messages, consumer count, dead letters)")
  public AgentToolResponse<QueueHealthResult> getQueueHealth() {
    ToolResult<QueueHealthResult> result =
        guard.execute("getQueueHealth", "none", queueHealthTool::getQueueHealth);
    return collector.collect(result);
  }

  @Tool(
      "Get a single provider's health (response time, timeout rate, circuit breaker state, connection pool usage) for a time window")
  public AgentToolResponse<ProviderHealthResult> getProviderHealth(
      String provider, String startAt, String endAt) {
    ProviderHealthRequest request =
        new ProviderHealthRequest(
            provider, parseInstant(startAt, "startAt"), parseInstant(endAt, "endAt"));
    ToolResult<ProviderHealthResult> result =
        guard.execute(
            "getProviderHealth", request, () -> providerHealthTool.getProviderHealth(request));
    return collector.collect(result);
  }

  @Tool("Get recent config/deploy/observation changes for a component within a time window")
  public AgentToolResponse<RecentChangesResult> getRecentChanges(
      String from, String to, String component) {
    RecentChangesRequest request =
        new RecentChangesRequest(
            parseInstant(from, "from"), parseInstant(to, "to"), optionalFilter(component));
    ToolResult<RecentChangesResult> result =
        guard.execute(
            "getRecentChanges", request, () -> recentChangesTool.getRecentChanges(request));
    return collector.collect(result);
  }

  @Tool(
      "Search incident knowledge base (runbooks, prior incidents) for relevant guidance, optionally filtered to a provider")
  public List<KnowledgeReference> searchIncidentKnowledge(
      String query, String providerFilter, int topK) {
    var searchResults =
        guard.execute(
            "searchIncidentKnowledge",
            query + "|" + providerFilter + "|" + topK,
            () ->
                ToolResult.success(
                    java.util.UUID.randomUUID().toString(),
                    "searchIncidentKnowledge",
                    Instant.now(),
                    knowledgeSearchPort.searchIncidentKnowledge(query, providerFilter, topK)));
    return collector.collectKnowledge(searchResults.data());
  }

  private static Instant parseInstant(String value, String parameterName) {
    try {
      return Instant.parse(value);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(parameterName + " must be an ISO-8601 UTC instant", e);
    }
  }

  /**
   * Some live models emit a JSON string ("true") instead of a JSON boolean for this argument;
   * LangChain4j's tool-argument coercion then throws before the tool body runs. Accepting the raw
   * string here and parsing deterministically avoids that — same fix as {@link #parseInstant} for
   * {@code Instant} (docs/superpowers/plans M5 deviation #2).
   */
  private static boolean parseBoolean(String value, String parameterName) {
    if ("true".equalsIgnoreCase(value)) {
      return true;
    }
    if ("false".equalsIgnoreCase(value)) {
      return false;
    }
    throw new IllegalArgumentException(parameterName + " must be true or false");
  }

  private static String optionalFilter(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
