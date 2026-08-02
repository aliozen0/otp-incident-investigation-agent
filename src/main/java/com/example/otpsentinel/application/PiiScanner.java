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
      Pattern.compile(
          "\\+?\\d{2,3}(?:[-. ]?\\d{3}){2}[-. ]?\\d{2,4}\\b|\\+?\\d{10,14}\\b");
  private static final Pattern API_KEY =
      Pattern.compile("(?i)\\b(api[_-]?key|secret|token)\\b\\s*[:=]\\s*\\S{6,}");

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
    if (PHONE_NUMBER.matcher(text).find()) {
      return Optional.of("PHONE_NUMBER");
    }
    return Optional.empty();
  }
}
