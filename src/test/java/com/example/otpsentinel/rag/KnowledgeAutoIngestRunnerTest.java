package com.example.otpsentinel.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import com.example.otpsentinel.rag.fixtures.KnowledgeFixtureCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

/**
 * Auto-ingest runs the MVP knowledge fixture set into a real pgvector-enabled Postgres
 * (Testcontainers), using the deterministic hash embedding test double (no NVIDIA_API_KEY needed —
 * same pattern as {@link JdbcKnowledgeRetrievalIntegrationTest}). Verifies the idempotency contract
 * required by prompts/handoff/M9-prompt.md: running twice never duplicates rows.
 */
class KnowledgeAutoIngestRunnerTest extends AbstractPostgresIntegrationTest {

  private static final int DIMENSION = 1024;

  private KnowledgeAutoIngestRunner newEnabledRunner() {
    JdbcKnowledgeRepository repository = new JdbcKnowledgeRepository(jdbcTemplate);
    KnowledgeIngestionService ingestionService =
        new KnowledgeIngestionService(
            new ContentSanitizer(),
            new Chunker(),
            new DeterministicHashEmbeddingService(DIMENSION),
            repository);
    return new KnowledgeAutoIngestRunner(ingestionService, repository, true);
  }

  @Test
  void firstRunIngestsAllFourMvpDocuments() throws Exception {
    newEnabledRunner().run(new DefaultApplicationArguments());

    Integer count =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM knowledge_document", Integer.class);
    assertThat(count).isEqualTo(KnowledgeFixtureCatalog.mvpDocuments().size());
  }

  @Test
  void secondRunDoesNotDuplicateRows() throws Exception {
    KnowledgeAutoIngestRunner runner = newEnabledRunner();
    runner.run(new DefaultApplicationArguments());
    runner.run(new DefaultApplicationArguments());

    Integer count =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM knowledge_document", Integer.class);
    assertThat(count).isEqualTo(KnowledgeFixtureCatalog.mvpDocuments().size());
  }

  @Test
  void disabledRunnerDoesNothing() throws Exception {
    JdbcKnowledgeRepository repository = new JdbcKnowledgeRepository(jdbcTemplate);
    KnowledgeIngestionService ingestionService =
        new KnowledgeIngestionService(
            new ContentSanitizer(),
            new Chunker(),
            new DeterministicHashEmbeddingService(DIMENSION),
            repository);
    KnowledgeAutoIngestRunner disabled =
        new KnowledgeAutoIngestRunner(ingestionService, repository, false);

    disabled.run(new DefaultApplicationArguments());

    Integer count =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM knowledge_document", Integer.class);
    assertThat(count).isZero();
  }
}
