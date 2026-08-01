package com.example.otpsentinel.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.otpsentinel.api.dto.InvestigationRequestDto;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class InvestigationRequestValidatorTest {

  private final InvestigationRequestValidator validator = new InvestigationRequestValidator();

  @Test
  void rejectsFutureEndTime() {
    InvestigationRequestDto request = new InvestigationRequestDto(
        "why did OTP success rate drop suddenly",
        new InvestigationRequestDto.TimeWindowRangeDto(
            Instant.now().minus(10, ChronoUnit.MINUTES), Instant.now().plus(5, ChronoUnit.MINUTES)),
        "tr-TR");

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(ApiException.class)
        .extracting(e -> ((ApiException) e).errorCode())
        .isEqualTo("INVALID_TIME_WINDOW");
  }

  @Test
  void rejectsIntervalLongerThan24Hours() {
    Instant end = Instant.now().minus(1, ChronoUnit.MINUTES);
    InvestigationRequestDto request = new InvestigationRequestDto(
        "why did OTP success rate drop suddenly",
        new InvestigationRequestDto.TimeWindowRangeDto(end.minus(25, ChronoUnit.HOURS), end), "tr-TR");

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(ApiException.class)
        .extracting(e -> ((ApiException) e).errorCode())
        .isEqualTo("INVALID_TIME_WINDOW");
  }

  @Test
  void acceptsAValidWindow() {
    Instant end = Instant.now().minus(1, ChronoUnit.MINUTES);
    InvestigationRequestDto request = new InvestigationRequestDto(
        "why did OTP success rate drop suddenly",
        new InvestigationRequestDto.TimeWindowRangeDto(end.minus(15, ChronoUnit.MINUTES), end), "tr-TR");

    assertThat(validator.validate(request)).isNotNull();
  }
}
