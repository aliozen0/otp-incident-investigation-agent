package com.example.otpsentinel.tools;

import java.time.Instant;
import java.util.Objects;

public record ProviderHealthResult(
    String provider,
    String status,
    double averageResponseSeconds,
    double timeoutRate,
    Instant lastSuccessfulRequestAt,
    String circuitBreakerState,
    int activeConnections,
    int maxConnections) {

  public ProviderHealthResult {
    Objects.requireNonNull(provider, "provider must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(circuitBreakerState, "circuitBreakerState must not be null");
  }
}
