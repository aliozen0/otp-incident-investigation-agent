package com.example.otpsentinel.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Invariant 6: a high-risk action is never auto-executed. */
class RecommendedActionTest {

  @Test
  void acceptsHighRiskActionThatRequiresApproval() {
    RecommendedAction action =
        new RecommendedAction(
            ActionType.CHANGE_PROPOSAL,
            "Propose rollback to change management",
            Severity.HIGH,
            true,
            ExecutionMode.DRAFT_ONLY);

    assertThat(action.requiresApproval()).isTrue();
  }

  @Test
  void rejectsHighRiskActionWithoutApproval() {
    assertThatThrownBy(
            () ->
                new RecommendedAction(
                    ActionType.RESTART,
                    "Restart the gateway",
                    Severity.HIGH,
                    false,
                    ExecutionMode.MANUAL_CHECK))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsLowRiskActionWithoutApproval() {
    RecommendedAction action =
        new RecommendedAction(
            ActionType.MANUAL_CHECK,
            "Inspect connection pool",
            Severity.LOW,
            false,
            ExecutionMode.MANUAL_CHECK);

    assertThat(action.requiresApproval()).isFalse();
  }
}
