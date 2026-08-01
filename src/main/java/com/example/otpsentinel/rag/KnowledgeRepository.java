package com.example.otpsentinel.rag;

import java.util.List;

/** Port: persists an ingested {@link KnowledgeDocument} and its embedded chunks. */
public interface KnowledgeRepository {

  void save(KnowledgeDocument document, List<EmbeddedChunk> chunks);

  /** Used by {@link KnowledgeAutoIngestRunner} to make startup ingestion idempotent. */
  boolean existsDocument(String documentId, String version);
}
