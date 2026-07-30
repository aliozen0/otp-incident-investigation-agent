package com.example.otpsentinel.agent;

/** Id-only citation of an application-minted evidence id (ADR-008: the model cites, never mints). */
public record EvidenceReference(String evidenceId) {
  public EvidenceReference {
    if (evidenceId == null || evidenceId.isBlank()) {
      throw new IllegalArgumentException("evidenceId must not be blank");
    }
  }
}
