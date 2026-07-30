package com.example.otpsentinel.rag;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * A knowledge document before chunking (docs/08-rag-spec.md "Metadata"). {@code rawContent} is
 * unsanitized markdown as authored — sanitization happens in {@link KnowledgeIngestionService}, not
 * here, so this record can also carry adversarial fixture content untouched for tests.
 */
public record KnowledgeDocument(
    String documentId,
    String version,
    String title,
    KnowledgeDocumentType documentType,
    String provider,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    String language,
    List<String> tags,
    String rawContent) {

  public KnowledgeDocument {
    if (documentId == null || documentId.isBlank()) {
      throw new IllegalArgumentException("documentId must not be blank");
    }
    if (version == null || version.isBlank()) {
      throw new IllegalArgumentException("version must not be blank");
    }
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title must not be blank");
    }
    Objects.requireNonNull(documentType, "documentType must not be null");
    Objects.requireNonNull(effectiveFrom, "effectiveFrom must not be null");
    if (language == null || language.isBlank()) {
      throw new IllegalArgumentException("language must not be blank");
    }
    tags = tags == null ? List.of() : List.copyOf(tags);
    if (rawContent == null || rawContent.isBlank()) {
      throw new IllegalArgumentException("rawContent must not be blank");
    }
  }
}
