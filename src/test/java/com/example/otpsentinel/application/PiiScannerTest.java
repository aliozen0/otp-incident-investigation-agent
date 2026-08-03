package com.example.otpsentinel.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiiScannerTest {

  private final PiiScanner scanner = new PiiScanner();

  @Test
  void detectsOtpValue() {
    assertThat(scanner.scan("customer OTP is 482913")).contains("OTP_VALUE");
  }

  @Test
  void detectsPhoneNumber() {
    assertThat(scanner.scan("call +1 555-123-4567 for support")).contains("PHONE_NUMBER");
  }

  @Test
  void detectsApiKeyLikeToken() {
    assertThat(scanner.scan("api_key: sk-abcdef1234567890")).contains("API_KEY_OR_SECRET");
  }

  @Test
  void doesNotFlagTheInvestigatedTimeWindow() {
    // Regression: an ISO instant is a dash-separated digit run, so the phone heuristic used to
    // reject correct analyses that quoted their own time window.
    assertThat(
            scanner.scan(
                "Verify provider timeouts during window 2026-08-03T19:29:28Z/2026-08-03T19:44:28Z"))
        .isEmpty();
  }

  @Test
  void doesNotFlagOrdinaryMetricsSummary() {
    assertThat(
            scanner.scan(
                "OTP success rate dropped to 72.10% with OPERATOR_B using 850/1000 connections"
                    + " on 2026-07-30"))
        .isEmpty();
  }
}
