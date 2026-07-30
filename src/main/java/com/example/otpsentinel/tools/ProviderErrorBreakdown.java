package com.example.otpsentinel.tools;

import java.util.Objects;

public record ProviderErrorBreakdown(String provider, long total, long delivered, long failed, double successRate) {

  public ProviderErrorBreakdown {
    Objects.requireNonNull(provider, "provider must not be null");
    if (delivered + failed != total) {
      throw new IllegalArgumentException("delivered + failed must equal total");
    }
  }
}
