package com.example.otpsentinel.domain;

/** Invariant 6: a high-risk action is never auto-executed; it always requires approval. */
public record RecommendedAction(
    ActionType actionType,
    String description,
    Severity risk,
    boolean requiresApproval,
    ExecutionMode executionMode) {

  public RecommendedAction {
    if (actionType == null) {
      throw new IllegalArgumentException("actionType must not be null");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("description must not be blank");
    }
    if (risk == null) {
      throw new IllegalArgumentException("risk must not be null");
    }
    if (executionMode == null) {
      throw new IllegalArgumentException("executionMode must not be null");
    }
    if (isHighRisk(risk) && !requiresApproval) {
      throw new IllegalArgumentException("high-risk action must require approval");
    }
  }

  private static boolean isHighRisk(Severity risk) {
    return risk == Severity.HIGH || risk == Severity.CRITICAL;
  }
}
