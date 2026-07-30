package com.example.otpsentinel.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import com.example.otpsentinel.rag.fixtures.KnowledgeFixtureCatalog;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * M4 acceptance test (docs/14-implementation-plan.md "M4 kabul", docs/08 "Evaluation set"): ingests
 * the MVP knowledge fixture set into a real pgvector-enabled Postgres (Testcontainers) and runs the
 * five evaluation-set queries through {@link JdbcKnowledgeSearchAdapter}, using the deterministic
 * hash embedding test double so this runs without a NVIDIA_API_KEY (prompts/handoff/M4-prompt.md
 * "Kısıtlar").
 */
class JdbcKnowledgeRetrievalIntegrationTest extends AbstractPostgresIntegrationTest {

  private static final int DIMENSION = 1024;

  private JdbcKnowledgeSearchAdapter searchAdapter;

  @BeforeEach
  void ingestFixtures() {
    EmbeddingService embeddingService = new DeterministicHashEmbeddingService(DIMENSION);
    KnowledgeIngestionService ingestionService =
        new KnowledgeIngestionService(
            new ContentSanitizer(),
            new Chunker(),
            embeddingService,
            new JdbcKnowledgeRepository(jdbcTemplate));

    KnowledgeFixtureCatalog.mvpDocuments().forEach(ingestionService::ingest);

    searchAdapter = new JdbcKnowledgeSearchAdapter(jdbcTemplate, embeddingService, 5, 0.10);
  }

  @Test
  void providerTimeoutConnectionPoolReturnsIncidentPostmortemInTopFive() {
    List<KnowledgeSearchResult> results =
        searchAdapter.searchIncidentKnowledge("provider timeout connection pool", null, 5);

    assertThat(documentIds(results)).contains("INC-2026-041");
    assertThat(citationFieldsArePresent(results)).isTrue();
  }

  @Test
  void otpDegradationRunbookReturnsRunbookInTopFive() {
    List<KnowledgeSearchResult> results =
        searchAdapter.searchIncidentKnowledge("OTP degradation runbook", null, 5);

    assertThat(documentIds(results)).contains("RB-OTP-001");
  }

  @Test
  void providerTimeoutMeaningReturnsErrorReferenceInTopFive() {
    List<KnowledgeSearchResult> results =
        searchAdapter.searchIncidentKnowledge("PROVIDER_TIMEOUT meaning", null, 5);

    assertThat(documentIds(results)).contains("ERR-OTP-001");
  }

  @Test
  void rollbackApprovalReturnsChangePolicyInTopFive() {
    List<KnowledgeSearchResult> results =
        searchAdapter.searchIncidentKnowledge("rollback approval", null, 5);

    assertThat(documentIds(results)).contains("POL-CHANGE-001");
  }

  @Test
  void marketingCampaignReturnsNoIncidentKnowledgeResult() {
    List<KnowledgeSearchResult> results =
        searchAdapter.searchIncidentKnowledge("marketing campaign", null, 5);

    assertThat(documentIds(results))
        .doesNotContain("INC-2026-041", "RB-OTP-001", "ERR-OTP-001", "POL-CHANGE-001");
  }

  @Test
  void resultsCarryCitationFields() {
    List<KnowledgeSearchResult> results =
        searchAdapter.searchIncidentKnowledge("provider timeout connection pool", null, 5);

    assertThat(results).isNotEmpty();
    assertThat(results.get(0).documentId()).isEqualTo("INC-2026-041");
    assertThat(results.get(0).version()).isEqualTo("1");
    assertThat(results.get(0).title()).isNotBlank();
    assertThat(results.get(0).chunkId()).isNotBlank();
    assertThat(results.get(0).similarityScore()).isBetween(-1.0, 1.0);
  }

  private List<String> documentIds(List<KnowledgeSearchResult> results) {
    return results.stream().map(KnowledgeSearchResult::documentId).collect(Collectors.toList());
  }

  private boolean citationFieldsArePresent(List<KnowledgeSearchResult> results) {
    return results.stream()
        .allMatch(
            r ->
                r.documentId() != null
                    && r.version() != null
                    && r.chunkId() != null
                    && r.title() != null);
  }
}
