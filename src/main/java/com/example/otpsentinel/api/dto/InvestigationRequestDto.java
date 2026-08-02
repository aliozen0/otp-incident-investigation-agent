package com.example.otpsentinel.api.dto;

public record InvestigationRequestDto(
    String question,
    TimeWindowRangeDto timeWindow,
    String locale,
    String sessionId,
    String modelId,
    String mode) {
  public record TimeWindowRangeDto(java.time.Instant startAt, java.time.Instant endAt) {}
}
