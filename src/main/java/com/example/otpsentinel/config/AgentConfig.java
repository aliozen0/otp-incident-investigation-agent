package com.example.otpsentinel.config;

import com.example.otpsentinel.agent.stub.StubChatModel;
import com.example.otpsentinel.agent.stub.StubScript;
import com.example.otpsentinel.agent.stub.StubScriptStep;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the offline deterministic model or the NVIDIA NIM-compatible live model through {@code
 * AI_MODE}. The main test suite and default demo remain network-independent.
 */
@Configuration
public class AgentConfig {

  @Bean
  public ChatModel chatModel(
      @Value("${AI_MODE:stub}") String aiMode,
      @Value("${NVIDIA_BASE_URL:https://integrate.api.nvidia.com/v1}") String baseUrl,
      @Value("${NVIDIA_API_KEY:}") String apiKey,
      @Value("${NVIDIA_CHAT_MODEL:}") String modelId) {
    if ("live".equalsIgnoreCase(aiMode)) {
      return OpenAiChatModel.builder().baseUrl(baseUrl).apiKey(apiKey).modelName(modelId).build();
    }
    return new StubChatModel(defaultStubScript());
  }

  private static StubScript defaultStubScript() {
    return new StubScript(
        List.of(
            StubScriptStep.finalAnswer(
                """
                {"status":"INSUFFICIENT_DATA","severity":"LOW","summary":"No scenario is wired.",
                 "evidence":[],"hypotheses":[],"recommendedActions":[],
                 "knowledgeReferences":[],"confidence":0.0}
                """)));
  }
}
