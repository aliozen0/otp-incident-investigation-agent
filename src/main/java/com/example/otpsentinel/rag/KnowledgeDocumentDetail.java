package com.example.otpsentinel.rag;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Safe, sanitized knowledge document projection for the read-only explorer. */
public record KnowledgeDocumentDetail(
    String documentId,
    String version,
    String title,
    KnowledgeDocumentType documentType,
    String provider,
    List<String> tags,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    String language,
    Instant createdAt,
    String sanitizedContent,
    List<ChunkDetail> chunks) {

  public record ChunkDetail(
      String chunkId, String sectionTitle, String content, int tokenCount, String embeddingModel) {}
}
