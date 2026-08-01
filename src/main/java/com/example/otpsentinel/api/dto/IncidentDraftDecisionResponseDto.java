package com.example.otpsentinel.api.dto;

public record IncidentDraftDecisionResponseDto(
    String incidentDraftId, String externalIncidentId, String status, boolean idempotentReplay) {}
