package com.example.otpsentinel.tools;

/** T-002: failure breakdown by error code and provider. Read-only (FR-007). */
public interface ErrorDistributionTool {

  ToolResult<ErrorDistributionResult> getErrorDistribution(ErrorDistributionRequest request);
}
