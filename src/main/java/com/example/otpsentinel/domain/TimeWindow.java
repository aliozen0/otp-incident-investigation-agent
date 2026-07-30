package com.example.otpsentinel.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** DATA-001: instants are UTC/ISO-8601 by construction (java.time.Instant). */
public record TimeWindow(Instant startAt, Instant endAt) {

  private static final Duration MIN_DURATION = Duration.ofMinutes(1);
  private static final Duration MAX_DURATION = Duration.ofHours(24);

  public TimeWindow {
    Objects.requireNonNull(startAt, "startAt must not be null");
    Objects.requireNonNull(endAt, "endAt must not be null");
    if (!endAt.isAfter(startAt)) {
      throw new IllegalArgumentException("endAt must be after startAt");
    }
    Duration duration = Duration.between(startAt, endAt);
    if (duration.compareTo(MIN_DURATION) < 0) {
      throw new IllegalArgumentException("time window must be at least 1 minute");
    }
    if (duration.compareTo(MAX_DURATION) > 0) {
      throw new IllegalArgumentException("time window must be at most 24 hours");
    }
  }

  public Duration duration() {
    return Duration.between(startAt, endAt);
  }
}
