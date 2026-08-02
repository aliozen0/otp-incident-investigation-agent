package com.example.otpsentinel.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IntentDecisionValidatorTest {

  private final IntentDecisionValidator validator = new IntentDecisionValidator();

  @Test
  void acceptsChatWithoutClarificationQuestion() {
    IntentDecision decision =
        validator.validate(new IntentDecision(IntentType.CHAT, 0.91, "assistant capability", null));

    assertThat(decision.intent()).isEqualTo(IntentType.CHAT);
  }

  @Test
  void requiresOneQuestionOnlyForClarification() {
    assertThatThrownBy(
            () ->
                validator.validate(
                    new IntentDecision(IntentType.CLARIFICATION, 0.65, "operator health", null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("clarificationQuestion");

    assertThatThrownBy(
            () ->
                validator.validate(
                    new IntentDecision(
                        IntentType.INVESTIGATION, 0.9, "investigate drop", "Which provider?")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("only");
  }

  @Test
  void rejectsOutOfRangeConfidenceAndPii() {
    assertThatThrownBy(
            () -> validator.validate(new IntentDecision(IntentType.CHAT, 1.01, "hello", null)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                validator.validate(
                    new IntentDecision(IntentType.CHAT, 0.8, "call +90 555 123 45 67", null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("PII");
  }
}
