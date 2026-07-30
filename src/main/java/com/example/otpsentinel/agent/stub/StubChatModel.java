package com.example.otpsentinel.agent.stub;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Deterministic fake {@link ChatModel} driven by a fixed {@link StubScript} (docs/13 "Deterministic
 * stub model yaklaşımı", ADR-011). Ignores the actual {@link ChatRequest} content — ordering is
 * fixed by the script, not by interpreting prior tool results, which is enough to exercise every
 * fixture tool + RAG + the domain mapping end to end without a real LLM call.
 */
public final class StubChatModel implements ChatModel {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final StubScript script;
  private int stepIndex = 0;

  public StubChatModel(StubScript script) {
    this.script = Objects.requireNonNull(script, "script must not be null");
  }

  @Override
  public ChatResponse chat(ChatRequest chatRequest) {
    if (stepIndex >= script.steps().size()) {
      throw new IllegalStateException("StubScript exhausted after " + stepIndex + " steps");
    }
    StubScriptStep step = script.steps().get(stepIndex++);
    if (step.isFinal()) {
      return ChatResponse.builder().aiMessage(AiMessage.from(step.finalAnswerJson())).build();
    }
    List<ToolExecutionRequest> requests = new ArrayList<>();
    for (StubScriptStep.PlannedToolCall call : step.toolCalls()) {
      requests.add(
          ToolExecutionRequest.builder()
              .id(UUID.randomUUID().toString())
              .name(call.toolName())
              .arguments(toJsonArguments(call.arguments()))
              .build());
    }
    return ChatResponse.builder().aiMessage(AiMessage.from(requests)).build();
  }

  private static String toJsonArguments(java.util.Map<String, Object> arguments) {
    // Real, well-formed JSON (not hand-rolled) so LangChain4j's tool-argument deserializer parses
    // it exactly as it would a real model's function-call arguments — Jackson is already on the
    // classpath via spring-boot-starter-web.
    try {
      return JSON.writeValueAsString(arguments);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException("failed to serialize stub tool arguments: " + arguments, e);
    }
  }
}
