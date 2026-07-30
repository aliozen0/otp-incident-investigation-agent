package com.example.otpsentinel.tools.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.tools.RecentChangesRequest;
import com.example.otpsentinel.tools.RecentChangesResult;
import com.example.otpsentinel.tools.ToolResult;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FixtureRecentChangesToolTest {

  private static final Instant FROM = Instant.parse("2026-07-30T11:00:00Z");
  private static final Instant TO = Instant.parse("2026-07-30T11:30:00Z");

  @Test
  void returnsAllFourOtpDrop001ChangesVerbatimFromDemoFixtures() {
    FixtureRecentChangesTool tool = new FixtureRecentChangesTool(FixtureCatalog.forFixture(FixtureId.OTP_DROP_001));

    ToolResult<RecentChangesResult> result =
        tool.getRecentChanges(new RecentChangesRequest(FROM, TO, null));

    assertThat(result.data().changes())
        .extracting("changeId")
        .containsExactly("chg-101", "chg-102", "obs-103", "obs-104");
    assertThat(result.data().changes().get(1).version()).isEqualTo("v2.4");
    assertThat(result.data().changes().get(2).approved()).isNull();
  }

  @Test
  void filtersByComponentWhenRequested() {
    FixtureRecentChangesTool tool = new FixtureRecentChangesTool(FixtureCatalog.forFixture(FixtureId.OTP_DROP_001));

    ToolResult<RecentChangesResult> result =
        tool.getRecentChanges(new RecentChangesRequest(FROM, TO, "OPERATOR_B_ADAPTER"));

    assertThat(result.data().changes()).extracting("changeId").containsExactly("obs-103");
  }
}
