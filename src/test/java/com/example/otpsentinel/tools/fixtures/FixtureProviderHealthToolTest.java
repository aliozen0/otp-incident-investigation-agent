package com.example.otpsentinel.tools.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.tools.ProviderHealthRequest;
import com.example.otpsentinel.tools.ProviderHealthResult;
import com.example.otpsentinel.tools.ToolResult;
import com.example.otpsentinel.tools.ToolStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FixtureProviderHealthToolTest {

  private static final Instant START = Instant.parse("2026-07-30T11:15:00Z");
  private static final Instant END = Instant.parse("2026-07-30T11:30:00Z");

  @Test
  void returnsOperatorBHealthVerbatimFromDemoFixturesForOtpDrop001() {
    FixtureProviderHealthTool tool = new FixtureProviderHealthTool(FixtureCatalog.forFixture(FixtureId.OTP_DROP_001));

    ToolResult<ProviderHealthResult> result =
        tool.getProviderHealth(new ProviderHealthRequest("OPERATOR_B", START, END));

    assertThat(result.status()).isEqualTo(ToolStatus.SUCCESS);
    ProviderHealthResult data = result.data();
    assertThat(data.status()).isEqualTo("DEGRADED");
    assertThat(data.averageResponseSeconds()).isEqualTo(13.9);
    assertThat(data.timeoutRate()).isEqualTo(0.31);
    assertThat(data.lastSuccessfulRequestAt()).isEqualTo(Instant.parse("2026-07-30T11:29:42Z"));
    assertThat(data.circuitBreakerState()).isEqualTo("HALF_OPEN");
    assertThat(data.activeConnections()).isEqualTo(48);
    assertThat(data.maxConnections()).isEqualTo(50);
  }

  @Test
  void otpPartial001TimesOutForOperatorB() {
    FixtureProviderHealthTool tool =
        new FixtureProviderHealthTool(FixtureCatalog.forFixture(FixtureId.OTP_PARTIAL_001));

    ToolResult<ProviderHealthResult> result =
        tool.getProviderHealth(new ProviderHealthRequest("OPERATOR_B", START, END));

    assertThat(result.status()).isEqualTo(ToolStatus.TIMEOUT);
    assertThat(result.data()).isNull();
    assertThat(result.error()).isNotNull();
    assertThat(result.error().code()).isEqualTo("TIMEOUT");
  }

  @Test
  void unknownProviderYieldsAnErrorResultInsteadOfFabricatedData() {
    FixtureProviderHealthTool tool = new FixtureProviderHealthTool(FixtureCatalog.forFixture(FixtureId.OTP_DROP_001));

    ToolResult<ProviderHealthResult> result =
        tool.getProviderHealth(new ProviderHealthRequest("OPERATOR_A", START, END));

    assertThat(result.status()).isEqualTo(ToolStatus.ERROR);
    assertThat(result.error().code()).isEqualTo("PROVIDER_NOT_FOUND");
  }
}
