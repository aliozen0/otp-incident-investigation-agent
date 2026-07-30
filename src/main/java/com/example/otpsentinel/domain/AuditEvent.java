package com.example.otpsentinel.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Append-only audit record (FR-017, DATA-005). Fields follow docs/09-security-governance.md "Audit
 * alanları" exactly; callers must never place secrets/OTP/phone/PII into {@code result} or {@code
 * correlationId}.
 */
public record AuditEvent(
    UUID id,
    Instant occurredAt,
    String actor,
    AuditEventType action,
    InvestigationId investigationId,
    IncidentDraftId approvalId,
    String correlationId,
    String result,
    String policyVersion) {

  public AuditEvent {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    if (actor == null || actor.isBlank()) {
      throw new IllegalArgumentException("actor must not be blank");
    }
    Objects.requireNonNull(action, "action must not be null");
    if (result == null || result.isBlank()) {
      throw new IllegalArgumentException("result must not be blank");
    }
  }

  public static AuditEvent of(
      String actor,
      AuditEventType action,
      InvestigationId investigationId,
      IncidentDraftId approvalId,
      String correlationId,
      String result,
      String policyVersion) {
    return new AuditEvent(
        UUID.randomUUID(),
        Instant.now(),
        actor,
        action,
        investigationId,
        approvalId,
        correlationId,
        result,
        policyVersion);
  }
}
