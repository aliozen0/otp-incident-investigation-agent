package com.example.otpsentinel.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Live admission evidence for models that can complete a sequential two-tool turn and return typed
 * structured output. A candidate must pass this gate before it can enter ModelCatalog.
 */
@Tag("local-live")
class NvidiaNimSequentialMultiToolCandidateLiveTest {

  record WeatherAnswer(int temperatureCelsius, int humidityPercent) {}

  interface Weather {
    @dev.langchain4j.service.UserMessage(
        "Ankara için önce sıcaklık, sonra nem aracını çağır. Araçları aynı anda çağırma. "
            + "İki sonucu da structured cevapta döndür.")
    WeatherAnswer ask();
  }

  static class WeatherTools {
    @Tool("Bir şehir için güncel sıcaklığı Celsius olarak döndürür")
    public int currentTemperature(String city) {
      return 21;
    }

    @Tool("Bir şehir için güncel bağıl nem yüzdesini döndürür")
    public int currentHumidity(String city) {
      return 40;
    }
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {
        "meta/llama-3.1-8b-instruct",
        "nvidia/nemotron-3-super-120b-a12b",
        "nvidia/nemotron-3-ultra-550b-a55b"
      })
  void completesSequentialMultiToolStructuredRoundTrip(String modelId) {
    String apiKey = System.getenv("NVIDIA_API_KEY");
    assumeTrue(apiKey != null && !apiKey.isBlank(), "NVIDIA_API_KEY not set, skipping live spike");
    String baseUrl =
        System.getenv().getOrDefault("NVIDIA_BASE_URL", "https://integrate.api.nvidia.com/v1");

    ChatModel model =
        new SequentialToolCallChatModel(
            OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelId)
                .parallelToolCalls(false)
                .build());
    Weather weather =
        AiServices.builder(Weather.class).chatModel(model).tools(new WeatherTools()).build();

    WeatherAnswer answer = weather.ask();

    assertThat(answer.temperatureCelsius()).isEqualTo(21);
    assertThat(answer.humidityPercent()).isEqualTo(40);
  }
}
