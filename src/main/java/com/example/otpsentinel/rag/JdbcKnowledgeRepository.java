package com.example.otpsentinel.rag;

import java.sql.Array;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
        language, tags, title, sanitized_content
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
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
    save(
        document,
        chunks.stream()
            .map(embedded -> embedded.chunk().content())
            .collect(Collectors.joining("\n\n")),
        chunks);
  }

  @Override
  public void save(
      KnowledgeDocument document, String sanitizedContent, List<EmbeddedChunk> chunks) {
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
        document.title(),
        sanitizedContent);

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

  @Override
  public List<KnowledgeDocumentSummary> listDocuments() {
    return jdbcTemplate.query(
        """
        SELECT kd.*,
               ARRAY(SELECT jsonb_array_elements_text(kd.tags)) AS tag_values,
               (SELECT COUNT(*) FROM knowledge_chunk kc
                 WHERE kc.document_id = kd.document_id AND kc.version = kd.version) AS chunk_count,
               (SELECT MIN(kc.embedding_model) FROM knowledge_chunk kc
                 WHERE kc.document_id = kd.document_id AND kc.version = kd.version) AS embedding_model
        FROM knowledge_document kd
        ORDER BY kd.created_at DESC
        """,
        (rs, rowNum) -> summary(rs));
  }

  @Override
  public Optional<KnowledgeDocumentDetail> findDocument(String documentId, String version) {
    List<KnowledgeDocumentDetail> documents =
        jdbcTemplate.query(
            """
            SELECT kd.*, ARRAY(SELECT jsonb_array_elements_text(kd.tags)) AS tag_values
            FROM knowledge_document kd
            WHERE kd.document_id = ? AND kd.version = ?
            """,
            (rs, rowNum) ->
                new KnowledgeDocumentDetail(
                    rs.getString("document_id"),
                    rs.getString("version"),
                    rs.getString("title"),
                    KnowledgeDocumentType.valueOf(rs.getString("document_type")),
                    rs.getString("provider"),
                    tags(rs),
                    rs.getDate("effective_from").toLocalDate(),
                    rs.getDate("effective_to") == null
                        ? null
                        : rs.getDate("effective_to").toLocalDate(),
                    rs.getString("language"),
                    rs.getTimestamp("created_at").toInstant(),
                    Objects.requireNonNullElse(rs.getString("sanitized_content"), ""),
                    chunks(documentId, version)),
            documentId,
            version);
    return documents.stream().findFirst();
  }

  private List<KnowledgeDocumentDetail.ChunkDetail> chunks(String documentId, String version) {
    return jdbcTemplate.query(
        """
        SELECT chunk_id, section_title, content, token_count, embedding_model
        FROM knowledge_chunk
        WHERE document_id = ? AND version = ?
        ORDER BY chunk_id
        """,
        (rs, rowNum) ->
            new KnowledgeDocumentDetail.ChunkDetail(
                rs.getString("chunk_id"),
                rs.getString("section_title"),
                rs.getString("content"),
                rs.getInt("token_count"),
                rs.getString("embedding_model")),
        documentId,
        version);
  }

  private KnowledgeDocumentSummary summary(ResultSet rs) throws SQLException {
    return new KnowledgeDocumentSummary(
        rs.getString("document_id"),
        rs.getString("version"),
        rs.getString("title"),
        KnowledgeDocumentType.valueOf(rs.getString("document_type")),
        rs.getString("provider"),
        tags(rs),
        rs.getDate("effective_from").toLocalDate(),
        rs.getDate("effective_to") == null ? null : rs.getDate("effective_to").toLocalDate(),
        rs.getString("language"),
        rs.getInt("chunk_count"),
        rs.getString("embedding_model"),
        rs.getTimestamp("created_at").toInstant());
  }

  private List<String> tags(ResultSet rs) throws SQLException {
    Array array = rs.getArray("tag_values");
    if (array == null) {
      return List.of();
    }
    return Arrays.stream((Object[]) array.getArray()).map(Object::toString).toList();
  }

  /** Tags are plain strings only, so a hand-rolled JSON array avoids pulling in Jackson here. */
  private String toJsonArray(List<String> tags) {
    return tags.stream()
        .map(tag -> "\"" + tag.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
        .collect(Collectors.joining(",", "[", "]"));
  }
}
