package com.example.otpsentinel.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.otpsentinel.agent.InvestigationMode;
import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.domain.TimeWindow;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ConversationOrchestratorTest {

  @Test
  void autoChatUsesToolFreeResponderAndNeverRunsInvestigation() {
    AtomicInteger investigations = new AtomicInteger();
    ConversationOrchestrator orchestrator =
        orchestrator(
            (message, context, model) ->
                new IntentDecision(IntentType.CHAT, 0.95, "greeting", null),
            (message, context, model, locale) ->
                new ConversationReply("Merhaba, OTP operasyonlarında yardımcı olurum.", List.of()),
            investigations);

    ConversationResult result = orchestrator.handle(command(InteractionMode.AUTO));

    assertThat(result.responseType()).isEqualTo(IntentType.CHAT);
    assertThat(result.investigation()).isNull();
    assertThat(investigations).hasValue(0);
  }

  @Test
  void clarificationDoesNotCallResponderOrInvestigation() {
    AtomicInteger investigations = new AtomicInteger();
    AtomicInteger responders = new AtomicInteger();
    ConversationOrchestrator orchestrator =
        orchestrator(
            (message, context, model) ->
                new IntentDecision(
                    IntentType.CLARIFICATION,
                    0.61,
                    "operator health",
                    "Hangi zaman aralığını ve metriği inceleyeyim?"),
            (message, context, model, locale) -> {
              responders.incrementAndGet();
              return new ConversationReply("unused", List.of());
            },
            investigations);

    ConversationResult result = orchestrator.handle(command(InteractionMode.AUTO));

    assertThat(result.responseType()).isEqualTo(IntentType.CLARIFICATION);
    assertThat(result.assistantMessage()).contains("Hangi zaman");
    assertThat(responders).hasValue(0);
    assertThat(investigations).hasValue(0);
  }

  @Test
  void explicitModesBypassRouterAndInvestigationDelegatesToExistingPipelinePort() {
    AtomicInteger routes = new AtomicInteger();
    AtomicInteger investigations = new AtomicInteger();
    ConversationOrchestrator orchestrator =
        orchestrator(
            (message, context, model) -> {
              routes.incrementAndGet();
              throw new AssertionError("router must be bypassed");
            },
            (message, context, model, locale) ->
                new ConversationReply("Toolsuz sohbet", List.of()),
            investigations);

    ConversationResult chat = orchestrator.handle(command(InteractionMode.CHAT));
    ConversationResult investigation =
        orchestrator.handle(command(InteractionMode.INVESTIGATION));

    assertThat(chat.responseType()).isEqualTo(IntentType.CHAT);
    assertThat(investigation.responseType()).isEqualTo(IntentType.INVESTIGATION);
    assertThat(investigation.investigation()).isNotNull();
    assertThat(routes).hasValue(0);
    assertThat(investigations).hasValue(1);
  }

  @Test
  void malformedAutoRouteIsRetriedOnceThenFailsClosed() {
    AtomicInteger attempts = new AtomicInteger();
    ConversationOrchestrator orchestrator =
        orchestrator(
            (message, context, model) -> {
              attempts.incrementAndGet();
              return new IntentDecision(null, 5, "", "bad");
            },
            (message, context, model, locale) -> new ConversationReply("unused", List.of()),
            new AtomicInteger());

    assertThatThrownBy(() -> orchestrator.handle(command(InteractionMode.AUTO)))
        .isInstanceOf(IntentRoutingFailedException.class);
    assertThat(attempts).hasValue(2);
  }

  private static ConversationOrchestrator orchestrator(
      IntentRouter router, ConversationResponder responder, AtomicInteger investigations) {
    InvestigationExecutor executor =
        command -> {
          investigations.incrementAndGet();
          TimeWindow window =
              new TimeWindow(
                  Instant.parse("2026-08-02T18:00:00Z"),
                  Instant.parse("2026-08-02T18:15:00Z"));
          return Investigation.receive(
              command.message(), window, "v1", "v1", command.sessionId());
        };
    return new ConversationOrchestrator(
        router, responder, executor, new SemanticSessionContextStore(4, 10), 1);
  }

  private static ConversationCommand command(InteractionMode mode) {
    return new ConversationCommand(
        "Merhaba",
        "8f663bc4-7a1b-47af-aeb7-6b86fb189cc0",
        "meta/llama-3.1-8b-instruct",
        mode,
        InvestigationMode.THOROUGH,
        null,
        null,
        "tr-TR",
        "corr-1");
  }
}
