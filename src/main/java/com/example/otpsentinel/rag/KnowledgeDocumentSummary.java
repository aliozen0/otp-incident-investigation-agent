package com.example.otpsentinel.rag;

import java.time.LocalDate;

public record KnowledgeDocumentSummary(
    String documentId,
    String version,
    String title,
    KnowledgeDocumentType documentType,
    LocalDate effectiveFrom) {}
