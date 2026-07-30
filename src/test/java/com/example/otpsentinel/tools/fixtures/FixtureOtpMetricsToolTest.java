package com.example.otpsentinel.tools.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.tools.OtpMetricsRequest;
import com.example.otpsentinel.tools.OtpMetricsResult;
import com.example.otpsentinel.tools.ToolResult;
import com.example.otpsentinel.tools.ToolStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class FixtureOtpMetricsToolTest {

  private static final Instant NOW = Instant.parse("2026-07-30T11:31:00Z");

  @Test
  void returnsOtpDrop001NumbersVerbatimFromDemoFixtures() {
    FixtureOtpMetricsTool tool =
        new FixtureOtpMetricsTool(
            FixtureCatalog.forFixture(FixtureId.OTP_DROP_001), Clock.fixed(NOW, ZoneOffset.UTC));

    ToolResult<OtpMetricsResult> result =
        tool.getOtpMetrics(
            new OtpMetricsRequest(
                Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z"), true));

    assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
    assertThat(result.toolName()).isEqualTo("getOtpMetrics");
    assertThat(result.observedAt()).isEqualTo(NOW);

    OtpMetricsResult data = result.data();
    assertThat(data.total()).isEqualTo(12_480L);
    assertThat(data.delivered()).isEqualTo(8_998L);
    assertThat(data.failed()).isEqualTo(3_482L);
    assertThat(data.successRate()).isEqualTo(72.10);
    assertThat(data.averageDeliverySeconds()).isEqualTo(8.7);

    assertThat(data.previousPeriod().total()).isEqualTo(11_940L);
    assertThat(data.previousPeriod().successRate()).isEqualTo(98.10);
    assertThat(data.previousPeriod().averageDeliverySeconds()).isEqualTo(2.2);
  }

  @Test
  void otpNormal001ReflectsTheDocumentedNoAnomalySuccessRate() {
    FixtureOtpMetricsTool tool =
        new FixtureOtpMetricsTool(
            FixtureCatalog.forFixture(FixtureId.OTP_NORMAL_001), Clock.fixed(NOW, ZoneOffset.UTC));

    ToolResult<OtpMetricsResult> result =
        tool.getOtpMetrics(
            new OtpMetricsRequest(
                Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z"), true));

    assertThat(result.data().successRate()).isEqualTo(98.40);
  }
}
