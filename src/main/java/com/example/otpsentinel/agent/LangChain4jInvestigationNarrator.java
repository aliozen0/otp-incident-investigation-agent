package com.example.otpsentinel.agent;

import com.example.otpsentinel.application.InvestigationNarrator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Narrates on the conversation model, which is picked for prose rather than tool discipline. */
public final class LangChain4jInvestigationNarrator implements InvestigationNarrator {

  private static final Logger LOG = LoggerFactory.getLogger(LangChain4jInvestigationNarrator.class);

  private final Function<String, ChatModel> modelFactory;
  private final String modelId;

  public LangChain4jInvestigationNarrator(Function<String, ChatModel> modelFactory, String modelId) {
    this.modelFactory = Objects.requireNonNull(modelFactory);
    this.modelId = modelId;
  }

  @Override
  public Optional<String> narrate(String findings) {
    if (findings == null || findings.isBlank()) {
      return Optional.empty();
    }
    try {
      InvestigationNarratorAiService service =
          AiServices.builder(InvestigationNarratorAiService.class)
              .chatModel(modelFactory.apply(modelId))
              .build();
      String narration = service.narrate(findings);
      return narration == null || narration.isBlank() ? Optional.empty() : Optional.of(narration.trim());
    } catch (RuntimeException failure) {
      // Never let presentation cost the operator the analysis itself.
      LOG.warn("investigation narration failed, keeping the model summary: {}", failure.toString());
      return Optional.empty();
    }
  }
}
