package com.example.otpsentinel.config;

import com.example.otpsentinel.agent.SequentialToolCallChatModel;
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
import com.example.otpsentinel.operations.JdbcOperationalDataTools;
import com.example.otpsentinel.operations.OperationalDataSeeder;
import com.example.otpsentinel.tools.ErrorDistributionTool;
import com.example.otpsentinel.tools.OtpMetricsTool;
import com.example.otpsentinel.tools.ProviderHealthTool;
import com.example.otpsentinel.tools.QueueHealthTool;
import com.example.otpsentinel.tools.RecentChangesTool;
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
import java.time.Clock;
import java.time.Duration;
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
   * {@code otp-sentinel.rag.min-score} (0.50) is tuned for NVIDIA embeddings: measured against the
   * ingested corpus, an exact title match on nv-embedqa-e5-v5 scores ~0.74 and a good topical match
   * lands in the 0.5-0.7 band, so the original 0.70 floor rejected nearly every real hit. Hash-trick
   * cosine
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
                new SequentialToolCallChatModel(
                    OpenAiChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(id)
                        .parallelToolCalls(false)
                        .logResponses(true)
                        .build()));
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
      @Value("${otp-sentinel.rag.min-score:0.50}") double minScore) {
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

  /**
   * {@code otp-sentinel.operations.source=db} (the default) answers the five tools from the
   * operational tables, so the console's data explorer and the agent read the same rows and an
   * operator can verify any figure the model cites. {@code fixture} keeps the in-code dataset for
   * offline demos and for tests that assert the docs/15 numbers.
   */
  @Bean
  public OperationalDataSeeder operationalDataSeeder(
      JdbcTemplate jdbcTemplate,
      @Value("${otp-sentinel.operations.history-hours:24}") long historyHours,
      // 15 minutes on purpose: the default question asks about "the last 15 minutes", so the
      // previous-period comparison lands on healthy traffic and the drop is actually visible.
      @Value("${otp-sentinel.operations.degraded-lookback-minutes:15}") long degradedMinutes) {
    return new OperationalDataSeeder(
        jdbcTemplate,
        Clock.systemUTC(),
        Duration.ofHours(historyHours),
        Duration.ofMinutes(degradedMinutes));
  }

  @Bean
  public JdbcOperationalDataTools jdbcOperationalDataTools(JdbcTemplate jdbcTemplate) {
    return new JdbcOperationalDataTools(jdbcTemplate, Clock.systemUTC());
  }

  @Bean
  public OtpMetricsTool otpMetricsTool(
      @Value("${otp-sentinel.operations.source:db}") String source,
      JdbcOperationalDataTools databaseTools,
      FixtureScenario demoFixtureScenario) {
    return fromDatabase(source)
        ? databaseTools
        : new FixtureOtpMetricsTool(demoFixtureScenario);
  }

  @Bean
  public ErrorDistributionTool errorDistributionTool(
      @Value("${otp-sentinel.operations.source:db}") String source,
      JdbcOperationalDataTools databaseTools,
      FixtureScenario demoFixtureScenario) {
    return fromDatabase(source)
        ? databaseTools
        : new FixtureErrorDistributionTool(demoFixtureScenario);
  }

  @Bean
  public QueueHealthTool queueHealthTool(
      @Value("${otp-sentinel.operations.source:db}") String source,
      JdbcOperationalDataTools databaseTools,
      FixtureScenario demoFixtureScenario) {
    return fromDatabase(source)
        ? databaseTools
        : new FixtureQueueHealthTool(demoFixtureScenario);
  }

  @Bean
  public ProviderHealthTool providerHealthTool(
      @Value("${otp-sentinel.operations.source:db}") String source,
      JdbcOperationalDataTools databaseTools,
      FixtureScenario demoFixtureScenario) {
    return fromDatabase(source)
        ? databaseTools
        : new FixtureProviderHealthTool(demoFixtureScenario);
  }

  @Bean
  public RecentChangesTool recentChangesTool(
      @Value("${otp-sentinel.operations.source:db}") String source,
      JdbcOperationalDataTools databaseTools,
      FixtureScenario demoFixtureScenario) {
    return fromDatabase(source)
        ? databaseTools
        : new FixtureRecentChangesTool(demoFixtureScenario);
  }

  private static boolean fromDatabase(String source) {
    return !"fixture".equalsIgnoreCase(source);
  }
}
