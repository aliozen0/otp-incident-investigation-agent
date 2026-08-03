package com.example.otpsentinel.agent;

import com.example.otpsentinel.application.ConversationReply;
import com.example.otpsentinel.application.ConversationResponder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import java.util.Objects;
import java.util.function.Function;

/** Tool-free normal conversation adapter using the same verified selected-model factory. */
public final class LangChain4jConversationResponder implements ConversationResponder {

  private final Function<String, ChatModel> modelFactory;

  public LangChain4jConversationResponder(Function<String, ChatModel> modelFactory) {
    this.modelFactory = Objects.requireNonNull(modelFactory);
  }

  @Override
  public ConversationReply respond(
      String message, String sessionContext, String modelId, String locale) {
    ConversationResponseAiService service =
        AiServices.builder(ConversationResponseAiService.class)
            .chatModel(modelFactory.apply(modelId))
            .build();
    return service.respond(message, sessionContext, modelId, locale);
  }
}
