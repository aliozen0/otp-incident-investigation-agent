package com.example.otpsentinel.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * M5 compatibility spike (prompts/handoff/M5-prompt.md step 1, docs/19): a single live NVIDIA NIM
 * tool-calling + structured-output round trip through LangChain4j's {@code OpenAiChatModel},
 * proving ADR-015's approach extends from embeddings to chat.
 *
 * <p>Tagged {@code local-live}, excluded from the default Surefire run (pom.xml excludedGroups) —
 * mirrors {@code NvidiaNimEmbeddingServiceLiveTest}. Run explicitly with {@code
 * -Dsurefire.excludedGroups= -Dtest=NvidiaNimChatServiceLiveTest} once {@code NVIDIA_API_KEY} is
 * exported.
 */
@Tag("local-live")
class NvidiaNimChatServiceLiveTest {

  interface Weather {
    @dev.langchain4j.service.UserMessage("What is the temperature in {{city}}? Use the tool.")
    String ask(@dev.langchain4j.service.V("city") String city);
  }

  static class WeatherTool {
    @Tool("Returns the current temperature in Celsius for a city")
    public int currentTemperature(String city) {
      return 21;
    }
  }

  @Test
  void callsToolThroughRealNvidiaNimEndpoint() {
    String apiKey = System.getenv("NVIDIA_API_KEY");
    assumeTrue(apiKey != null && !apiKey.isBlank(), "NVIDIA_API_KEY not set, skipping live spike");

    String baseUrl =
        System.getenv().getOrDefault("NVIDIA_BASE_URL", "https://integrate.api.nvidia.com/v1");
    String modelId = System.getenv("NVIDIA_CHAT_MODEL");
    assumeTrue(modelId != null && !modelId.isBlank(), "NVIDIA_CHAT_MODEL not set, skipping");

    OpenAiChatModel chatModel =
        OpenAiChatModel.builder().baseUrl(baseUrl).apiKey(apiKey).modelName(modelId).build();

    Weather weather =
        AiServices.builder(Weather.class).chatModel(chatModel).tools(new WeatherTool()).build();

    String answer = weather.ask("Ankara");

    assertThat(answer).contains("21");
  }
}
