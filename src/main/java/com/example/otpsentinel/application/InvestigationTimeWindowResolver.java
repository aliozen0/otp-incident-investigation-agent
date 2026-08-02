package com.example.otpsentinel.application;

import com.example.otpsentinel.domain.TimeWindow;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic investigation-only UTC time-window resolution shared by both API entry points. */
public final class InvestigationTimeWindowResolver {

  private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(15);
  private static final Pattern RELATIVE_WINDOW =
      Pattern.compile(
          "(?iu)\\b(?:son\\s+(\\d{1,4})\\s*(dakika(?:da|daki)?|dk|saat(?:te|teki)?)|last\\s+(\\d{1,4})\\s*(minutes?|hours?))\\b");

  private final Clock clock;

  public InvestigationTimeWindowResolver() {
    this(Clock.systemUTC());
  }

  public InvestigationTimeWindowResolver(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
  }

  public TimeWindow resolve(String message, Instant explicitStartAt, Instant explicitEndAt) {
    Instant now = clock.instant();
    if (explicitStartAt == null && explicitEndAt == null) {
      Duration duration = resolveRelativeDuration(message);
      return new TimeWindow(now.minus(duration), now);
    }
    if (explicitStartAt == null || explicitEndAt == null) {
      throw new IllegalArgumentException("startAt and endAt are required together");
    }
    if (explicitEndAt.isAfter(now) || explicitStartAt.isAfter(now)) {
      throw new IllegalArgumentException("time window must not end in the future");
    }
    return new TimeWindow(explicitStartAt, explicitEndAt);
  }

  private static Duration resolveRelativeDuration(String message) {
    Matcher matcher = RELATIVE_WINDOW.matcher(message);
    if (!matcher.find()) {
      return DEFAULT_WINDOW;
    }
    String amountText = matcher.group(1) != null ? matcher.group(1) : matcher.group(3);
    String unit = matcher.group(2) != null ? matcher.group(2) : matcher.group(4);
    long amount = Long.parseLong(amountText);
    return unit.toLowerCase(Locale.ROOT).startsWith("saat")
            || unit.toLowerCase(Locale.ROOT).startsWith("hour")
        ? Duration.ofHours(amount)
        : Duration.ofMinutes(amount);
  }
}
