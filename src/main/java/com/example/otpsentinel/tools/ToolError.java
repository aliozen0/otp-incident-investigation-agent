package com.example.otpsentinel.tools;

import java.util.Objects;

public record ToolError(String code, String message) {

  public ToolError {
    Objects.requireNonNull(code, "code must not be null");
    Objects.requireNonNull(message, "message must not be null");
  }
}
