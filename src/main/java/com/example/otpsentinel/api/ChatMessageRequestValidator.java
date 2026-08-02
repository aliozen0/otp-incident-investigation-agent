package com.example.otpsentinel.api;

import com.example.otpsentinel.agent.InvestigationMode;
import com.example.otpsentinel.api.dto.ChatMessageRequestDto;
import com.example.otpsentinel.application.ConversationCommand;
import com.example.otpsentinel.application.InteractionMode;
import com.example.otpsentinel.application.PiiScanner;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class ChatMessageRequestValidator {

  private static final Set<String> ALLOWED_LOCALES = Set.of("tr-TR", "en-US");
  private final PiiScanner piiScanner = new PiiScanner();

  public ConversationCommand validate(ChatMessageRequestDto request, String correlationId) {
    if (request == null || request.message() == null) {
      throw invalid("message is required");
    }
    String message = request.message().trim();
    if (message.isEmpty() || message.length() > 2000) {
      throw invalid("message must be between 1 and 2000 characters");
    }
    piiScanner
        .scan(message)
        .ifPresent(
            hit -> {
              throw invalid("message contains prohibited PII");
            });
    String sessionId = requireUuid(request.sessionId());
    if (request.modelId() == null || !ModelCatalog.VERIFIED_MODELS.contains(request.modelId())) {
      throw invalid("modelId is not in the verified allowlist");
    }
    String locale = request.locale() == null ? "tr-TR" : request.locale();
    if (!ALLOWED_LOCALES.contains(locale)) {
      throw invalid("locale is not in the allowlist");
    }
    InteractionMode interactionMode =
        parseEnum(
            request.interactionMode(),
            InteractionMode.AUTO,
            InteractionMode.class,
            "interactionMode");
    InvestigationMode investigationMode =
        parseEnum(
            request.investigationMode(),
            InvestigationMode.THOROUGH,
            InvestigationMode.class,
            "investigationMode");
    return new ConversationCommand(
        message,
        sessionId,
        request.modelId(),
        interactionMode,
        investigationMode,
        request.timeWindow() == null ? null : request.timeWindow().startAt(),
        request.timeWindow() == null ? null : request.timeWindow().endAt(),
        locale,
        correlationId);
  }

  private static String requireUuid(String value) {
    try {
      return UUID.fromString(value).toString();
    } catch (RuntimeException failure) {
      throw invalid("sessionId must be a UUID");
    }
  }

  private static <E extends Enum<E>> E parseEnum(
      String value, E defaultValue, Class<E> type, String field) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException failure) {
      throw invalid(field + " is not allowed");
    }
  }

  private static ApiException invalid(String detail) {
    return new ApiException(400, "INVALID_CHAT_REQUEST", "Invalid chat request", detail);
  }
}
