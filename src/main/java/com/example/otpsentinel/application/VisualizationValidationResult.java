package com.example.otpsentinel.application;

import com.example.otpsentinel.domain.VisualizationSpec;
import java.util.List;

public record VisualizationValidationResult(
    List<VisualizationSpec> accepted, List<String> warnings) {
  public VisualizationValidationResult {
    accepted = List.copyOf(accepted);
    warnings = List.copyOf(warnings);
  }
}
