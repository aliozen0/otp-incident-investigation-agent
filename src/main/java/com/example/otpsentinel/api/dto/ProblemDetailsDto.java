package com.example.otpsentinel.api.dto;

public record ProblemDetailsDto(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    String correlationId,
    String errorCode) {}
