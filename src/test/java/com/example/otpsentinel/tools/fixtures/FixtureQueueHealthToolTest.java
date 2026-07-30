package com.example.otpsentinel.tools.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.tools.QueueHealthResult;
import com.example.otpsentinel.tools.ToolResult;
import org.junit.jupiter.api.Test;

class FixtureQueueHealthToolTest {

  @Test
  void returnsOtpDrop001QueueHealthVerbatimFromDemoFixtures() {
    FixtureQueueHealthTool tool = new FixtureQueueHealthTool(FixtureCatalog.forFixture(FixtureId.OTP_DROP_001));

    ToolResult<QueueHealthResult> result = tool.getQueueHealth();

    QueueHealthResult data = result.data();
    assertThat(data.pendingMessages()).isEqualTo(184L);
    assertThat(data.normalPendingThreshold()).isEqualTo(1_000L);
    assertThat(data.oldestMessageAgeSeconds()).isEqualTo(4L);
    assertThat(data.normalOldestAgeThresholdSeconds()).isEqualTo(30L);
    assertThat(data.activeConsumers()).isEqualTo(8);
    assertThat(data.expectedConsumers()).isEqualTo(8);
    assertThat(data.deadLetterCount()).isEqualTo(3L);
    assertThat(data.processingRateStatus()).isEqualTo("NORMAL");
    assertThat(data.status()).isEqualTo("HEALTHY");
  }
}
