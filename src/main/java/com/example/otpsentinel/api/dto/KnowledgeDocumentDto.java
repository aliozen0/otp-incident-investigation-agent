package com.example.otpsentinel.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record KnowledgeDocumentDto(
    String title,
    String documentType,
    String provider,
    List<String> tags,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    String language,
    String content) {

  /** Response body of a successful upload, so the client can cite/track the generated id. */
  public record UploadResponse(String documentId, String version) {}

  public record ListItem(
      String documentId,
      String version,
      String title,
      String documentType,
      String provider,
      List<String> tags,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      String language,
      int chunkCount,
      String embeddingModel,
      Instant createdAt) {}

  public record Detail(
      String documentId,
      String version,
      String title,
      String documentType,
      String provider,
      List<String> tags,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      String language,
      Instant createdAt,
      String sanitizedContent,
      List<Chunk> chunks) {}

  public record Chunk(
      String chunkId, String sectionTitle, String content, int tokenCount, String embeddingModel) {}

  public record SearchPreviewRequest(String query, String provider, Integer topK) {}

  public record SearchPreviewResponse(List<SearchResult> results) {}

  public record SearchResult(
      String documentId,
      String version,
      String title,
      String chunkId,
      String sectionTitle,
      double similarityScore,
      String contentExcerpt) {}
}
