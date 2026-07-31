package com.example.otpsentinel.application;

import com.example.otpsentinel.domain.TimeWindow;
import java.util.Objects;

/** Validated input needed to start one isolated investigation. */
public record InvestigationRequest(
    String question, TimeWindow resolvedTimeWindow, String promptVersion, String schemaVersion) {

  public InvestigationRequest {
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("question must not be blank");
    }
    Objects.requireNonNull(resolvedTimeWindow, "resolvedTimeWindow must not be null");
    if (promptVersion == null || promptVersion.isBlank()) {
      throw new IllegalArgumentException("promptVersion must not be blank");
    }
    if (schemaVersion == null || schemaVersion.isBlank()) {
      throw new IllegalArgumentException("schemaVersion must not be blank");
    }
  }
}
