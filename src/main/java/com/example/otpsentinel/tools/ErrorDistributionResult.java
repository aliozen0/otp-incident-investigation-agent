package com.example.otpsentinel.tools;

import java.util.List;
import java.util.Objects;

public record ErrorDistributionResult(
    long failedTotal, List<ErrorCount> byErrorCode, List<ProviderErrorBreakdown> byProvider) {

  public ErrorDistributionResult {
    Objects.requireNonNull(byErrorCode, "byErrorCode must not be null");
    Objects.requireNonNull(byProvider, "byProvider must not be null");
    byErrorCode = List.copyOf(byErrorCode);
    byProvider = List.copyOf(byProvider);
  }
}
