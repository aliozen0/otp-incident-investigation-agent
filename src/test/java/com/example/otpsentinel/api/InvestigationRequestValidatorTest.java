package com.example.otpsentinel.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.otpsentinel.api.dto.InvestigationRequestDto;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class InvestigationRequestValidatorTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-08-02T18:30:00Z");
  private final InvestigationRequestValidator validator = new InvestigationRequestValidator();

  @Test
  void resolvesTurkishRelativeMinutesWhenExplicitWindowIsMissing() {
    InvestigationRequestValidator fixedValidator =
        new InvestigationRequestValidator(Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    InvestigationRequestDto request =
        new InvestigationRequestDto(
            "Son 15 dakikada OTP başarı oranı neden düştü?", null, "tr-TR", null, null, null);

    assertThat(fixedValidator.validate(request))
        .extracting(window -> window.startAt(), window -> window.endAt())
        .containsExactly(FIXED_NOW.minus(15, ChronoUnit.MINUTES), FIXED_NOW);
  }

  @Test
  void defaultsToLastFifteenMinutesForActionableQuestionWithoutTimeExpression() {
    InvestigationRequestValidator fixedValidator =
        new InvestigationRequestValidator(Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    InvestigationRequestDto request =
        new InvestigationRequestDto(
            "OTP başarı oranındaki düşüşü operatör bazında incele",
            null,
            "tr-TR",
            null,
            null,
            null);

    assertThat(fixedValidator.validate(request))
        .extracting(window -> window.startAt(), window -> window.endAt())
        .containsExactly(FIXED_NOW.minus(15, ChronoUnit.MINUTES), FIXED_NOW);
  }

  @Test
  void rejectsRelativeWindowLongerThanTwentyFourHours() {
    InvestigationRequestValidator fixedValidator =
        new InvestigationRequestValidator(Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    InvestigationRequestDto request =
        new InvestigationRequestDto(
            "Son 25 saatte OTP başarı oranını incele", null, "tr-TR", null, null, null);

    assertThatThrownBy(() -> fixedValidator.validate(request))
        .isInstanceOf(ApiException.class)
        .extracting(e -> ((ApiException) e).errorCode())
        .isEqualTo("INVALID_TIME_WINDOW");
  }

  @Test
  void resolveModeDefaultsToThoroughAndAcceptsKnownModesCaseInsensitively() {
    assertThat(validator.resolveMode(null))
        .isEqualTo(com.example.otpsentinel.agent.InvestigationMode.THOROUGH);
    assertThat(validator.resolveMode("  "))
        .isEqualTo(com.example.otpsentinel.agent.InvestigationMode.THOROUGH);
    assertThat(validator.resolveMode("quick"))
        .isEqualTo(com.example.otpsentinel.agent.InvestigationMode.QUICK);
    assertThat(validator.resolveMode("Thorough"))
        .isEqualTo(com.example.otpsentinel.agent.InvestigationMode.THOROUGH);
  }

  @Test
  void resolveModeRejectsAnUnknownModeWith400() {
    assertThatThrownBy(() -> validator.resolveMode("blazing"))
        .isInstanceOf(ApiException.class)
        .satisfies(
            e -> {
              assertThat(((ApiException) e).status()).isEqualTo(400);
              assertThat(((ApiException) e).errorCode()).isEqualTo("INVALID_REQUEST");
            });
  }

  @Test
  void rejectsFutureEndTime() {
    InvestigationRequestDto request =
        new InvestigationRequestDto(
            "why did OTP success rate drop suddenly",
            new InvestigationRequestDto.TimeWindowRangeDto(
                Instant.now().minus(10, ChronoUnit.MINUTES),
                Instant.now().plus(5, ChronoUnit.MINUTES)),
            "tr-TR",
            null,
            null,
            null);

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(ApiException.class)
        .extracting(e -> ((ApiException) e).errorCode())
        .isEqualTo("INVALID_TIME_WINDOW");
  }

  @Test
  void rejectsIntervalLongerThan24Hours() {
    Instant end = Instant.now().minus(1, ChronoUnit.MINUTES);
    InvestigationRequestDto request =
        new InvestigationRequestDto(
            "why did OTP success rate drop suddenly",
            new InvestigationRequestDto.TimeWindowRangeDto(end.minus(25, ChronoUnit.HOURS), end),
            "tr-TR",
            null,
            null,
            null);

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(ApiException.class)
        .extracting(e -> ((ApiException) e).errorCode())
        .isEqualTo("INVALID_TIME_WINDOW");
  }

  @Test
  void acceptsAValidWindow() {
    Instant end = Instant.now().minus(1, ChronoUnit.MINUTES);
    InvestigationRequestDto request =
        new InvestigationRequestDto(
            "why did OTP success rate drop suddenly",
            new InvestigationRequestDto.TimeWindowRangeDto(end.minus(15, ChronoUnit.MINUTES), end),
            "tr-TR",
            null,
            null,
            null);

    assertThat(validator.validate(request)).isNotNull();
  }

  @Test
  void rejectsNullStartAt() {
    Instant end = Instant.now().minus(1, ChronoUnit.MINUTES);
    InvestigationRequestDto request =
        new InvestigationRequestDto(
            "why did OTP success rate drop suddenly",
            new InvestigationRequestDto.TimeWindowRangeDto(null, end),
            "tr-TR",
            null,
            null,
            null);

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(ApiException.class)
        .extracting(e -> ((ApiException) e).errorCode())
        .isEqualTo("INVALID_TIME_WINDOW");
  }

  @Test
  void rejectsNullEndAt() {
    Instant start = Instant.now().minus(15, ChronoUnit.MINUTES);
    InvestigationRequestDto request =
        new InvestigationRequestDto(
            "why did OTP success rate drop suddenly",
            new InvestigationRequestDto.TimeWindowRangeDto(start, null),
            "tr-TR",
            null,
            null,
            null);

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(ApiException.class)
        .extracting(e -> ((ApiException) e).errorCode())
        .isEqualTo("INVALID_TIME_WINDOW");
  }
}
