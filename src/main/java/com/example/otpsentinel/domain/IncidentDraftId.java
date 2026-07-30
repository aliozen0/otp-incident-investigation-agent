package com.example.otpsentinel.domain;

import java.util.Objects;
import java.util.UUID;

public record IncidentDraftId(UUID value) {

  public IncidentDraftId {
    Objects.requireNonNull(value, "value must not be null");
  }

  public static IncidentDraftId generate() {
    return new IncidentDraftId(UUID.randomUUID());
  }

  public static IncidentDraftId of(String value) {
    return new IncidentDraftId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
