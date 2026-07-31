package com.example.otpsentinel.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.agent.stub.StubChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;

class AgentConfigTest {

  private final AgentConfig config = new AgentConfig();

  @Test
  void selectsOfflineStubModelByDefaultMode() {
    assertThat(config.chatModel("stub", "https://example.invalid/v1", "", ""))
        .isInstanceOf(StubChatModel.class);
  }

  @Test
  void selectsNvidiaCompatibleOpenAiModelInLiveMode() {
    assertThat(
            config.chatModel(
                "live", "https://integrate.api.nvidia.com/v1", "test-key", "test-model"))
        .isInstanceOf(OpenAiChatModel.class);
  }
}
