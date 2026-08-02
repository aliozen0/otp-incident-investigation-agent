package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.InvestigationRequestDto;
import com.example.otpsentinel.domain.TimeWindow;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InvestigationRequestValidator {

  private static final Set<String> ALLOWED_LOCALES = Set.of("tr-TR", "en-US");
  private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(15);
  private static final Pattern RELATIVE_WINDOW =
      Pattern.compile(
          "(?iu)\\b(?:son\\s+(\\d{1,4})\\s*(dakika(?:da|daki)?|dk|saat(?:te|teki)?)|last\\s+(\\d{1,4})\\s*(minutes?|hours?))\\b");

  private final Clock clock;

  public InvestigationRequestValidator() {
    this(Clock.systemUTC());
  }

  InvestigationRequestValidator(Clock clock) {
    this.clock = clock;
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
      Instant now = clock.instant();
      if (request.timeWindow() == null) {
        Duration duration = resolveRelativeDuration(request.question());
        return new TimeWindow(now.minus(duration), now);
      }
      Instant startAt = request.timeWindow().startAt();
      Instant endAt = request.timeWindow().endAt();
      if (startAt == null || endAt == null) {
        throw new IllegalArgumentException("startAt and endAt are required");
      }
      if (endAt.isAfter(now) || startAt.isAfter(now)) {
        throw new IllegalArgumentException("time window must not end in the future");
      }
      return new TimeWindow(startAt, endAt);
    } catch (IllegalArgumentException e) {
      throw new ApiException(400, "INVALID_TIME_WINDOW", "Invalid time window", e.getMessage());
    }
  }

  private Duration resolveRelativeDuration(String question) {
    Matcher matcher = RELATIVE_WINDOW.matcher(question);
    if (!matcher.find()) {
      return DEFAULT_WINDOW;
    }
    String amountText = matcher.group(1) != null ? matcher.group(1) : matcher.group(3);
    String unit = matcher.group(2) != null ? matcher.group(2) : matcher.group(4);
    long amount = Long.parseLong(amountText);
    return unit.toLowerCase(java.util.Locale.ROOT).startsWith("saat")
            || unit.toLowerCase(java.util.Locale.ROOT).startsWith("hour")
        ? Duration.ofHours(amount)
        : Duration.ofMinutes(amount);
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
