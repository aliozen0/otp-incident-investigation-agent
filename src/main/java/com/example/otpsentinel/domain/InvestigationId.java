package com.example.otpsentinel.domain;

import java.util.Objects;
import java.util.UUID;

public record InvestigationId(UUID value) {

  public InvestigationId {
    Objects.requireNonNull(value, "value must not be null");
  }

  public static InvestigationId generate() {
    return new InvestigationId(UUID.randomUUID());
  }

  public static InvestigationId of(String value) {
    return new InvestigationId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
