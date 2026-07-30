package com.example.otpsentinel.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TimeWindowTest {

  private static final Instant BASE = Instant.parse("2026-07-30T11:00:00Z");

  @Test
  void acceptsAWindowWithinBounds() {
    TimeWindow window = new TimeWindow(BASE, BASE.plusSeconds(900));

    assertThat(window.startAt()).isEqualTo(BASE);
    assertThat(window.endAt()).isEqualTo(BASE.plusSeconds(900));
  }

  @Test
  void rejectsEndAtNotAfterStartAt() {
    assertThatThrownBy(() -> new TimeWindow(BASE, BASE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsWindowShorterThanOneMinute() {
    assertThatThrownBy(() -> new TimeWindow(BASE, BASE.plusSeconds(30)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsWindowLongerThan24Hours() {
    assertThatThrownBy(() -> new TimeWindow(BASE, BASE.plusSeconds(25 * 3600)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
