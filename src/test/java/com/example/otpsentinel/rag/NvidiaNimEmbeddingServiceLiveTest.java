package com.example.otpsentinel.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * M4 compatibility spike (prompts/handoff/M4-prompt.md step 1, docs/19 "Compatibility spike"): a
 * single live NVIDIA NIM embedding call through LangChain4j's {@code OpenAiEmbeddingModel}, plus
 * one pgvector insert/search — proves the ADR-015 approach end to end against the real endpoint.
 *
 * <p>Tagged {@code local-live} and excluded from the default Surefire run (pom.xml excludedGroups)
 * — the main suite never needs {@code NVIDIA_API_KEY} (docs/19 "Kısıtlar"). Run explicitly with
 * {@code -Dgroups=local-live} once {@code NVIDIA_API_KEY} is exported.
 */
@Tag("local-live")
class NvidiaNimEmbeddingServiceLiveTest extends AbstractPostgresIntegrationTest {

  @Test
  void embedsAgainstRealNvidiaNimEndpointAndRoundTripsThroughPgvector() {
    String apiKey = System.getenv("NVIDIA_API_KEY");
    assumeTrue(apiKey != null && !apiKey.isBlank(), "NVIDIA_API_KEY not set, skipping live spike");

    String baseUrl =
        System.getenv().getOrDefault("NVIDIA_BASE_URL", "https://integrate.api.nvidia.com/v1");
    String modelId =
        System.getenv().getOrDefault("NVIDIA_EMBEDDING_MODEL", "nvidia/nv-embedqa-e5-v5");

    EmbeddingService embeddingService =
        new NvidiaNimEmbeddingService(baseUrl, apiKey, modelId, 1024);

    List<Float> passage =
        embeddingService.embed(
            "Gateway connection pool leaked connections after provider timeout.",
            EmbeddingInputType.PASSAGE);
    List<Float> query =
        embeddingService.embed("provider timeout connection pool", EmbeddingInputType.QUERY);

    assertThat(passage).hasSize(1024);
    assertThat(query).hasSize(1024);

    jdbcTemplate.update(
        "INSERT INTO knowledge_document ("
            + "document_id, version, document_type, effective_from, language, title"
            + ") VALUES ('SPIKE-001', '1', 'RUNBOOK', CURRENT_DATE, 'en', 'spike')");
    jdbcTemplate.update(
        "INSERT INTO knowledge_chunk ("
            + "chunk_id, document_id, version, content, token_count, embedding_model, embedding"
            + ") VALUES ('SPIKE-001#v1#c0', 'SPIKE-001', '1', 'spike content', 2, ?, ?::vector)",
        modelId,
        VectorLiterals.toLiteral(passage));

    Double similarity =
        jdbcTemplate.queryForObject(
            "SELECT 1 - (embedding <=> ?::vector) FROM knowledge_chunk WHERE chunk_id = 'SPIKE-001#v1#c0'",
            Double.class,
            VectorLiterals.toLiteral(query));

    assertThat(similarity).isNotNull();
  }
}
