package com.example.otpsentinel.agent;

import com.example.otpsentinel.tools.ToolStatus;
import java.util.List;
import java.util.Objects;

/** What the model actually sees for a tool call: raw data plus the evidence ids it may cite. */
public record AgentToolResponse<T>(ToolStatus status, T data, List<String> evidenceIds, String errorMessage) {

  public AgentToolResponse {
    Objects.requireNonNull(status, "status must not be null");
    evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
  }
}
