package com.example.otpsentinel.tools;

import com.example.otpsentinel.domain.TimeWindow;
import java.util.Objects;

/**
 * Previous-period figures for T-001. Only fields actually published in docs/15-demo-fixtures.md are
 * modeled (delivered/failed for the previous period are not given there and must not be
 * derived/rounded into existence).
 */
public record PeriodComparison(
    TimeWindow window, long total, double successRate, double averageDeliverySeconds) {

  public PeriodComparison {
    Objects.requireNonNull(window, "window must not be null");
    if (total < 0) {
      throw new IllegalArgumentException("total must not be negative");
    }
  }
}
