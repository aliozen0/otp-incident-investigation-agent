package com.example.otpsentinel.agent;

import java.util.concurrent.TimeoutException;

public final class ToolTimeoutException extends RuntimeException {
  public ToolTimeoutException(TimeoutException cause) {
    super("tool call timed out", cause);
  }
}
