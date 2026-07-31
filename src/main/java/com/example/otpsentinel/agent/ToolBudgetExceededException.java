package com.example.otpsentinel.agent;

public final class ToolBudgetExceededException extends RuntimeException {
  public ToolBudgetExceededException(String message) {
    super(message);
  }
}
