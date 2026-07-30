package com.example.otpsentinel.tools;

import java.time.Instant;
import java.util.Objects;

public record ErrorDistributionRequest(Instant startAt, Instant endAt, String provider) {

  public ErrorDistributionRequest {
    Objects.requireNonNull(startAt, "startAt must not be null");
    Objects.requireNonNull(endAt, "endAt must not be null");
    if (!endAt.isAfter(startAt)) {
      throw new IllegalArgumentException("endAt must be after startAt");
    }
  }
}
