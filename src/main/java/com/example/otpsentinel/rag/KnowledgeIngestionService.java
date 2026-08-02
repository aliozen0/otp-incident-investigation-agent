package com.example.otpsentinel.rag;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Sanitizes, chunks, embeds and persists a raw knowledge document (docs/08-rag-spec.md, DATA-003).
 * Independent of the agent/tool-calling layer (prompts/handoff/M4-prompt.md constraint) — nothing
 * here is a LangChain4j {@code @Tool}.
 */
public final class KnowledgeIngestionService {

  private final ContentSanitizer sanitizer;
  private final Chunker chunker;
  private final EmbeddingService embeddingService;
  private final KnowledgeRepository repository;

  public KnowledgeIngestionService(
      ContentSanitizer sanitizer,
      Chunker chunker,
      EmbeddingService embeddingService,
      KnowledgeRepository repository) {
    this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer must not be null");
    this.chunker = Objects.requireNonNull(chunker, "chunker must not be null");
    this.embeddingService =
        Objects.requireNonNull(embeddingService, "embeddingService must not be null");
    this.repository = Objects.requireNonNull(repository, "repository must not be null");
  }

  /**
   * Parses {@code documentTypeRaw} against the {@link KnowledgeDocumentType} allowlist before doing
   * anything else — an unknown type (e.g. a marketing document per docs/15's negative fixture) is
   * rejected here and never reaches sanitization, chunking or pgvector.
   *
   * @throws KnowledgeIngestionRejectedException if {@code documentTypeRaw} is not an allowed type.
   */
  public void ingest(
      String documentId,
      String version,
      String title,
      String documentTypeRaw,
      String provider,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      String language,
      List<String> tags,
      String rawContent) {
    KnowledgeDocumentType documentType;
    try {
      documentType = KnowledgeDocumentType.valueOf(documentTypeRaw);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new KnowledgeIngestionRejectedException(
          "documentType '" + documentTypeRaw + "' is not an allowed knowledge document type");
    }
    ingest(
        new KnowledgeDocument(
            documentId,
            version,
            title,
            documentType,
            provider,
            effectiveFrom,
            effectiveTo,
            language,
            tags,
            rawContent));
  }

  /**
   * @throws KnowledgeIngestionRejectedException if the sanitized content exceeds the size limit;
   *     the document is never chunked, embedded or stored in that case.
   */
  public void ingest(KnowledgeDocument document) {
    Objects.requireNonNull(document, "document must not be null");

    String sanitized = sanitizer.sanitize(document.rawContent());
    String withFrontMatter = frontMatter(document) + "\n\n" + sanitized;

    List<DocumentChunk> chunks =
        chunker.chunk(document.documentId(), document.version(), withFrontMatter);

    List<EmbeddedChunk> embedded =
        chunks.stream()
            .map(
                c ->
                    new EmbeddedChunk(
                        c,
                        embeddingService.embed(c.content(), EmbeddingInputType.PASSAGE),
                        embeddingService.modelId()))
            .toList();

    repository.save(document, sanitized, embedded);
  }

  /**
   * A synthetic first section carrying the title and tags into the searchable text (as its own
   * chunk) — metadata a real embedding model would also weigh, and here it also keeps the hashing
   * test double's evaluation-set queries (docs/08 "Evaluation set") groundable in tag vocabulary.
   */
  private String frontMatter(KnowledgeDocument document) {
    return "## Özet\n" + document.title() + "\nEtiketler: " + String.join(" ", document.tags());
  }
}
