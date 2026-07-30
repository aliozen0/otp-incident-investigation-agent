package com.example.otpsentinel.tools.fixtures;

import com.example.otpsentinel.tools.RecentChangesRequest;
import com.example.otpsentinel.tools.RecentChangesResult;
import com.example.otpsentinel.tools.RecentChangesTool;
import com.example.otpsentinel.tools.ToolResult;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/** Fixture adapter for T-005. Deterministic per {@link FixtureScenario} (AI-006). */
public final class FixtureRecentChangesTool implements RecentChangesTool {

  private final FixtureScenario scenario;
  private final Clock clock;

  public FixtureRecentChangesTool(FixtureScenario scenario) {
    this(scenario, Clock.systemUTC());
  }

  public FixtureRecentChangesTool(FixtureScenario scenario, Clock clock) {
    this.scenario = Objects.requireNonNull(scenario, "scenario must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public ToolResult<RecentChangesResult> getRecentChanges(RecentChangesRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    RecentChangesResult full = scenario.recentChanges();
    RecentChangesResult data =
        request.component() == null ? full : filterByComponent(full, request.component());
    return ToolResult.success(
        UUID.randomUUID().toString(), "getRecentChanges", clock.instant(), data);
  }

  private static RecentChangesResult filterByComponent(RecentChangesResult full, String component) {
    return new RecentChangesResult(
        full.changes().stream().filter(c -> c.component().equals(component)).toList());
  }
}
