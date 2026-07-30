package com.example.otpsentinel.domain;

import java.time.Instant;
import java.util.Objects;

public record Evidence(
    String id,
    String sourceType,
    String sourceReference,
    String observation,
    Instant observedAt,
    String metricName,
    Double metricValue,
    String metricUnit) {

  public Evidence {
    requireNonBlank(id, "id");
    requireNonBlank(sourceType, "sourceType");
    requireNonBlank(sourceReference, "sourceReference");
    requireNonBlank(observation, "observation");
    Objects.requireNonNull(observedAt, "observedAt must not be null");
  }

  public boolean isMetric() {
    return metricName != null;
  }

  private static void requireNonBlank(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
