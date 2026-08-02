package com.example.otpsentinel.api.dto;

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
      LocalDate effectiveFrom) {}
}
