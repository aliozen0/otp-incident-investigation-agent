package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.InvestigationRequestDto;
import com.example.otpsentinel.application.InvestigationTimeWindowResolver;
import com.example.otpsentinel.domain.TimeWindow;
import java.time.Clock;
import java.util.Set;

public final class InvestigationRequestValidator {

  private static final Set<String> ALLOWED_LOCALES = Set.of("tr-TR", "en-US");
  private final InvestigationTimeWindowResolver timeWindowResolver;

  public InvestigationRequestValidator() {
    this(Clock.systemUTC());
  }

  InvestigationRequestValidator(Clock clock) {
    this.timeWindowResolver = new InvestigationTimeWindowResolver(clock);
  }

  public TimeWindow validate(InvestigationRequestDto request) {
    if (request.question() == null
        || request.question().length() < 10
        || request.question().length() > 1000) {
      throw new ApiException(
          400,
          "INVALID_REQUEST",
          "Invalid request",
          "question must be between 10 and 1000 characters");
    }
    if (request.locale() != null && !ALLOWED_LOCALES.contains(request.locale())) {
      throw new ApiException(
          400,
          "INVALID_REQUEST",
          "Invalid request",
          "locale is not in the allowlist: " + request.locale());
    }
    try {
      return timeWindowResolver.resolve(
          request.question(),
          request.timeWindow() == null ? null : request.timeWindow().startAt(),
          request.timeWindow() == null ? null : request.timeWindow().endAt());
    } catch (IllegalArgumentException e) {
      throw new ApiException(400, "INVALID_TIME_WINDOW", "Invalid time window", e.getMessage());
    }
  }

  public com.example.otpsentinel.agent.InvestigationMode resolveMode(String mode) {
    if (mode == null || mode.isBlank()) {
      return com.example.otpsentinel.agent.InvestigationMode.THOROUGH;
    }
    try {
      return com.example.otpsentinel.agent.InvestigationMode.valueOf(
          mode.toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new ApiException(
          400, "INVALID_REQUEST", "Invalid request", "mode must be 'quick' or 'thorough'");
    }
  }
}
