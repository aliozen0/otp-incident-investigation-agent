package com.example.otpsentinel.operations;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import com.example.otpsentinel.tools.ErrorDistributionRequest;
import com.example.otpsentinel.tools.ErrorDistributionResult;
import com.example.otpsentinel.tools.OtpMetricsRequest;
import com.example.otpsentinel.tools.OtpMetricsResult;
import com.example.otpsentinel.tools.ProviderHealthRequest;
import com.example.otpsentinel.tools.ProviderHealthResult;
import com.example.otpsentinel.tools.QueueHealthResult;
import com.example.otpsentinel.tools.RecentChangesRequest;
import com.example.otpsentinel.tools.RecentChangesResult;
import com.example.otpsentinel.tools.ToolResult;
import com.example.otpsentinel.tools.ToolStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The tools must answer from the seeded rows, and an operator must be able to recompute every
 * figure from those same rows — that equivalence is what makes the console's data explorer a real
 * verification surface rather than decoration.
 */
class JdbcOperationalDataToolsTest extends AbstractPostgresIntegrationTest {

  private static final Instant NOW = Instant.parse("2026-08-04T12:00:00Z");

  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private JdbcOperationalDataTools tools;

  @BeforeEach
  void seed() {
    jdbcTemplate.update("DELETE FROM otp_delivery_sample");
    jdbcTemplate.update("DELETE FROM otp_error_sample");
    jdbcTemplate.update("DELETE FROM provider_health_sample");
    jdbcTemplate.update("DELETE FROM queue_health_sample");
    jdbcTemplate.update("DELETE FROM change_event");
    // 15-minute degradation so the 15 minutes before it are still healthy and the previous-period
    // comparison has something to contrast with.
    new OperationalDataSeeder(jdbcTemplate, clock, Duration.ofHours(2), Duration.ofMinutes(15))
        .seedHistory();
    tools = new JdbcOperationalDataTools(jdbcTemplate, clock);
  }

  @Test
  void metricsAggregateTheDegradedWindowAndCompareItWithTheHealthyOne() {
    Instant end = NOW.truncatedTo(ChronoUnit.MINUTES);
    Instant start = end.minus(Duration.ofMinutes(15));

    ToolResult<OtpMetricsResult> result =
        tools.getOtpMetrics(new OtpMetricsRequest(start, end, true));

    assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
    OtpMetricsResult data = result.data();
    // Volume varies per minute and per provider, so the assertions describe the shape of the
    // incident rather than one hard-coded total: a degraded window in the low 70s against a healthy
    // previous window in the high 90s.
    assertThat(data.total()).isGreaterThan(5_000L);
    assertThat(data.delivered() + data.failed()).isEqualTo(data.total());
    assertThat(data.successRate()).isBetween(65.0, 80.0);
    assertThat(data.averageDeliverySeconds()).isGreaterThan(5.0);
    assertThat(data.previousPeriod().successRate()).isGreaterThan(95.0);
  }

  @Test
  void errorDistributionSharesAddUpToTheFailureTotal() {
    Instant end = NOW.truncatedTo(ChronoUnit.MINUTES);
    Instant start = end.minus(Duration.ofMinutes(15));

    ToolResult<ErrorDistributionResult> result =
        tools.getErrorDistribution(new ErrorDistributionRequest(start, end, null));

    assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
    ErrorDistributionResult data = result.data();
    assertThat(data.byErrorCode().stream().mapToLong(e -> e.count()).sum())
        .isEqualTo(data.failedTotal());
    assertThat(data.byErrorCode().getFirst().errorCode()).isEqualTo("PROVIDER_TIMEOUT");
    assertThat(data.byProvider()).extracting(p -> p.provider())
        .containsExactly("OPERATOR_A", "OPERATOR_B", "OPERATOR_C", "OPERATOR_D");
  }

  @Test
  void providerHealthFallsBackToTheWorstProviderWhenTheModelAsksForAll() {
    Instant end = NOW.truncatedTo(ChronoUnit.MINUTES);
    Instant start = end.minus(Duration.ofMinutes(15));

    ToolResult<ProviderHealthResult> result =
        tools.getProviderHealth(new ProviderHealthRequest("ALL", start, end));

    assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
    assertThat(result.data().provider()).isEqualTo("OPERATOR_B");
    assertThat(result.data().status()).isEqualTo("DEGRADED");
  }

  @Test
  void queueStaysHealthyAndChangesAreVisibleAroundTheIncident() {
    ToolResult<QueueHealthResult> queue = tools.getQueueHealth();
    assertThat(queue.status()).isEqualTo(ToolStatus.SUCCESS);
    assertThat(queue.data().status()).isEqualTo("HEALTHY");

    ToolResult<RecentChangesResult> changes =
        tools.getRecentChanges(
            new RecentChangesRequest(NOW.minus(Duration.ofHours(2)), NOW.plusSeconds(60), null));
    assertThat(changes.data().changes()).extracting(c -> c.changeId())
        .contains("chg-101", "chg-102", "obs-103");
  }

  @Test
  void reportsNoDataInsteadOfInventingNumbersForAnEmptyWindow() {
    Instant start = NOW.minus(Duration.ofDays(30));
    ToolResult<OtpMetricsResult> result =
        tools.getOtpMetrics(new OtpMetricsRequest(start, start.plusSeconds(900), false));

    assertThat(result.status()).isEqualTo(ToolStatus.ERROR);
    assertThat(result.error().code()).isEqualTo("NO_DATA");
  }
}
