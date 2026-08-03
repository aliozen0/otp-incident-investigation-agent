package com.example.otpsentinel.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * M12.1 admission gate: candidates stay out of ModelCatalog until this real tool round-trip passes.
 */
@Tag("local-live")
class NvidiaNimCandidateModelsLiveTest {

  interface Weather {
    @dev.langchain4j.service.UserMessage(
        "Ankara sıcaklığını öğrenmek için aracı kullan ve sonucu yaz.")
    String ask();
  }

  static class WeatherTool {
    @Tool("Bir şehir için güncel sıcaklığı Celsius olarak döndürür")
    public int currentTemperature(String city) {
      return 21;
    }
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {"nvidia/llama-3.3-nemotron-super-49b-v1.5", "nvidia/nemotron-3-nano-30b-a3b"})
  void callsToolThroughRealNvidiaEndpoint(String modelId) {
    String apiKey = System.getenv("NVIDIA_API_KEY");
    assumeTrue(apiKey != null && !apiKey.isBlank(), "NVIDIA_API_KEY not set, skipping live spike");
    String baseUrl =
        System.getenv().getOrDefault("NVIDIA_BASE_URL", "https://integrate.api.nvidia.com/v1");

    OpenAiChatModel model =
        OpenAiChatModel.builder().baseUrl(baseUrl).apiKey(apiKey).modelName(modelId).build();
    Weather weather =
        AiServices.builder(Weather.class).chatModel(model).tools(new WeatherTool()).build();

    assertThat(weather.ask()).contains("21");
  }
}
