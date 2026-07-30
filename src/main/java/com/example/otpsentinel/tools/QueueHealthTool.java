package com.example.otpsentinel.tools;

/** T-003: OTP outbound queue health. Read-only (FR-007), no input per docs/07-agent-tool-spec.md. */
public interface QueueHealthTool {

  ToolResult<QueueHealthResult> getQueueHealth();
}
