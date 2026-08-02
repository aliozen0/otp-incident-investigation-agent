package com.example.otpsentinel.api.dto;

import java.time.Instant;

public record ChatMessageRequestDto(
    String message,
    String sessionId,
    String modelId,
    String interactionMode,
    String investigationMode,
    TimeWindowRangeDto timeWindow,
    String locale) {

  public record TimeWindowRangeDto(Instant startAt, Instant endAt) {}
}
