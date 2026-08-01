package com.example.otpsentinel.api.dto;

import java.util.List;

public record IncidentDraftPreviewDto(
    String title,
    String severity,
    String summary,
    int evidenceCount,
    List<String> recommendedChecks,
    boolean requiresExplicitApproval) {}
