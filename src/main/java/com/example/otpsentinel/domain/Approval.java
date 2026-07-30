package com.example.otpsentinel.domain;

import java.time.Instant;
import java.util.Objects;

public record Approval(String actor, Instant decidedAt, ApprovalDecision decision, String reason) {

  public Approval {
    if (actor == null || actor.isBlank()) {
      throw new IllegalArgumentException("actor must not be blank");
    }
    Objects.requireNonNull(decidedAt, "decidedAt must not be null");
    Objects.requireNonNull(decision, "decision must not be null");
    if (decision == ApprovalDecision.REJECT && (reason == null || reason.isBlank())) {
      throw new IllegalArgumentException("reason is required for a rejection");
    }
  }
}
