package com.example.otpsentinel.agent.stub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StubChatModelTest {

  @Test
  void firstStepReturnsToolExecutionRequest() {
    StubScript script =
        new StubScript(
            List.of(
                StubScriptStep.callTools(
                    new StubScriptStep.PlannedToolCall(
                        "getOtpMetrics", Map.of("startAt", "2026-07-30T11:15:00Z"))),
                StubScriptStep.finalAnswer("{\"status\":\"ANOMALY_CONFIRMED\"}")));
    StubChatModel model = new StubChatModel(script);

    ChatRequest request =
        ChatRequest.builder().messages(List.of(UserMessage.from("investigate"))).build();
    ChatResponse response = model.chat(request);

    assertThat(response.aiMessage().hasToolExecutionRequests()).isTrue();
    assertThat(response.aiMessage().toolExecutionRequests().get(0).name())
        .isEqualTo("getOtpMetrics");
  }

  @Test
  void advancesToNextStepOnEachCall() {
    StubScript script =
        new StubScript(
            List.of(
                StubScriptStep.callTools(
                    new StubScriptStep.PlannedToolCall("getQueueHealth", Map.of())),
                StubScriptStep.finalAnswer("{\"status\":\"NO_ANOMALY\"}")));
    StubChatModel model = new StubChatModel(script);
    ChatRequest request =
        ChatRequest.builder().messages(List.of(UserMessage.from("investigate"))).build();

    ChatResponse first = model.chat(request);
    ChatResponse second = model.chat(request);

    assertThat(first.aiMessage().hasToolExecutionRequests()).isTrue();
    assertThat(second.aiMessage().text()).isEqualTo("{\"status\":\"NO_ANOMALY\"}");
  }

  @Test
  void exhaustingTheScriptThrows() {
    StubScript script = new StubScript(List.of(StubScriptStep.finalAnswer("{}")));
    StubChatModel model = new StubChatModel(script);
    ChatRequest request =
        ChatRequest.builder().messages(List.of(UserMessage.from("investigate"))).build();

    model.chat(request);
    assertThatThrownBy(() -> model.chat(request)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void toolArgumentsSerializeToValidJsonWithEscaping() {
    StubScript script =
        new StubScript(
            List.of(
                StubScriptStep.callTools(
                    new StubScriptStep.PlannedToolCall(
                        "getOtpMetrics",
                        Map.of(
                            "note", "has \"quotes\" and \\backslash",
                            "limit", 5,
                            "urgent", true))),
                StubScriptStep.finalAnswer("{}")));
    StubChatModel model = new StubChatModel(script);
    ChatRequest request =
        ChatRequest.builder().messages(List.of(UserMessage.from("investigate"))).build();

    String arguments = model.chat(request).aiMessage().toolExecutionRequests().get(0).arguments();

    // must round-trip through a real JSON parser, same as LangChain4j's tool-argument
    // deserializer would when invoking the real @Tool method.
    com.fasterxml.jackson.databind.ObjectMapper mapper =
        new com.fasterxml.jackson.databind.ObjectMapper();
    assertThat(arguments).isNotBlank();
    var node = org.assertj.core.api.Assertions.catchThrowable(() -> mapper.readTree(arguments));
    assertThat(node).isNull(); // no parse exception thrown
  }
}
