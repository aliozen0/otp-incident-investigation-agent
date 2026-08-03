package com.example.otpsentinel.rag;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record KnowledgeDocumentSummary(
    String documentId,
    String version,
    String title,
    KnowledgeDocumentType documentType,
    String provider,
    List<String> tags,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    String language,
    int chunkCount,
    String embeddingModel,
    Instant createdAt) {}
