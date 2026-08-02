package com.example.otpsentinel.rag;

/**
 * A retrieved chunk with its citation fields (docs/08-rag-spec.md "Citation") plus the sanitized
 * content itself. {@code documentId}/{@code version}/{@code chunkId} are always application data,
 * never model output (ADR-008).
 */
public record KnowledgeSearchResult(
    String documentId,
    String version,
    String title,
    String chunkId,
    String sectionTitle,
    double similarityScore,
    String content) {

  public KnowledgeSearchResult(
      String documentId,
      String version,
      String title,
      String chunkId,
      double similarityScore,
      String content) {
    this(documentId, version, title, chunkId, null, similarityScore, content);
  }
}
