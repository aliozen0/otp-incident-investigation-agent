package com.example.otpsentinel.application;

import com.example.otpsentinel.domain.Investigation;
import java.util.List;
import java.util.Objects;

/** Two-stage semantic route followed by a tool-free response or the existing investigation port. */
public final class ConversationOrchestrator {

  private final IntentRouter intentRouter;
  private final ConversationResponder responder;
  private final InvestigationExecutor investigationExecutor;
  private final SemanticSessionContextStore contextStore;
  private final int maxRepairAttempts;
  private final IntentDecisionValidator validator = new IntentDecisionValidator();
  private final PiiScanner piiScanner = new PiiScanner();

  public ConversationOrchestrator(
      IntentRouter intentRouter,
      ConversationResponder responder,
      InvestigationExecutor investigationExecutor,
      SemanticSessionContextStore contextStore,
      int maxRepairAttempts) {
    this.intentRouter = Objects.requireNonNull(intentRouter);
    this.responder = Objects.requireNonNull(responder);
    this.investigationExecutor = Objects.requireNonNull(investigationExecutor);
    this.contextStore = Objects.requireNonNull(contextStore);
    if (maxRepairAttempts < 0 || maxRepairAttempts > 1) {
      throw new IllegalArgumentException("maxRepairAttempts must be 0 or 1");
    }
    this.maxRepairAttempts = maxRepairAttempts;
  }

  public ConversationResult handle(ConversationCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    String context = contextStore.context(command.sessionId());
    IntentDecision route = resolveRoute(command, context);
    return switch (route.intent()) {
      case CHAT -> chat(command, context, route);
      case CLARIFICATION -> clarification(command, route);
      case INVESTIGATION -> investigate(command, route);
    };
  }

  private IntentDecision resolveRoute(ConversationCommand command, String context) {
    if (command.interactionMode() == InteractionMode.CHAT) {
      return new IntentDecision(IntentType.CHAT, 1.0, command.message(), null);
    }
    if (command.interactionMode() == InteractionMode.INVESTIGATION) {
      return new IntentDecision(IntentType.INVESTIGATION, 1.0, command.message(), null);
    }
    RuntimeException lastFailure = null;
    for (int attempt = 0; attempt <= maxRepairAttempts; attempt++) {
      try {
        return validator.validate(
            intentRouter.route(command.message(), context, command.modelId()));
      } catch (RuntimeException failure) {
        lastFailure = failure;
      }
    }
    throw new IntentRoutingFailedException(
        "intent routing failed after one repair attempt", lastFailure);
  }

  private ConversationResult chat(
      ConversationCommand command, String context, IntentDecision route) {
    ConversationReply reply =
        responder.respond(command.message(), context, command.modelId(), command.locale());
    String message = validateAssistantMessage(reply == null ? null : reply.message());
    List<String> suggestions =
        validator.validateSuggestions(reply == null ? List.of() : reply.suggestions());
    contextStore.append(command.sessionId(), command.message(), message);
    return new ConversationResult(IntentType.CHAT, message, route, suggestions, null);
  }

  private ConversationResult clarification(ConversationCommand command, IntentDecision route) {
    String message = validateAssistantMessage(route.clarificationQuestion());
    contextStore.append(command.sessionId(), command.message(), message);
    return new ConversationResult(IntentType.CLARIFICATION, message, route, List.of(), null);
  }

  private ConversationResult investigate(ConversationCommand command, IntentDecision route) {
    Investigation investigation = investigationExecutor.execute(command);
    String message =
        investigation.summary() == null || investigation.summary().isBlank()
            ? "OTP incelemesi başlatıldı."
            : investigation.summary();
    message = validateAssistantMessage(message);
    contextStore.append(command.sessionId(), command.message(), message);
    return new ConversationResult(
        IntentType.INVESTIGATION, message, route, List.of(), investigation);
  }

  private String validateAssistantMessage(String value) {
    if (value == null || value.isBlank() || value.length() > 4000) {
      throw new IllegalArgumentException("assistant message length is invalid");
    }
    String lower = value.toLowerCase(java.util.Locale.ROOT);
    if (value.indexOf('<') >= 0 || value.indexOf('>') >= 0 || lower.contains("javascript:")) {
      throw new IllegalArgumentException("assistant message must be safe plain text");
    }
    piiScanner
        .scan(value)
        .ifPresent(
            hit -> {
              throw new IllegalArgumentException("PII detected in assistant message");
            });
    return value.trim();
  }
}
