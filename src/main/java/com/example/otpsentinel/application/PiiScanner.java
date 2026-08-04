package com.example.otpsentinel.application;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Deterministic PII/secret scan (docs/09-security-governance.md "PII", AC-028). Coarse regex-based
 * heuristic, not a general-purpose PII detector: covers the three MVP-relevant shapes (OTP-labeled
 * codes, phone-number-shaped digit runs, API key/secret-labeled tokens).
 */
public final class PiiScanner {

  private static final Pattern OTP_VALUE = Pattern.compile("(?i)\\botp\\b\\D{0,10}\\d{4,8}\\b");
  private static final Pattern PHONE_NUMBER =
      Pattern.compile("(?<!\\d)\\+?(?:\\d[-. ]?){9,13}\\d\\b");
  private static final Pattern API_KEY =
      Pattern.compile("(?i)\\b(api[_-]?key|secret|token)\\b\\s*[:=]\\s*\\S{6,}");

  /**
   * Every analysis quotes the investigated time window, and an ISO-8601 instant is a long run of
   * digits separated by dashes — exactly the phone-number shape. Timestamps are masked out before
   * the phone scan so a correct report is not rejected as leaking a subscriber number.
   */
  private static final Pattern ISO_TIMESTAMP =
      Pattern.compile("\\d{4}-\\d{2}-\\d{2}(?:[T ]\\d{2}:\\d{2}(?::\\d{2}(?:\\.\\d+)?)?Z?)?");

  public Optional<String> scan(String text) {
    if (text == null || text.isBlank()) {
      return Optional.empty();
    }
    if (OTP_VALUE.matcher(text).find()) {
      return Optional.of("OTP_VALUE");
    }
    if (API_KEY.matcher(text).find()) {
      return Optional.of("API_KEY_OR_SECRET");
    }
    if (PHONE_NUMBER.matcher(ISO_TIMESTAMP.matcher(text).replaceAll(" ")).find()) {
      return Optional.of("PHONE_NUMBER");
    }
    return Optional.empty();
  }
}
