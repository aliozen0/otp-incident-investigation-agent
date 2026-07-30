package com.example.otpsentinel.tools;

import com.example.otpsentinel.domain.TimeWindow;
import java.util.Objects;

public record OtpMetricsResult(
    TimeWindow currentWindow,
    long total,
    long delivered,
    long failed,
    double successRate,
    double averageDeliverySeconds,
    PeriodComparison previousPeriod) {

  public OtpMetricsResult {
    Objects.requireNonNull(currentWindow, "currentWindow must not be null");
    if (delivered + failed != total) {
      throw new IllegalArgumentException("delivered + failed must equal total");
    }
  }
}
