package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.InvestigationRequestDto;
import com.example.otpsentinel.domain.TimeWindow;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public final class InvestigationRequestValidator {

  private static final Set<String> ALLOWED_LOCALES = Set.of("tr-TR", "en-US");

  public TimeWindow validate(InvestigationRequestDto request) {
    if (request.question() == null
        || request.question().length() < 10
        || request.question().length() > 1000) {
      throw new ApiException(400, "INVALID_REQUEST", "Invalid request",
          "question must be between 10 and 1000 characters");
    }
    if (request.locale() != null && !ALLOWED_LOCALES.contains(request.locale())) {
      throw new ApiException(400, "INVALID_REQUEST", "Invalid request",
          "locale is not in the allowlist: " + request.locale());
    }
    if (request.timeWindow() == null) {
      throw new ApiException(400, "INVALID_TIME_WINDOW", "Invalid time window",
          "timeWindow is required (relative-time resolution is out of scope for M7)");
    }
    Instant startAt = request.timeWindow().startAt();
    Instant endAt = request.timeWindow().endAt();
    Instant now = Instant.now();
    if (endAt.isAfter(now) || startAt.isAfter(now)) {
      throw new ApiException(400, "INVALID_TIME_WINDOW", "Invalid time window",
          "time window must not end in the future");
    }
    try {
      return new TimeWindow(startAt, endAt);
    } catch (IllegalArgumentException e) {
      throw new ApiException(400, "INVALID_TIME_WINDOW", "Invalid time window", e.getMessage());
    }
  }
}
