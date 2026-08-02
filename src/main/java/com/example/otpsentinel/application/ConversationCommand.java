package com.example.otpsentinel.application;

import com.example.otpsentinel.agent.InvestigationMode;
import java.time.Instant;

public record ConversationCommand(
    String message,
    String sessionId,
    String modelId,
    InteractionMode interactionMode,
    InvestigationMode investigationMode,
    Instant explicitStartAt,
    Instant explicitEndAt,
    String locale,
    String correlationId) {}
