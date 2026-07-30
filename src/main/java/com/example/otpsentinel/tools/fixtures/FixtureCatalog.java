package com.example.otpsentinel.tools.fixtures;

import com.example.otpsentinel.domain.TimeWindow;
import com.example.otpsentinel.tools.ChangeEvent;
import com.example.otpsentinel.tools.ErrorCount;
import com.example.otpsentinel.tools.ErrorDistributionResult;
import com.example.otpsentinel.tools.OtpMetricsResult;
import com.example.otpsentinel.tools.PeriodComparison;
import com.example.otpsentinel.tools.ProviderErrorBreakdown;
import com.example.otpsentinel.tools.ProviderHealthResult;
import com.example.otpsentinel.tools.QueueHealthResult;
import com.example.otpsentinel.tools.RecentChangesResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Deterministic, in-code fixture data (AI-006). Numbers for {@link FixtureId#OTP_DROP_001} are
 * copied verbatim from docs/15-demo-fixtures.md; see that file for the source table.
 *
 * <p>docs/15 only fully specifies OTP-DROP-001. The other four scenarios are not given complete
 * per-tool figures, so this catalog resolves them as follows (documented, not invented silently
 * per prompts/handoff/M2-prompt.md constraints):
 *
 * <ul>
 *   <li>OTP-NORMAL-001: docs/15 gives only "success 98.4%, NO_ANOMALY". A minimal, internally
 *       consistent healthy dataset is synthesized (round numbers chosen so the ratio is exact,
 *       never rounded) purely so the five tools have something deterministic to return; only the
 *       98.4% success rate is a normative assertion from docs/15.
 *   <li>OTP-PARTIAL-001: docs/15 says only "provider tool timeout". Reuses the OTP-DROP-001
 *       dataset for every tool except getProviderHealth(OPERATOR_B), which times out.
 *   <li>OTP-RAG-NONE-001 / OTP-INJECTION-001: docs/15 describes both as differing only in
 *       knowledge-base content ("live evidence var, knowledge yok" / "knowledge içinde kötü
 *       niyetli talimat") — the RAG/knowledge layer is out of scope for M2. Both reuse the
 *       OTP-DROP-001 tool dataset unchanged.
 * </ul>
 */
public final class FixtureCatalog {

  private FixtureCatalog() {}

  public static FixtureScenario forFixture(FixtureId id) {
    return switch (id) {
      case OTP_DROP_001 -> dropScenario();
      case OTP_NORMAL_001 -> normalScenario();
      case OTP_PARTIAL_001 -> partialScenario();
      case OTP_RAG_NONE_001 -> dropScenario().withId(FixtureId.OTP_RAG_NONE_001);
      case OTP_INJECTION_001 -> dropScenario().withId(FixtureId.OTP_INJECTION_001);
    };
  }

  private static FixtureScenario dropScenario() {
    TimeWindow currentWindow =
        new TimeWindow(Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z"));
    TimeWindow previousWindow =
        new TimeWindow(Instant.parse("2026-07-30T11:00:00Z"), Instant.parse("2026-07-30T11:15:00Z"));

    OtpMetricsResult otpMetrics =
        new OtpMetricsResult(
            currentWindow,
            12_480L,
            8_998L,
            3_482L,
            72.10,
            8.7,
            new PeriodComparison(previousWindow, 11_940L, 98.10, 2.2));

    List<ProviderErrorBreakdown> byProvider =
        List.of(
            new ProviderErrorBreakdown("OPERATOR_A", 3_500L, 3_427L, 73L, 97.91),
            new ProviderErrorBreakdown("OPERATOR_B", 6_936L, 3_588L, 3_348L, 51.73),
            new ProviderErrorBreakdown("OPERATOR_C", 2_044L, 1_983L, 61L, 97.02));

    List<ErrorCount> byErrorCode =
        List.of(
            new ErrorCount("PROVIDER_TIMEOUT", 2_228L, 63.99),
            new ErrorCount("RATE_LIMITED", 627L, 18.01),
            new ErrorCount("CONNECTION_RESET", 383L, 11.00),
            new ErrorCount("INVALID_NUMBER", 139L, 3.99),
            new ErrorCount("UNKNOWN", 105L, 3.02));

    ErrorDistributionResult errorDistribution = new ErrorDistributionResult(3_482L, byErrorCode, byProvider);

    QueueHealthResult queueHealth = new QueueHealthResult(184L, 1_000L, 4L, 30L, 8, 8, 3L, "NORMAL", "HEALTHY");

    ProviderHealthResult operatorB =
        new ProviderHealthResult(
            "OPERATOR_B",
            "DEGRADED",
            13.9,
            0.31,
            Instant.parse("2026-07-30T11:29:42Z"),
            "HALF_OPEN",
            48,
            50);

    RecentChangesResult recentChanges =
        new RecentChangesResult(
            List.of(
                new ChangeEvent(
                    "chg-101",
                    Instant.parse("2026-07-30T11:05:00Z"),
                    "CONFIG",
                    "OTP_GATEWAY",
                    "Retry count changed from 3 to 2",
                    null,
                    true),
                new ChangeEvent(
                    "chg-102",
                    Instant.parse("2026-07-30T11:12:00Z"),
                    "DEPLOY",
                    "OTP_GATEWAY",
                    "Gateway v2.4 deployed",
                    "v2.4",
                    true),
                new ChangeEvent(
                    "obs-103",
                    Instant.parse("2026-07-30T11:16:00Z"),
                    "OBSERVATION",
                    "OPERATOR_B_ADAPTER",
                    "Provider response time started increasing",
                    null,
                    null),
                new ChangeEvent(
                    "obs-104",
                    Instant.parse("2026-07-30T11:18:00Z"),
                    "OBSERVATION",
                    "OTP_GATEWAY",
                    "OTP success rate dropped materially",
                    null,
                    null)));

    return new FixtureScenario(
        FixtureId.OTP_DROP_001,
        otpMetrics,
        errorDistribution,
        queueHealth,
        Map.of("OPERATOR_B", operatorB),
        null,
        recentChanges);
  }

  private static FixtureScenario normalScenario() {
    TimeWindow currentWindow =
        new TimeWindow(Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z"));
    TimeWindow previousWindow =
        new TimeWindow(Instant.parse("2026-07-30T11:00:00Z"), Instant.parse("2026-07-30T11:15:00Z"));

    // 9,840 / 10,000 = 98.40% exactly, matching docs/15's "OTP-NORMAL-001: %98.4, NO_ANOMALY".
    OtpMetricsResult otpMetrics =
        new OtpMetricsResult(
            currentWindow,
            10_000L,
            9_840L,
            160L,
            98.40,
            2.0,
            new PeriodComparison(previousWindow, 9_950L, 98.30, 2.05));

    List<ProviderErrorBreakdown> byProvider =
        List.of(
            new ProviderErrorBreakdown("OPERATOR_A", 3_400L, 3_360L, 40L, 98.82),
            new ProviderErrorBreakdown("OPERATOR_B", 3_400L, 3_340L, 60L, 98.24),
            new ProviderErrorBreakdown("OPERATOR_C", 3_200L, 3_140L, 60L, 98.13));

    List<ErrorCount> byErrorCode =
        List.of(new ErrorCount("INVALID_NUMBER", 100L, 62.5), new ErrorCount("UNKNOWN", 60L, 37.5));

    ErrorDistributionResult errorDistribution = new ErrorDistributionResult(160L, byErrorCode, byProvider);

    QueueHealthResult queueHealth = new QueueHealthResult(184L, 1_000L, 4L, 30L, 8, 8, 3L, "NORMAL", "HEALTHY");

    ProviderHealthResult operatorB =
        new ProviderHealthResult(
            "OPERATOR_B",
            "HEALTHY",
            1.8,
            0.01,
            Instant.parse("2026-07-30T11:29:58Z"),
            "CLOSED",
            10,
            50);

    return new FixtureScenario(
        FixtureId.OTP_NORMAL_001,
        otpMetrics,
        errorDistribution,
        queueHealth,
        Map.of("OPERATOR_B", operatorB),
        null,
        new RecentChangesResult(List.of()));
  }

  private static FixtureScenario partialScenario() {
    FixtureScenario base = dropScenario();
    return new FixtureScenario(
        FixtureId.OTP_PARTIAL_001,
        base.otpMetrics(),
        base.errorDistribution(),
        base.queueHealth(),
        base.providerHealthByProvider(),
        "OPERATOR_B",
        base.recentChanges());
  }
}
