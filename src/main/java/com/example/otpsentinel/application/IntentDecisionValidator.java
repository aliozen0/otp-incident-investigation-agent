package com.example.otpsentinel.application;

import java.util.List;

/** Fail-closed validation for the model-owned semantic route; no routing policy lives here. */
public final class IntentDecisionValidator {

  private static final int MAX_NORMALIZED_REQUEST = 500;
  private static final int MAX_CLARIFICATION = 300;
  private final PiiScanner piiScanner = new PiiScanner();

  public IntentDecision validate(IntentDecision decision) {
    if (decision == null || decision.intent() == null) {
      throw new IllegalArgumentException("intent must not be null");
    }
    if (!Double.isFinite(decision.confidence())
        || decision.confidence() < 0
        || decision.confidence() > 1) {
      throw new IllegalArgumentException("confidence must be within 0..1");
    }
    requirePlainText(decision.normalizedRequest(), "normalizedRequest", 1, MAX_NORMALIZED_REQUEST);
    boolean clarification = decision.intent() == IntentType.CLARIFICATION;
    if (clarification) {
      requirePlainText(
          decision.clarificationQuestion(), "clarificationQuestion", 1, MAX_CLARIFICATION);
    } else if (decision.clarificationQuestion() != null
        && !decision.clarificationQuestion().isBlank()) {
      throw new IllegalArgumentException("clarificationQuestion is allowed only for CLARIFICATION");
    }
    return new IntentDecision(
        decision.intent(),
        decision.confidence(),
        decision.normalizedRequest().trim(),
        clarification ? decision.clarificationQuestion().trim() : null);
  }

  public List<String> validateSuggestions(List<String> suggestions) {
    if (suggestions == null) {
      return List.of();
    }
    if (suggestions.size() > 3) {
      throw new IllegalArgumentException("at most 3 suggestions are allowed");
    }
    return suggestions.stream()
        .map(
            suggestion -> {
              requirePlainText(suggestion, "suggestion", 1, 160);
              return suggestion.trim();
            })
        .distinct()
        .toList();
  }

  private void requirePlainText(String value, String field, int min, int max) {
    if (value == null || value.trim().length() < min || value.trim().length() > max) {
      throw new IllegalArgumentException(field + " length is invalid");
    }
    String trimmed = value.trim();
    String lower = trimmed.toLowerCase(java.util.Locale.ROOT);
    if (trimmed.indexOf('<') >= 0
        || trimmed.indexOf('>') >= 0
        || lower.contains("javascript:")
        || lower.contains("data:")) {
      throw new IllegalArgumentException(field + " must be plain text");
    }
    piiScanner
        .scan(trimmed)
        .ifPresent(
            hit -> {
              throw new IllegalArgumentException("PII detected in " + field);
            });
  }
}
