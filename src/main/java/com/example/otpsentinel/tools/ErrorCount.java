package com.example.otpsentinel.tools;

import java.util.Objects;

public record ErrorCount(String errorCode, long count, double share) {

  public ErrorCount {
    Objects.requireNonNull(errorCode, "errorCode must not be null");
    if (count < 0) {
      throw new IllegalArgumentException("count must not be negative");
    }
  }
}
