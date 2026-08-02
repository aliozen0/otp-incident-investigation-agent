package com.example.otpsentinel.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * M11 compatibility spike for the second model in {@link com.example.otpsentinel.api.ModelCatalog}
 * (prompts/handoff/M11-prompt.md step 4, docs/19). Same shape as {@link NvidiaNimChatServiceLiveTest}
 * but pins the alternate model id explicitly instead of reading it from env, so both verified models
 * have a permanent regression spike.
 */
@Tag("local-live")
class NvidiaNimAlternateModelLiveTest {

  private static final String MODEL_ID = "meta/llama-3.3-70b-instruct";

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
  void callsToolThroughRealNvidiaNimEndpointOnAlternateModel() {
    String apiKey = System.getenv("NVIDIA_API_KEY");
    assumeTrue(apiKey != null && !apiKey.isBlank(), "NVIDIA_API_KEY not set, skipping live spike");

    String baseUrl =
        System.getenv().getOrDefault("NVIDIA_BASE_URL", "https://integrate.api.nvidia.com/v1");

    OpenAiChatModel chatModel =
        OpenAiChatModel.builder().baseUrl(baseUrl).apiKey(apiKey).modelName(MODEL_ID).build();

    Weather weather =
        AiServices.builder(Weather.class).chatModel(chatModel).tools(new WeatherTool()).build();

    String answer = weather.ask("Ankara");

    assertThat(answer).contains("21");
  }
}
