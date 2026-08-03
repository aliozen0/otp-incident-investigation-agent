package com.example.otpsentinel.domain;

/** Canonical application-owned metadata for one retrieved knowledge chunk (ADR-008/ADR-018). */
public record KnowledgeCitation(
    String documentId, String version, String title, String chunkId, double similarityScore) {

  public KnowledgeCitation {
    if (documentId == null || documentId.isBlank()) {
      throw new IllegalArgumentException("documentId must not be blank");
    }
    if (version == null || version.isBlank()) {
      throw new IllegalArgumentException("version must not be blank");
    }
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title must not be blank");
    }
    if (chunkId == null || chunkId.isBlank()) {
      throw new IllegalArgumentException("chunkId must not be blank");
    }
    if (similarityScore < 0.0 || similarityScore > 1.0) {
      throw new IllegalArgumentException("similarityScore must be within 0..1");
    }
  }
}
