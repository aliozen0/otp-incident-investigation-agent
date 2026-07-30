package com.example.otpsentinel.tools;

/**
 * T-005: config/deploy/observation timeline. Read-only (FR-007). Timing-only correlation, no
 * causal claim (per docs/07-agent-tool-spec.md).
 */
public interface RecentChangesTool {

  ToolResult<RecentChangesResult> getRecentChanges(RecentChangesRequest request);
}
