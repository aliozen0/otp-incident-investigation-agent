package com.example.otpsentinel.tools.fixtures;

import com.example.otpsentinel.tools.ProviderHealthRequest;
import com.example.otpsentinel.tools.ProviderHealthResult;
import com.example.otpsentinel.tools.ProviderHealthTool;
import com.example.otpsentinel.tools.ToolError;
import com.example.otpsentinel.tools.ToolResult;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Fixture adapter for T-004. Deterministic per {@link FixtureScenario} (AI-006). Simulates NFR-008
 * timeout behavior when the requested provider matches {@link FixtureScenario#timedOutProvider()}
 * (OTP-PARTIAL-001).
 */
public final class FixtureProviderHealthTool implements ProviderHealthTool {

  private final FixtureScenario scenario;
  private final Clock clock;

  public FixtureProviderHealthTool(FixtureScenario scenario) {
    this(scenario, Clock.systemUTC());
  }

  public FixtureProviderHealthTool(FixtureScenario scenario, Clock clock) {
    this.scenario = Objects.requireNonNull(scenario, "scenario must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public ToolResult<ProviderHealthResult> getProviderHealth(ProviderHealthRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    String executionId = UUID.randomUUID().toString();
    Instant observedAt = clock.instant();
    if (request.provider().equals(scenario.timedOutProvider())) {
      return ToolResult.timeout(
          executionId,
          "getProviderHealth",
          observedAt,
          new ToolError(
              "TIMEOUT",
              "Provider health check for " + request.provider() + " did not complete in time"));
    }
    ProviderHealthResult data = scenario.providerHealthByProvider().get(request.provider());
    if (data == null) {
      return ToolResult.error(
          executionId,
          "getProviderHealth",
          observedAt,
          new ToolError(
              "PROVIDER_NOT_FOUND", "No fixture data for provider " + request.provider()));
    }
    return ToolResult.success(executionId, "getProviderHealth", observedAt, data);
  }
}
