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

  public record ListItem(
      String documentId,
      String version,
      String title,
      String documentType,
      LocalDate effectiveFrom) {}
}
