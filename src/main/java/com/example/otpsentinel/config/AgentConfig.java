package com.example.otpsentinel.config;

import com.example.otpsentinel.agent.stub.OtpDropOneOhOneScript;
import com.example.otpsentinel.agent.stub.StubChatModel;
import com.example.otpsentinel.rag.JdbcKnowledgeSearchAdapter;
import com.example.otpsentinel.rag.KnowledgeSearchPort;
import com.example.otpsentinel.rag.NvidiaNimEmbeddingService;
import com.example.otpsentinel.rag.fixtures.FixtureKnowledgeSearchPort;
import com.example.otpsentinel.tools.fixtures.FixtureCatalog;
import com.example.otpsentinel.tools.fixtures.FixtureErrorDistributionTool;
import com.example.otpsentinel.tools.fixtures.FixtureId;
import com.example.otpsentinel.tools.fixtures.FixtureOtpMetricsTool;
import com.example.otpsentinel.tools.fixtures.FixtureProviderHealthTool;
import com.example.otpsentinel.tools.fixtures.FixtureQueueHealthTool;
import com.example.otpsentinel.tools.fixtures.FixtureRecentChangesTool;
import com.example.otpsentinel.tools.fixtures.FixtureScenario;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Selects the offline deterministic model or the NVIDIA NIM-compatible live model through {@code
 * AI_MODE}. The main test suite and default demo remain network-independent. Fixture tool beans are
 * all built from a single {@link FixtureScenario} (one demo fixture per app instance, per Design
 * decision #5/#6).
 */
@Configuration
public class AgentConfig {

  @Bean
  public Supplier<ChatModel> chatModelFactory(
      @Value("${AI_MODE:stub}") String aiMode,
      @Value("${NVIDIA_BASE_URL:https://integrate.api.nvidia.com/v1}") String baseUrl,
      @Value("${NVIDIA_API_KEY:}") String apiKey,
      @Value("${NVIDIA_CHAT_MODEL:}") String modelId) {
    if ("live".equalsIgnoreCase(aiMode)) {
      // Stateless HTTP client: build once and share across investigations.
      ChatModel live =
          OpenAiChatModel.builder().baseUrl(baseUrl).apiKey(apiKey).modelName(modelId).build();
      return () -> live;
    }
    // StubScript's stepIndex is mutable and monotonic, so each investigation needs its own
    // instance — do NOT collapse this to a cached instance like the live branch above.
    return () -> new StubChatModel(OtpDropOneOhOneScript.build());
  }

  @Bean
  public KnowledgeSearchPort knowledgeSearchPort(
      @Value("${AI_MODE:stub}") String aiMode,
      JdbcTemplate jdbcTemplate,
      @Value("${NVIDIA_BASE_URL:https://integrate.api.nvidia.com/v1}") String baseUrl,
      @Value("${NVIDIA_API_KEY:}") String apiKey,
      @Value("${NVIDIA_EMBEDDING_MODEL:}") String embeddingModel,
      @Value("${otp-sentinel.rag.top-k:5}") int topK,
      @Value("${otp-sentinel.rag.min-score:0.70}") double minScore) {
    if ("live".equalsIgnoreCase(aiMode)) {
      return new JdbcKnowledgeSearchAdapter(
          jdbcTemplate,
          new NvidiaNimEmbeddingService(baseUrl, apiKey, embeddingModel, 1024),
          topK,
          minScore);
    }
    return new FixtureKnowledgeSearchPort();
  }

  @Bean
  public FixtureScenario demoFixtureScenario(
      @Value("${otp-sentinel.demo.fixture:OTP-DROP-001}") String fixtureId) {
    return FixtureCatalog.forFixture(FixtureId.valueOf(fixtureId.replace('-', '_')));
  }

  @Bean
  public FixtureOtpMetricsTool fixtureOtpMetricsTool(FixtureScenario demoFixtureScenario) {
    return new FixtureOtpMetricsTool(demoFixtureScenario);
  }

  @Bean
  public FixtureErrorDistributionTool fixtureErrorDistributionTool(
      FixtureScenario demoFixtureScenario) {
    return new FixtureErrorDistributionTool(demoFixtureScenario);
  }

  @Bean
  public FixtureQueueHealthTool fixtureQueueHealthTool(FixtureScenario demoFixtureScenario) {
    return new FixtureQueueHealthTool(demoFixtureScenario);
  }

  @Bean
  public FixtureProviderHealthTool fixtureProviderHealthTool(FixtureScenario demoFixtureScenario) {
    return new FixtureProviderHealthTool(demoFixtureScenario);
  }

  @Bean
  public FixtureRecentChangesTool fixtureRecentChangesTool(FixtureScenario demoFixtureScenario) {
    return new FixtureRecentChangesTool(demoFixtureScenario);
  }
}
