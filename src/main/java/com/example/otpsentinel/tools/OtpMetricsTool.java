package com.example.otpsentinel.tools;

/** T-001: current vs. previous OTP delivery performance. Read-only (FR-007). */
public interface OtpMetricsTool {

  ToolResult<OtpMetricsResult> getOtpMetrics(OtpMetricsRequest request);
}
