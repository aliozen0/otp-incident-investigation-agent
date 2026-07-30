package com.example.otpsentinel.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Invariant 3: every hypothesis must carry supporting evidence. */
class HypothesisTest {

  @Test
  void acceptsHypothesisWithSupportingEvidence() {
    Hypothesis hypothesis =
        new Hypothesis(
            1,
            "Connection pool exhaustion",
            0.85,
            List.of("ev-1", "ev-2"),
            List.of(),
            List.of("Inspect pool metrics"));

    assertThat(hypothesis.supportingEvidenceIds()).containsExactly("ev-1", "ev-2");
  }

  @Test
  void rejectsHypothesisWithoutSupportingEvidence() {
    assertThatThrownBy(
            () ->
                new Hypothesis(
                    1, "Connection pool exhaustion", 0.85, List.of(), List.of(), List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsProbabilityOutOfRange() {
    assertThatThrownBy(() -> new Hypothesis(1, "cause", 1.5, List.of("ev-1"), List.of(), List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
