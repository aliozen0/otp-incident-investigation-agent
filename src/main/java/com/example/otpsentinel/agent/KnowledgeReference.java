package com.example.otpsentinel.agent;

/** Id-only citation of a T-006 search result (ADR-008: documentId/chunkId are application data). */
public record KnowledgeReference(String documentId, String chunkId) {
  public KnowledgeReference {
    if (documentId == null || documentId.isBlank()) {
      throw new IllegalArgumentException("documentId must not be blank");
    }
    if (chunkId == null || chunkId.isBlank()) {
      throw new IllegalArgumentException("chunkId must not be blank");
    }
  }
}
