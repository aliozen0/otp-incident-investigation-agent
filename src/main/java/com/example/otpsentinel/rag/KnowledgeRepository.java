package com.example.otpsentinel.rag;

import java.util.List;

/** Port: persists an ingested {@link KnowledgeDocument} and its embedded chunks. */
public interface KnowledgeRepository {

  void save(KnowledgeDocument document, List<EmbeddedChunk> chunks);
}
