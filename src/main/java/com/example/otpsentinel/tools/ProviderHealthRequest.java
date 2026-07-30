package com.example.otpsentinel.tools;

import java.time.Instant;
import java.util.Objects;

public record ProviderHealthRequest(String provider, Instant startAt, Instant endAt) {

  public ProviderHealthRequest {
    Objects.requireNonNull(provider, "provider must not be null");
    Objects.requireNonNull(startAt, "startAt must not be null");
    Objects.requireNonNull(endAt, "endAt must not be null");
    if (!endAt.isAfter(startAt)) {
      throw new IllegalArgumentException("endAt must be after startAt");
    }
  }
}
