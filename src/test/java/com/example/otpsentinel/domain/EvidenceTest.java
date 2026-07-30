package com.example.otpsentinel.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class EvidenceTest {

  @Test
  void createsPlainObservationWithoutMetric() {
    Evidence evidence =
        new Evidence(
            "ev-1",
            "QUEUE_HEALTH",
            "tool:getQueueHealth:exec-1",
            "Queue healthy",
            Instant.parse("2026-07-30T11:30:00Z"),
            null,
            null,
            null);

    assertThat(evidence.isMetric()).isFalse();
  }

  @Test
  void createsMetricObservation() {
    Evidence evidence =
        new Evidence(
            "ev-2",
            "OTP_METRICS",
            "tool:getOtpMetrics:exec-1",
            "Success rate dropped",
            Instant.parse("2026-07-30T11:30:00Z"),
            "successRate.current",
            72.1,
            "percent");

    assertThat(evidence.isMetric()).isTrue();
  }

  @Test
  void rejectsBlankId() {
    assertThatThrownBy(
            () -> new Evidence("", "QUEUE_HEALTH", "ref", "obs", Instant.now(), null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
