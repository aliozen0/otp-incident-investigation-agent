package com.example.otpsentinel.api.dto;

public record InvestigationRequestDto(
    String question, TimeWindowRangeDto timeWindow, String locale) {
  public record TimeWindowRangeDto(java.time.Instant startAt, java.time.Instant endAt) {}
}
