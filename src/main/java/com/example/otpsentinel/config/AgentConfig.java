package com.example.otpsentinel.config;

import com.example.otpsentinel.agent.stub.OtpDropOneOhOneScript;
import com.example.otpsentinel.agent.stub.StubChatModel;
import com.example.otpsentinel.rag.Chunker;
import com.example.otpsentinel.rag.ContentSanitizer;
import com.example.otpsentinel.rag.EmbeddingService;
import com.example.otpsentinel.rag.HashEmbeddingService;
import com.example.otpsentinel.rag.JdbcKnowledgeRepository;
import com.example.otpsentinel.rag.JdbcKnowledgeSearchAdapter;
import com.example.otpsentinel.rag.KnowledgeAutoIngestRunner;
import com.example.otpsentinel.rag.KnowledgeIngestionService;
import com.example.otpsentinel.rag.KnowledgeRepository;
import com.example.otpsentinel.rag.KnowledgeSearchPort;
import com.example.otpsentinel.rag.NvidiaNimEmbeddingService;
import com.example.otpsentinel.rag.fixtures.CompositeKnowledgeSearchPort;
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

  /**
   * {@code otp-sentinel.rag.min-score} (0.70) is tuned for NVIDIA embeddings. Hash-trick cosine
   * scores track raw vocabulary overlap and sit far lower, so the non-live adapter uses the same
   * threshold the M4 hash-embedding retrieval tests use.
   */
  private static final double HASH_EMBEDDING_MIN_SCORE = 0.10;

  @Bean
  public java.util.function.Function<String, ChatModel> chatModelFactory(
      @Value("${AI_MODE:stub}") String aiMode,
      @Value("${NVIDIA_BASE_URL:https://integrate.api.nvidia.com/v1}") String baseUrl,
      @Value("${NVIDIA_API_KEY:}") String apiKey,
      @Value("${NVIDIA_CHAT_MODEL:}") String defaultModelId) {
    if ("live".equalsIgnoreCase(aiMode)) {
      java.util.concurrent.ConcurrentMap<String, ChatModel> cache =
          new java.util.concurrent.ConcurrentHashMap<>();
      return requestedModelId -> {
        String modelId =
            (requestedModelId == null || requestedModelId.isBlank())
                ? defaultModelId
                : requestedModelId;
        // Lazily cached per model id: the first request for a given model builds one stateless
        // HTTP client and shares it across subsequent investigations, same rationale as before
        // (logResponses, never logRequests — NVIDIA_API_KEY never gets logged).
        return cache.computeIfAbsent(
            modelId,
            id ->
                OpenAiChatModel.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .modelName(id)
                    .parallelToolCalls(false)
                    .logResponses(true)
                    .build());
      };
    }
    // StubScript's stepIndex is mutable and monotonic, so each investigation needs its own
    // instance — do NOT collapse this to a cached instance like the live branch above.
    return requestedModelId -> new StubChatModel(OtpDropOneOhOneScript.build());
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
    // Non-live: keep the deterministic demo citation AND surface anything actually ingested with
    // the hash embedding (uploads via POST /api/v1/knowledge/documents) — M11 finding 4.
    return new CompositeKnowledgeSearchPort(
        new FixtureKnowledgeSearchPort(),
        new JdbcKnowledgeSearchAdapter(
            jdbcTemplate, new HashEmbeddingService(1024), topK, HASH_EMBEDDING_MIN_SCORE));
  }

  @Bean
  public KnowledgeRepository knowledgeRepository(JdbcTemplate jdbcTemplate) {
    return new JdbcKnowledgeRepository(jdbcTemplate);
  }

  @Bean
  public KnowledgeIngestionService knowledgeIngestionService(
      @Value("${AI_MODE:stub}") String aiMode,
      KnowledgeRepository knowledgeRepository,
      @Value("${NVIDIA_BASE_URL:https://integrate.api.nvidia.com/v1}") String baseUrl,
      @Value("${NVIDIA_API_KEY:}") String apiKey,
      @Value("${NVIDIA_EMBEDDING_MODEL:}") String embeddingModel) {
    EmbeddingService embeddingService =
        "live".equalsIgnoreCase(aiMode)
            ? new NvidiaNimEmbeddingService(baseUrl, apiKey, embeddingModel, 1024)
            : new HashEmbeddingService(1024);
    return new KnowledgeIngestionService(
        new ContentSanitizer(), new Chunker(), embeddingService, knowledgeRepository);
  }

  @Bean
  public KnowledgeAutoIngestRunner knowledgeAutoIngestRunner(
      @Value("${AI_MODE:stub}") String aiMode,
      KnowledgeRepository knowledgeRepository,
      KnowledgeIngestionService knowledgeIngestionService) {
    boolean live = "live".equalsIgnoreCase(aiMode);
    return new KnowledgeAutoIngestRunner(knowledgeIngestionService, knowledgeRepository, live);
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
