package com.example.otpsentinel.tools.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.tools.ErrorDistributionRequest;
import com.example.otpsentinel.tools.ErrorDistributionResult;
import com.example.otpsentinel.tools.ToolResult;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FixtureErrorDistributionToolTest {

  private static final Instant START = Instant.parse("2026-07-30T11:15:00Z");
  private static final Instant END = Instant.parse("2026-07-30T11:30:00Z");

  @Test
  void returnsOtpDrop001ErrorDistributionVerbatimFromDemoFixtures() {
    FixtureErrorDistributionTool tool =
        new FixtureErrorDistributionTool(FixtureCatalog.forFixture(FixtureId.OTP_DROP_001));

    ToolResult<ErrorDistributionResult> result =
        tool.getErrorDistribution(new ErrorDistributionRequest(START, END, null));

    ErrorDistributionResult data = result.data();
    assertThat(data.failedTotal()).isEqualTo(3_482L);
    assertThat(data.byErrorCode())
        .extracting("errorCode", "count")
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("PROVIDER_TIMEOUT", 2_228L),
            org.assertj.core.groups.Tuple.tuple("RATE_LIMITED", 627L),
            org.assertj.core.groups.Tuple.tuple("CONNECTION_RESET", 383L),
            org.assertj.core.groups.Tuple.tuple("INVALID_NUMBER", 139L),
            org.assertj.core.groups.Tuple.tuple("UNKNOWN", 105L));
    assertThat(data.byProvider())
        .extracting("provider")
        .containsExactly("OPERATOR_A", "OPERATOR_B", "OPERATOR_C");
  }

  @Test
  void filtersByProviderWhenRequested() {
    FixtureErrorDistributionTool tool =
        new FixtureErrorDistributionTool(FixtureCatalog.forFixture(FixtureId.OTP_DROP_001));

    ToolResult<ErrorDistributionResult> result =
        tool.getErrorDistribution(new ErrorDistributionRequest(START, END, "OPERATOR_B"));

    assertThat(result.data().byProvider()).extracting("provider").containsExactly("OPERATOR_B");
    assertThat(result.data().failedTotal()).isEqualTo(3_348L);
  }
}
