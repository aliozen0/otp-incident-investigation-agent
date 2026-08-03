package com.example.otpsentinel.api.dto;

import java.util.List;

public record ChatMessageResponseDto(
    String messageId,
    String sessionId,
    String responseType,
    String assistantMessage,
    RouteDto route,
    List<String> suggestions,
    InvestigationResponseDto investigation) {

  public record RouteDto(String intent, double confidence, String modelId) {}
}
