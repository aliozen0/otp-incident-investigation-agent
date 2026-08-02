package com.example.otpsentinel.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.otpsentinel.rag.fixtures.KnowledgeFixtureCatalog;
import com.example.otpsentinel.rag.fixtures.RawKnowledgeDocument;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeIngestionServiceTest {

  private final List<KnowledgeDocument> saved = new ArrayList<>();
  private final List<EmbeddedChunk> savedChunks = new ArrayList<>();
  private final KnowledgeRepository fakeRepository =
      new KnowledgeRepository() {
        @Override
        public void save(KnowledgeDocument document, List<EmbeddedChunk> chunks) {
          saved.add(document);
          savedChunks.addAll(chunks);
        }

        @Override
        public boolean existsDocument(String documentId, String version) {
          return false;
        }

        @Override
        public List<KnowledgeDocumentSummary> listDocuments() {
          return List.of();
        }
      };
  private final KnowledgeIngestionService service =
      new KnowledgeIngestionService(
          new ContentSanitizer(), new Chunker(), new HashEmbeddingService(64), fakeRepository);

  @Test
  void validDocumentIsSanitizedChunkedEmbeddedAndSaved() {
    service.ingest(KnowledgeFixtureCatalog.incidentPostmortem());

    assertThat(saved).hasSize(1);
    assertThat(saved.get(0).documentId()).isEqualTo("INC-2026-041");
    assertThat(savedChunks).isNotEmpty();
    assertThat(savedChunks)
        .allSatisfy(
            c -> {
              assertThat(c.embedding()).hasSize(64);
              assertThat(c.embeddingModel()).isEqualTo("hash-embedding-v1");
            });
  }

  @Test
  void unknownDocumentTypeIsRejectedBeforeChunkingOrEmbedding() {
    RawKnowledgeDocument marketing = KnowledgeFixtureCatalog.negativeMarketingDocument();

    assertThatThrownBy(
            () ->
                service.ingest(
                    marketing.documentId(),
                    marketing.version(),
                    marketing.title(),
                    marketing.documentTypeRaw(),
                    marketing.provider(),
                    marketing.effectiveFrom(),
                    marketing.effectiveTo(),
                    marketing.language(),
                    marketing.tags(),
                    marketing.rawContent()))
        .isInstanceOf(KnowledgeIngestionRejectedException.class);

    assertThat(saved).isEmpty();
  }

  @Test
  void injectionDocumentIsStoredWithScriptStrippedAndNeverExecuted() {
    RawKnowledgeDocument injection = KnowledgeFixtureCatalog.injectionDocument();

    service.ingest(
        injection.documentId(),
        injection.version(),
        injection.title(),
        injection.documentTypeRaw(),
        injection.provider(),
        injection.effectiveFrom(),
        injection.effectiveTo(),
        injection.language(),
        injection.tags(),
        injection.rawContent());

    assertThat(saved).hasSize(1);
    assertThat(savedChunks)
        .allSatisfy(c -> assertThat(c.chunk().content()).doesNotContain("<script>"));
  }

  @Test
  void oversizedContentIsRejected() {
    KnowledgeDocument document =
        new KnowledgeDocument(
            "DOC-BIG",
            "1",
            "Big",
            KnowledgeDocumentType.RUNBOOK,
            null,
            java.time.LocalDate.parse("2026-01-01"),
            null,
            "tr",
            List.of(),
            "a".repeat(ContentSanitizer.MAX_CONTENT_CHARS + 1));

    assertThatThrownBy(() -> service.ingest(document))
        .isInstanceOf(KnowledgeIngestionRejectedException.class);
    assertThat(saved).isEmpty();
  }
}
