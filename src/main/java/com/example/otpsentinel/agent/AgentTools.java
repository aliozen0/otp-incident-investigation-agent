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
  private final boolean ragEnabled;

  public AgentTools(
      OtpMetricsTool otpMetricsTool,
      ErrorDistributionTool errorDistributionTool,
      QueueHealthTool queueHealthTool,
      ProviderHealthTool providerHealthTool,
      RecentChangesTool recentChangesTool,
      KnowledgeSearchPort knowledgeSearchPort,
      ToolBudgetGuard guard,
      EvidenceCollector collector) {
    this(
        otpMetricsTool,
        errorDistributionTool,
        queueHealthTool,
        providerHealthTool,
        recentChangesTool,
        knowledgeSearchPort,
        guard,
        collector,
        true);
  }

  /**
   * {@code ragEnabled=false} is M11's quick mode: {@link #searchIncidentKnowledge} answers "no
   * results" immediately without touching {@link ToolBudgetGuard}, so the five live-signal tools
   * still run to completion and produce a complete (not {@code PARTIAL_ANALYSIS}) result — the mode
   * trades prior-incident knowledge for latency, not completeness.
   */
  public AgentTools(
      OtpMetricsTool otpMetricsTool,
      ErrorDistributionTool errorDistributionTool,
      QueueHealthTool queueHealthTool,
      ProviderHealthTool providerHealthTool,
      RecentChangesTool recentChangesTool,
      KnowledgeSearchPort knowledgeSearchPort,
      ToolBudgetGuard guard,
      EvidenceCollector collector,
      boolean ragEnabled) {
    this.ragEnabled = ragEnabled;
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
    return collectOrReuse(
        "getOtpMetrics", () -> guard.execute("getOtpMetrics", request, () -> otpMetricsTool.getOtpMetrics(request)));
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
    return collectOrReuse(
        "getErrorDistribution",
        () ->
            guard.execute(
                "getErrorDistribution",
                request,
                () -> errorDistributionTool.getErrorDistribution(request)));
  }

  @Tool("Get current OTP outbound queue health (pending messages, consumer count, dead letters)")
  public AgentToolResponse<QueueHealthResult> getQueueHealth() {
    return collectOrReuse(
        "getQueueHealth", () -> guard.execute("getQueueHealth", "none", queueHealthTool::getQueueHealth));
  }

  @Tool(
      "Get a single provider's health (response time, timeout rate, circuit breaker state, connection pool usage) for a time window")
  public AgentToolResponse<ProviderHealthResult> getProviderHealth(
      String provider, String startAt, String endAt) {
    ProviderHealthRequest request =
        new ProviderHealthRequest(
            // The port requires a provider; a placeholder becomes the explicit "any provider" ask,
            // which the adapter answers with the worst-performing one.
            optionalFilter(provider) == null ? "ALL" : provider.trim(),
            parseInstant(startAt, "startAt"),
            parseInstant(endAt, "endAt"));
    return collectOrReuse(
        "getProviderHealth",
        () ->
            guard.execute(
                "getProviderHealth", request, () -> providerHealthTool.getProviderHealth(request)));
  }

  @Tool("Get recent config/deploy/observation changes for a component within a time window")
  public AgentToolResponse<RecentChangesResult> getRecentChanges(
      String from, String to, String component) {
    RecentChangesRequest request =
        new RecentChangesRequest(
            parseInstant(from, "from"), parseInstant(to, "to"), optionalFilter(component));
    return collectOrReuse(
        "getRecentChanges",
        () ->
            guard.execute(
                "getRecentChanges", request, () -> recentChangesTool.getRecentChanges(request)));
  }

  @Tool(
      "Search incident knowledge base (runbooks, prior incidents) for relevant guidance, optionally filtered to a provider")
  public List<KnowledgeReference> searchIncidentKnowledge(
      String query, String providerFilter, int topK) {
    if (!ragEnabled) {
      // Quick mode: short-circuit before the guard so this costs neither a budget slot nor a
      // rejection — the model just sees "no knowledge results" and writes its final answer.
      return List.of();
    }
    ToolResult<java.util.List<com.example.otpsentinel.rag.KnowledgeSearchResult>> searchResults;
    try {
      searchResults =
        guard.executeUnbudgeted(
            "searchIncidentKnowledge",
            query + "|" + providerFilter + "|" + topK,
            () ->
                ToolResult.success(
                    java.util.UUID.randomUUID().toString(),
                    "searchIncidentKnowledge",
                    Instant.now(),
                    knowledgeSearchPort.searchIncidentKnowledge(query, providerFilter, topK)));
    } catch (DuplicateToolCallException duplicate) {
      return List.of();
    }
    return collector.collectKnowledge(searchResults.data());
  }

  /**
   * A repeated call is answered with an explanation instead of an exception. Live models re-call a
   * tool with slightly different arguments (a rounded timestamp, another provider) surprisingly
   * often, and letting that abort the run threw away investigations whose evidence was already
   * complete. The refusal still stands — no second execution, no extra evidence.
   */
  private <T> AgentToolResponse<T> collectOrReuse(
      String toolName, java.util.function.Supplier<ToolResult<T>> call) {
    try {
      return collector.collect(call.get());
    } catch (DuplicateToolCallException duplicate) {
      return new AgentToolResponse<>(
          ToolStatus.ERROR,
          null,
          List.of(),
          toolName
              + " was already called with these arguments in this turn; re-read that earlier result"
              + " and continue with the next tool.");
    }
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
   * {@code Instant} (docs/superpowers/plans M5 deviation #2). This argument is optional (see the
   * {@code @Tool} description on {@link #getOtpMetrics}), so a {@code null}/blank value — the model
   * omitted it — means "not requested", matching {@link #optionalFilter}'s treatment of omission.
   */
  private static boolean parseBoolean(String value, String parameterName) {
    if (value == null || value.isBlank()) {
      return false;
    }
    if ("true".equalsIgnoreCase(value)) {
      return true;
    }
    if ("false".equalsIgnoreCase(value)) {
      return false;
    }
    throw new IllegalArgumentException(parameterName + " must be true or false");
  }

  /**
   * Optional filters are the single biggest source of empty tool results with live models: instead
   * of omitting the argument they send a placeholder ("null", "default", "all", "string"). Those
   * mean "no filter", and treating them literally silently starved the analysis of error and
   * provider data, which then read as "no anomaly".
   */
  private static String optionalFilter(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim();
    return PLACEHOLDER_FILTERS.contains(normalized.toLowerCase(java.util.Locale.ROOT))
        ? null
        : normalized;
  }

  private static final java.util.Set<String> PLACEHOLDER_FILTERS =
      java.util.Set.of(
          "null", "none", "nil", "default", "all", "any", "*", "undefined", "n/a", "na", "string",
          "unknown", "-");
}
