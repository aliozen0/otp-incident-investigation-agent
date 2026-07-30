package com.example.otpsentinel.domain;

import java.util.List;

public record ValidationReport(ValidationStatus status, List<String> warnings) {

  public ValidationReport {
    if (status == null) {
      throw new IllegalArgumentException("status must not be null");
    }
    warnings = warnings == null ? List.of() : List.copyOf(warnings);
  }

  public static ValidationReport passed(List<String> warnings) {
    return new ValidationReport(ValidationStatus.PASSED, warnings);
  }

  public static ValidationReport failed(List<String> warnings) {
    return new ValidationReport(ValidationStatus.FAILED, warnings);
  }
}
