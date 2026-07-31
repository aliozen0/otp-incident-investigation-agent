package com.example.otpsentinel.agent;

public final class DuplicateToolCallException extends RuntimeException {
  public DuplicateToolCallException(String message) {
    super(message);
  }
}
