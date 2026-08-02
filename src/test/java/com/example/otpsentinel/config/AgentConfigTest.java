package com.example.otpsentinel.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.agent.stub.StubChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;

class AgentConfigTest {

  private final AgentConfig config = new AgentConfig();

  @Test
  void selectsOfflineStubModelByDefaultMode() {
    assertThat(config.chatModelFactory("stub", "https://example.invalid/v1", "", "").apply(null))
        .isInstanceOf(StubChatModel.class);
  }

  @Test
  void stubFactoryReturnsFreshInstanceOnEachCall() {
    var factory = config.chatModelFactory("stub", "https://example.invalid/v1", "", "");
    assertThat(factory.apply(null)).isNotSameAs(factory.apply(null));
  }

  @Test
  void selectsNvidiaCompatibleOpenAiModelInLiveMode() {
    assertThat(
            config
                .chatModelFactory(
                    "live", "https://integrate.api.nvidia.com/v1", "test-key", "test-model")
                .apply(null))
        .isInstanceOf(OpenAiChatModel.class);
  }

  @Test
  void liveModeCachesTheSameChatModelInstancePerModelId() {
    var factory =
        config.chatModelFactory(
            "live", "https://integrate.api.nvidia.com/v1", "test-key", "test-model");

    assertThat(factory.apply("some/model")).isSameAs(factory.apply("some/model"));
  }

  @Test
  void liveModeFallsBackToDefaultModelIdWhenRequestedIdIsBlank() {
    var factory =
        config.chatModelFactory(
            "live", "https://integrate.api.nvidia.com/v1", "test-key", "test-model");

    assertThat(factory.apply(null)).isSameAs(factory.apply(""));
  }
}
