package com.example.otpsentinel.tools;

import java.time.Instant;
import java.util.Objects;

public record RecentChangesRequest(Instant from, Instant to, String component) {

  public RecentChangesRequest {
    Objects.requireNonNull(from, "from must not be null");
    Objects.requireNonNull(to, "to must not be null");
    if (!to.isAfter(from)) {
      throw new IllegalArgumentException("to must be after from");
    }
  }
}
