package com.example.otpsentinel.tools;

import java.time.Instant;
import java.util.Objects;

/** docs/07-agent-tool-spec.md common result envelope for every operational tool call. */
public record ToolResult<T>(
    String executionId,
    String toolName,
    ToolStatus status,
    Instant observedAt,
    T data,
    ToolError error) {

  public ToolResult {
    Objects.requireNonNull(executionId, "executionId must not be null");
    Objects.requireNonNull(toolName, "toolName must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(observedAt, "observedAt must not be null");
    if (status == ToolStatus.SUCCESS && data == null) {
      throw new IllegalArgumentException("SUCCESS result must carry data");
    }
    if (status != ToolStatus.SUCCESS && error == null) {
      throw new IllegalArgumentException("non-SUCCESS result must carry error");
    }
  }

  public static <T> ToolResult<T> success(
      String executionId, String toolName, Instant observedAt, T data) {
    return new ToolResult<>(executionId, toolName, ToolStatus.SUCCESS, observedAt, data, null);
  }

  public static <T> ToolResult<T> timeout(
      String executionId, String toolName, Instant observedAt, ToolError error) {
    return new ToolResult<>(executionId, toolName, ToolStatus.TIMEOUT, observedAt, null, error);
  }

  public static <T> ToolResult<T> error(
      String executionId, String toolName, Instant observedAt, ToolError error) {
    return new ToolResult<>(executionId, toolName, ToolStatus.ERROR, observedAt, null, error);
  }
}
