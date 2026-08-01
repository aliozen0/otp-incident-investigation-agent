package com.example.otpsentinel.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.agent.stub.StubChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;

class AgentConfigTest {

  private final AgentConfig config = new AgentConfig();

  @Test
  void selectsOfflineStubModelByDefaultMode() {
    assertThat(config.chatModelFactory("stub", "https://example.invalid/v1", "", "").get())
        .isInstanceOf(StubChatModel.class);
  }

  @Test
  void stubSupplierReturnsFreshInstanceOnEachCall() {
    var supplier = config.chatModelFactory("stub", "https://example.invalid/v1", "", "");
    assertThat(supplier.get()).isNotSameAs(supplier.get());
  }

  @Test
  void selectsNvidiaCompatibleOpenAiModelInLiveMode() {
    assertThat(
            config
                .chatModelFactory(
                    "live", "https://integrate.api.nvidia.com/v1", "test-key", "test-model")
                .get())
        .isInstanceOf(OpenAiChatModel.class);
  }
}
