package com.example.otpsentinel.agent;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Narrows one assistant turn to one tool call so single-call NVIDIA chat templates never receive a
 * history message containing parallel tool calls. Remaining tools can be selected on later model
 * turns after the first result is visible.
 */
public final class SequentialToolCallChatModel implements ChatModel {

  private final ChatModel delegate;

  public SequentialToolCallChatModel(ChatModel delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
  }

  @Override
  public ChatResponse chat(ChatRequest request) {
    ChatResponse response = delegate.chat(request);
    AiMessage message = response.aiMessage();
    if (message == null || message.toolExecutionRequests().size() <= 1) {
      return response;
    }
    AiMessage sequentialMessage =
        message.toBuilder()
            .toolExecutionRequests(List.of(message.toolExecutionRequests().getFirst()))
            .build();
    return response.toBuilder().aiMessage(sequentialMessage).build();
  }

  @Override
  public ChatRequestParameters defaultRequestParameters() {
    return delegate.defaultRequestParameters();
  }

  @Override
  public List<ChatModelListener> listeners() {
    return delegate.listeners();
  }

  @Override
  public ModelProvider provider() {
    return delegate.provider();
  }

  @Override
  public Set<Capability> supportedCapabilities() {
    return delegate.supportedCapabilities();
  }
}
