package com.example.otpsentinel.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProblemDetailsDto(
    @Schema(example = "https://errors.example.local/investigation-timeout") String type,
    @Schema(example = "Investigation timed out") String title,
    @Schema(example = "504") int status,
    @Schema(example = "The investigation exceeded the configured deadline.") String detail,
    @Schema(example = "/api/v1/investigations") String instance,
    @Schema(example = "corr-ec3c") String correlationId,
    @Schema(example = "INVESTIGATION_TIMEOUT") String errorCode) {}
