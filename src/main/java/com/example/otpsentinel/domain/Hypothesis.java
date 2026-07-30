package com.example.otpsentinel.domain;

import java.util.List;

/** Invariant 3: every hypothesis must carry at least one supporting evidence id. */
public record Hypothesis(
    int rank,
    String possibleCause,
    double probability,
    List<String> supportingEvidenceIds,
    List<String> contradictingEvidenceIds,
    List<String> verificationSteps) {

  public Hypothesis {
    if (rank < 1) {
      throw new IllegalArgumentException("rank must be >= 1");
    }
    if (possibleCause == null || possibleCause.isBlank()) {
      throw new IllegalArgumentException("possibleCause must not be blank");
    }
    if (probability < 0.0 || probability > 1.0) {
      throw new IllegalArgumentException("probability must be within 0..1");
    }
    if (supportingEvidenceIds == null || supportingEvidenceIds.isEmpty()) {
      throw new IllegalArgumentException(
          "hypothesis must carry at least one supporting evidence id");
    }
    supportingEvidenceIds = List.copyOf(supportingEvidenceIds);
    contradictingEvidenceIds =
        contradictingEvidenceIds == null ? List.of() : List.copyOf(contradictingEvidenceIds);
    verificationSteps = verificationSteps == null ? List.of() : List.copyOf(verificationSteps);
  }
}
