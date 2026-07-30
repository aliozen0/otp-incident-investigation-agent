package com.example.otpsentinel.rag.fixtures;

import java.time.LocalDate;
import java.util.List;

/**
 * A document expressed as raw fields (unparsed {@code documentTypeRaw}), for fixtures that are
 * meant to be rejected or otherwise probed before/without becoming a valid {@code
 * KnowledgeDocument} — see {@link KnowledgeFixtureCatalog#negativeMarketingDocument()} and {@link
 * KnowledgeFixtureCatalog#injectionDocument()}.
 */
public record RawKnowledgeDocument(
    String documentId,
    String version,
    String title,
    String documentTypeRaw,
    String provider,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    String language,
    List<String> tags,
    String rawContent) {}
