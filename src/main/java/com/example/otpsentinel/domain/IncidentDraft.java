package com.example.otpsentinel.domain;

import java.time.Instant;

/**
 * IncidentDraft aggregate root. Lifecycle: PREVIEWED -&gt; APPROVED -&gt; CREATED (or PREVIEWED
 * -&gt; REJECTED).
 */
public final class IncidentDraft {

  private final IncidentDraftId id;
  private final InvestigationId investigationId;
  private final String payload;
  private final String idempotencyKey;

  private IncidentDraftStatus status;
  private Approval approval;
  private String externalIncidentId;

  private IncidentDraft(
      IncidentDraftId id, InvestigationId investigationId, String payload, String idempotencyKey) {
    this.id = id;
    this.investigationId = investigationId;
    this.payload = payload;
    this.idempotencyKey = idempotencyKey;
    this.status = IncidentDraftStatus.PREVIEWED;
  }

  public static IncidentDraft preview(
      InvestigationId investigationId, String payload, String idempotencyKey) {
    if (investigationId == null) {
      throw new IllegalArgumentException("investigationId must not be null");
    }
    if (payload == null || payload.isBlank()) {
      throw new IllegalArgumentException("payload must not be blank");
    }
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new IllegalArgumentException("idempotencyKey must not be blank");
    }
    return new IncidentDraft(IncidentDraftId.generate(), investigationId, payload, idempotencyKey);
  }

  public void approve(String actor) {
    requireStatus(IncidentDraftStatus.PREVIEWED);
    this.approval = new Approval(actor, Instant.now(), ApprovalDecision.APPROVE, null);
    this.status = IncidentDraftStatus.APPROVED;
  }

  public void reject(String actor, String reason) {
    requireStatus(IncidentDraftStatus.PREVIEWED);
    this.approval = new Approval(actor, Instant.now(), ApprovalDecision.REJECT, reason);
    this.status = IncidentDraftStatus.REJECTED;
  }

  /**
   * Invariant 7: an incident is never created without approval. Invariant 8: the same idempotency
   * key always yields a single incident; once CREATED, further calls are a no-op returning the
   * original external id.
   */
  public void create(String externalIncidentId) {
    if (status == IncidentDraftStatus.CREATED) {
      return;
    }
    requireStatus(IncidentDraftStatus.APPROVED);
    this.externalIncidentId = externalIncidentId;
    this.status = IncidentDraftStatus.CREATED;
  }

  private void requireStatus(IncidentDraftStatus expected) {
    if (status != expected) {
      throw new IllegalStateException("expected status " + expected + " but was " + status);
    }
  }

  public IncidentDraftId id() {
    return id;
  }

  public InvestigationId investigationId() {
    return investigationId;
  }

  public String payload() {
    return payload;
  }

  public String idempotencyKey() {
    return idempotencyKey;
  }

  public IncidentDraftStatus status() {
    return status;
  }

  public Approval approval() {
    return approval;
  }

  public String externalIncidentId() {
    return externalIncidentId;
  }
}
