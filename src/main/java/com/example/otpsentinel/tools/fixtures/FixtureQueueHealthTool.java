package com.example.otpsentinel.tools.fixtures;

import com.example.otpsentinel.tools.QueueHealthResult;
import com.example.otpsentinel.tools.QueueHealthTool;
import com.example.otpsentinel.tools.ToolResult;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/** Fixture adapter for T-003. Deterministic per {@link FixtureScenario} (AI-006). */
public final class FixtureQueueHealthTool implements QueueHealthTool {

  private final FixtureScenario scenario;
  private final Clock clock;

  public FixtureQueueHealthTool(FixtureScenario scenario) {
    this(scenario, Clock.systemUTC());
  }

  public FixtureQueueHealthTool(FixtureScenario scenario, Clock clock) {
    this.scenario = Objects.requireNonNull(scenario, "scenario must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public ToolResult<QueueHealthResult> getQueueHealth() {
    return ToolResult.success(UUID.randomUUID().toString(), "getQueueHealth", clock.instant(), scenario.queueHealth());
  }
}
