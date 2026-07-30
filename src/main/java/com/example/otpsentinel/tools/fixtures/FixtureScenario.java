package com.example.otpsentinel.tools.fixtures;

import com.example.otpsentinel.tools.ErrorDistributionResult;
import com.example.otpsentinel.tools.OtpMetricsResult;
import com.example.otpsentinel.tools.ProviderHealthResult;
import com.example.otpsentinel.tools.QueueHealthResult;
import com.example.otpsentinel.tools.RecentChangesResult;
import java.util.Map;
import java.util.Objects;

/**
 * One deterministic dataset shared by all five fixture tool adapters for a given {@link
 * FixtureId}. {@code timedOutProvider}, when set, makes {@code getProviderHealth} for that
 * provider return {@code ToolStatus.TIMEOUT} instead of data (NFR-008 timeout simulation).
 */
public record FixtureScenario(
    FixtureId id,
    OtpMetricsResult otpMetrics,
    ErrorDistributionResult errorDistribution,
    QueueHealthResult queueHealth,
    Map<String, ProviderHealthResult> providerHealthByProvider,
    String timedOutProvider,
    RecentChangesResult recentChanges) {

  public FixtureScenario {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(otpMetrics, "otpMetrics must not be null");
    Objects.requireNonNull(errorDistribution, "errorDistribution must not be null");
    Objects.requireNonNull(queueHealth, "queueHealth must not be null");
    Objects.requireNonNull(providerHealthByProvider, "providerHealthByProvider must not be null");
    Objects.requireNonNull(recentChanges, "recentChanges must not be null");
    providerHealthByProvider = Map.copyOf(providerHealthByProvider);
  }

  public FixtureScenario withId(FixtureId newId) {
    return new FixtureScenario(
        newId, otpMetrics, errorDistribution, queueHealth, providerHealthByProvider, timedOutProvider, recentChanges);
  }
}
