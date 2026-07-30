package com.example.otpsentinel.tools;

/**
 * T-004: single-provider health snapshot. Read-only (FR-007). NFR-008: adapters may report a
 * TIMEOUT status instead of data when the underlying call does not complete in time.
 */
public interface ProviderHealthTool {

  ToolResult<ProviderHealthResult> getProviderHealth(ProviderHealthRequest request);
}
