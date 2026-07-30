package com.example.otpsentinel.agent.stub;

import java.util.List;

/** A fixed sequence of {@link StubScriptStep}s a {@link StubChatModel} replays in order. */
public record StubScript(List<StubScriptStep> steps) {

  public StubScript {
    if (steps == null || steps.isEmpty()) {
      throw new IllegalArgumentException("steps must not be empty");
    }
    steps = List.copyOf(steps);
  }
}
