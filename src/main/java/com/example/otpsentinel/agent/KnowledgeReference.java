package com.example.otpsentinel.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Map;

/** Id-only citation of a T-006 search result (ADR-008: documentId/chunkId are application data). */
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public record KnowledgeReference(String documentId, String chunkId) {
  public KnowledgeReference {
    if (documentId == null || documentId.isBlank()) {
      throw new IllegalArgumentException("documentId must not be blank");
    }
    if (chunkId == null || chunkId.isBlank()) {
      throw new IllegalArgumentException("chunkId must not be blank");
    }
  }

  /**
   * Same leniency as {@link EvidenceReference}: models emit either the citation object or the bare
   * chunk id. A bare id is read as the chunk id and the document id is taken from its prefix
   * ({@code doc#v1#c0}); the citation is still resolved against real search results downstream.
   */
  @JsonCreator
  public static KnowledgeReference of(Object raw) {
    if (raw instanceof String id) {
      String documentId = id.contains("#") ? id.substring(0, id.indexOf('#')) : id;
      return new KnowledgeReference(documentId, id);
    }
    if (raw instanceof Map<?, ?> fields) {
      Object documentId = fields.get("documentId");
      Object chunkId = fields.get("chunkId");
      return new KnowledgeReference(
          documentId == null ? null : documentId.toString(),
          chunkId == null ? null : chunkId.toString());
    }
    throw new IllegalArgumentException("knowledge citation must be an id or an object with one");
  }
}
