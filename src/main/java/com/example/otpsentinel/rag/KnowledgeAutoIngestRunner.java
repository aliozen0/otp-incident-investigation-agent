package com.example.otpsentinel.rag;

import com.example.otpsentinel.rag.fixtures.KnowledgeFixtureCatalog;
import java.util.Objects;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * Ingests the curated synthetic knowledge fixture set (docs/15-demo-fixtures.md) on application
 * startup so {@code AI_MODE=live}'s pgvector RAG has real content without a manual step
 * (prompts/handoff/M9-prompt.md item 1). Idempotent: skips any document/version already present, so
 * repeated {@code docker compose up} restarts never duplicate rows. A no-op ({@code enabled =
 * false}) in stub mode, where {@link KnowledgeSearchPort} is the fixture-backed {@code
 * FixtureKnowledgeSearchPort} and pgvector content is irrelevant.
 */
public final class KnowledgeAutoIngestRunner implements ApplicationRunner {

  private final KnowledgeIngestionService ingestionService;
  private final KnowledgeRepository repository;
  private final boolean enabled;

  public KnowledgeAutoIngestRunner(
      KnowledgeIngestionService ingestionService, KnowledgeRepository repository, boolean enabled) {
    this.ingestionService =
        Objects.requireNonNull(ingestionService, "ingestionService must not be null");
    this.repository = Objects.requireNonNull(repository, "repository must not be null");
    this.enabled = enabled;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!enabled) {
      return;
    }
    // ponytail: check-then-insert isn't safe against concurrent app instances; single-container
    // demo makes this fine, add a DB-level unique constraint + ON CONFLICT if that ever changes.
    for (KnowledgeDocument document : KnowledgeFixtureCatalog.mvpDocuments()) {
      if (!repository.existsDocument(document.documentId(), document.version())) {
        ingestionService.ingest(document);
      }
    }
  }
}
