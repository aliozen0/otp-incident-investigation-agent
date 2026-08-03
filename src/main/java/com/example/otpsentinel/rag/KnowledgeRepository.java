package com.example.otpsentinel.rag;

import java.util.List;
import java.util.Optional;

/** Port: persists an ingested {@link KnowledgeDocument} and its embedded chunks. */
public interface KnowledgeRepository {

  void save(KnowledgeDocument document, List<EmbeddedChunk> chunks);

  default void save(
      KnowledgeDocument document, String sanitizedContent, List<EmbeddedChunk> chunks) {
    save(document, chunks);
  }

  /** Used by {@link KnowledgeAutoIngestRunner} to make startup ingestion idempotent. */
  boolean existsDocument(String documentId, String version);

  List<KnowledgeDocumentSummary> listDocuments();

  default Optional<KnowledgeDocumentDetail> findDocument(String documentId, String version) {
    return Optional.empty();
  }
}
