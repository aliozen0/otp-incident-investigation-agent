package com.example.otpsentinel.tools;

import java.util.Objects;

public record QueueHealthResult(
    long pendingMessages,
    long normalPendingThreshold,
    long oldestMessageAgeSeconds,
    long normalOldestAgeThresholdSeconds,
    int activeConsumers,
    int expectedConsumers,
    long deadLetterCount,
    String processingRateStatus,
    String status) {

  public QueueHealthResult {
    Objects.requireNonNull(processingRateStatus, "processingRateStatus must not be null");
    Objects.requireNonNull(status, "status must not be null");
  }
}
