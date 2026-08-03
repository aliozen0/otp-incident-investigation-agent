package com.example.otpsentinel.agent;

import com.example.otpsentinel.application.IntentDecision;
import com.example.otpsentinel.application.IntentRouter;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import java.util.Objects;
import java.util.function.Function;

/** Live semantic routing adapter. No tools are registered on this AiServices instance. */
public final class LangChain4jIntentRouter implements IntentRouter {

  private final Function<String, ChatModel> modelFactory;

  public LangChain4jIntentRouter(Function<String, ChatModel> modelFactory) {
    this.modelFactory = Objects.requireNonNull(modelFactory);
  }

  @Override
  public IntentDecision route(String message, String sessionContext, String modelId) {
    IntentRoutingAiService service =
        AiServices.builder(IntentRoutingAiService.class)
            .chatModel(modelFactory.apply(modelId))
            .build();
    return service.route(message, sessionContext, modelId);
  }
}
