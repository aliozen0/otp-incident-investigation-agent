package com.example.otpsentinel.rag;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@link KnowledgeSearchPort} over pgvector cosine similarity (docs/08-rag-spec.md "Retrieval
 * pipeline"/"Retrieval kuralları"): embeds the query as {@link EmbeddingInputType#QUERY}, filters
 * by provider and expiry, orders by distance, then drops anything below {@code minScore}.
 */
public final class JdbcKnowledgeSearchAdapter implements KnowledgeSearchPort {

  private static final String SEARCH =
      """
      SELECT kc.chunk_id, kc.document_id, kc.version, kd.title, kc.section_title, kc.content,
             1 - (kc.embedding <=> ?::vector) AS similarity
      FROM knowledge_chunk kc
      JOIN knowledge_document kd
        ON kd.document_id = kc.document_id AND kd.version = kc.version
      WHERE (kd.effective_to IS NULL OR kd.effective_to >= CURRENT_DATE)
        AND kc.embedding_model = ?
        AND (?::text IS NULL OR kd.provider = ?::text)
      ORDER BY kc.embedding <=> ?::vector
      LIMIT ?
      """;

  private final JdbcTemplate jdbcTemplate;
  private final EmbeddingService embeddingService;
  private final int defaultTopK;
  private final double minScore;

  public JdbcKnowledgeSearchAdapter(
      JdbcTemplate jdbcTemplate,
      EmbeddingService embeddingService,
      int defaultTopK,
      double minScore) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    this.embeddingService =
        Objects.requireNonNull(embeddingService, "embeddingService must not be null");
    if (defaultTopK <= 0) {
      throw new IllegalArgumentException("defaultTopK must be positive");
    }
    this.defaultTopK = defaultTopK;
    this.minScore = minScore;
  }

  @Override
  public List<KnowledgeSearchResult> searchIncidentKnowledge(
      String queryText, String providerFilter, int topK) {
    Objects.requireNonNull(queryText, "queryText must not be null");

    int effectiveTopK = Math.min(Math.min(topK <= 0 ? defaultTopK : topK, defaultTopK), 5);
    String queryVector =
        VectorLiterals.toLiteral(embeddingService.embed(queryText, EmbeddingInputType.QUERY));

    List<KnowledgeSearchResult> results =
        jdbcTemplate.query(
            SEARCH,
            this::mapRow,
            queryVector,
            // Cosine distance between two different embedding families is meaningless, so a
            // hash-embedded corpus and an NVIDIA-embedded corpus are never compared in one query
            // even when both live in knowledge_chunk (DATA-004 / M11 finding 5).
            embeddingService.modelId(),
            providerFilter,
            providerFilter,
            queryVector,
            effectiveTopK);

    return results.stream().filter(r -> r.similarityScore() >= minScore).toList();
  }

  private KnowledgeSearchResult mapRow(ResultSet rs, int rowNum) throws SQLException {
    return new KnowledgeSearchResult(
        rs.getString("document_id"),
        rs.getString("version"),
        rs.getString("title"),
        rs.getString("chunk_id"),
        rs.getString("section_title"),
        rs.getDouble("similarity"),
        rs.getString("content"));
  }
}
