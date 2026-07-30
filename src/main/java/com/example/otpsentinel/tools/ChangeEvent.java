package com.example.otpsentinel.tools;

import java.time.Instant;
import java.util.Objects;

/**
 * {@code version} and {@code approved} are nullable: recent-changes fixture entries of type
 * OBSERVATION carry neither field (see docs/15-demo-fixtures.md).
 */
public record ChangeEvent(
    String changeId,
    Instant occurredAt,
    String type,
    String component,
    String description,
    String version,
    Boolean approved) {

  public ChangeEvent {
    Objects.requireNonNull(changeId, "changeId must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(component, "component must not be null");
    Objects.requireNonNull(description, "description must not be null");
  }
}
