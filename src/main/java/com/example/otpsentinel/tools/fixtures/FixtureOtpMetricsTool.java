package com.example.otpsentinel.tools.fixtures;

import com.example.otpsentinel.tools.OtpMetricsRequest;
import com.example.otpsentinel.tools.OtpMetricsResult;
import com.example.otpsentinel.tools.OtpMetricsTool;
import com.example.otpsentinel.tools.ToolResult;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/** Fixture adapter for T-001. Deterministic per {@link FixtureScenario} (AI-006). */
public final class FixtureOtpMetricsTool implements OtpMetricsTool {

  private final FixtureScenario scenario;
  private final Clock clock;

  public FixtureOtpMetricsTool(FixtureScenario scenario) {
    this(scenario, Clock.systemUTC());
  }

  public FixtureOtpMetricsTool(FixtureScenario scenario, Clock clock) {
    this.scenario = Objects.requireNonNull(scenario, "scenario must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public ToolResult<OtpMetricsResult> getOtpMetrics(OtpMetricsRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    return ToolResult.success(
        UUID.randomUUID().toString(), "getOtpMetrics", clock.instant(), scenario.otpMetrics());
  }
}
