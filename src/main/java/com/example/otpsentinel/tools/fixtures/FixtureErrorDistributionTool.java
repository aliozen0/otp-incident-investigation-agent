package com.example.otpsentinel.tools.fixtures;

import com.example.otpsentinel.tools.ErrorDistributionRequest;
import com.example.otpsentinel.tools.ErrorDistributionResult;
import com.example.otpsentinel.tools.ErrorDistributionTool;
import com.example.otpsentinel.tools.ProviderErrorBreakdown;
import com.example.otpsentinel.tools.ToolResult;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Fixture adapter for T-002. Deterministic per {@link FixtureScenario} (AI-006). */
public final class FixtureErrorDistributionTool implements ErrorDistributionTool {

  private final FixtureScenario scenario;
  private final Clock clock;

  public FixtureErrorDistributionTool(FixtureScenario scenario) {
    this(scenario, Clock.systemUTC());
  }

  public FixtureErrorDistributionTool(FixtureScenario scenario, Clock clock) {
    this.scenario = Objects.requireNonNull(scenario, "scenario must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public ToolResult<ErrorDistributionResult> getErrorDistribution(ErrorDistributionRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    ErrorDistributionResult full = scenario.errorDistribution();
    ErrorDistributionResult data = request.provider() == null ? full : filterByProvider(full, request.provider());
    return ToolResult.success(UUID.randomUUID().toString(), "getErrorDistribution", clock.instant(), data);
  }

  private static ErrorDistributionResult filterByProvider(ErrorDistributionResult full, String provider) {
    List<ProviderErrorBreakdown> filtered =
        full.byProvider().stream().filter(p -> p.provider().equals(provider)).toList();
    long failedTotal = filtered.stream().mapToLong(ProviderErrorBreakdown::failed).sum();
    return new ErrorDistributionResult(failedTotal, full.byErrorCode(), filtered);
  }
}
