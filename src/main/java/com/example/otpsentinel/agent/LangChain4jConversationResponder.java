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
  private final String conversationModel;

  /**
   * Prose quality and structured-analysis quality are different jobs. The investigation runs on the
   * model the operator picked, but a small reasoning model writes broken Turkish, so the tool-free
   * reply is produced by a dedicated conversation model. The reply still names the selected catalog
   * model, because that is what the operator's investigations run on.
   */
  private String conversationModelId(String selectedModelId) {
    return conversationModel == null || conversationModel.isBlank() ? selectedModelId : conversationModel;
  }

  public LangChain4jConversationResponder(Function<String, ChatModel> modelFactory) {
    this(modelFactory, null);
  }

  public LangChain4jConversationResponder(
      Function<String, ChatModel> modelFactory, String conversationModel) {
    this.modelFactory = Objects.requireNonNull(modelFactory);
    this.conversationModel = conversationModel;
  }

  @Override
  public ConversationReply respond(
      String message, String sessionContext, String modelId, String locale) {
    ConversationResponseAiService service =
        AiServices.builder(ConversationResponseAiService.class)
            .chatModel(modelFactory.apply(conversationModelId(modelId)))
            .build();
    return service.respond(message, sessionContext, modelId, locale);
  }
}
