package com.example.otpsentinel.agent.stub;

import java.util.List;
import java.util.Objects;

/** One turn of a scripted conversation: either "call these tools next" or "final answer is this JSON". */
public record StubScriptStep(List<PlannedToolCall> toolCalls, String finalAnswerJson) {

  public StubScriptStep {
    toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
  }

  public static StubScriptStep callTools(PlannedToolCall... calls) {
    return new StubScriptStep(List.of(calls), null);
  }

  public static StubScriptStep finalAnswer(String json) {
    return new StubScriptStep(List.of(), Objects.requireNonNull(json));
  }

  public boolean isFinal() {
    return finalAnswerJson != null;
  }

  public record PlannedToolCall(String toolName, java.util.Map<String, Object> arguments) {}
}
