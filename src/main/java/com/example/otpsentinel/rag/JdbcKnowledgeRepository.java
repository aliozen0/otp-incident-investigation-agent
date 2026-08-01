package com.example.otpsentinel.rag;

import java.sql.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@link KnowledgeRepository} on plain JdbcTemplate, pgvector column via a {@code ::vector} text
 * cast.
 */
public final class JdbcKnowledgeRepository implements KnowledgeRepository {

  private static final String INSERT_DOCUMENT =
      """
      INSERT INTO knowledge_document (
        document_id, version, document_type, provider, effective_from, effective_to,
        language, tags, title
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
      """;

  private static final String INSERT_CHUNK =
      """
      INSERT INTO knowledge_chunk (
        chunk_id, document_id, version, section_title, content, token_count,
        embedding_model, embedding
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::vector)
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcKnowledgeRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
  }

  @Override
  public void save(KnowledgeDocument document, List<EmbeddedChunk> chunks) {
    jdbcTemplate.update(
        INSERT_DOCUMENT,
        document.documentId(),
        document.version(),
        document.documentType().name(),
        document.provider(),
        Date.valueOf(document.effectiveFrom()),
        document.effectiveTo() == null ? null : Date.valueOf(document.effectiveTo()),
        document.language(),
        toJsonArray(document.tags()),
        document.title());

    for (EmbeddedChunk embedded : chunks) {
      DocumentChunk chunk = embedded.chunk();
      jdbcTemplate.update(
          INSERT_CHUNK,
          chunk.chunkId(),
          chunk.documentId(),
          chunk.version(),
          chunk.sectionTitle(),
          chunk.content(),
          chunk.tokenCount(),
          embedded.embeddingModel(),
          VectorLiterals.toLiteral(embedded.embedding()));
    }
  }

  @Override
  public boolean existsDocument(String documentId, String version) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM knowledge_document WHERE document_id = ? AND version = ?",
            Integer.class,
            documentId,
            version);
    return count != null && count > 0;
  }

  /** Tags are plain strings only, so a hand-rolled JSON array avoids pulling in Jackson here. */
  private String toJsonArray(List<String> tags) {
    return tags.stream()
        .map(tag -> "\"" + tag.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
        .collect(Collectors.joining(",", "[", "]"));
  }
}
