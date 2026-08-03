package com.example.otpsentinel.agent;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class SequentialToolCallChatModelTest {

  @Test
  void keepsOnlyFirstToolCallFromEachAssistantTurn() {
    ToolExecutionRequest first = toolCall("call-1", "getOtpMetrics");
    ToolExecutionRequest second = toolCall("call-2", "getQueueHealth");
    ChatResponse original =
        ChatResponse.builder()
            .id("response-1")
            .modelName("test-model")
            .aiMessage(AiMessage.from("", List.of(first, second)))
            .build();
    ChatModel delegate =
        new ChatModel() {
          @Override
          public ChatResponse chat(ChatRequest request) {
            return original;
          }
        };

    ChatResponse narrowed =
        new SequentialToolCallChatModel(delegate)
            .chat(ChatRequest.builder().messages(UserMessage.from("investigate")).build());

    assertThat(narrowed.aiMessage().toolExecutionRequests()).containsExactly(first);
    assertThat(narrowed.id()).isEqualTo("response-1");
    assertThat(narrowed.modelName()).isEqualTo("test-model");
  }

  @Test
  void leavesNormalAssistantResponseUntouched() {
    ChatResponse original =
        ChatResponse.builder().aiMessage(AiMessage.from("final structured answer")).build();

    ChatResponse result =
        new SequentialToolCallChatModel(
                new ChatModel() {
                  @Override
                  public ChatResponse chat(ChatRequest request) {
                    return original;
                  }
                })
            .chat(ChatRequest.builder().messages(UserMessage.from("hello")).build());

    assertThat(result).isSameAs(original);
  }

  private static ToolExecutionRequest toolCall(String id, String name) {
    return ToolExecutionRequest.builder().id(id).name(name).arguments("{}").build();
  }
}
