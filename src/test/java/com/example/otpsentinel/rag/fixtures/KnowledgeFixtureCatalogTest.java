package com.example.otpsentinel.rag.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.rag.KnowledgeDocument;
import com.example.otpsentinel.rag.KnowledgeDocumentType;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeFixtureCatalogTest {

  @Test
  void providesAUniqueInstitutionalDemoPackAcrossAllAllowedDocumentTypes() {
    List<KnowledgeDocument> documents = KnowledgeFixtureCatalog.mvpDocuments();

    assertThat(documents).hasSizeGreaterThanOrEqualTo(16);
    assertThat(documents).extracting(KnowledgeDocument::documentId).doesNotHaveDuplicates();
    assertThat(documents)
        .extracting(KnowledgeDocument::documentType)
        .contains(
            KnowledgeDocumentType.INCIDENT_POSTMORTEM,
            KnowledgeDocumentType.RUNBOOK,
            KnowledgeDocumentType.ERROR_REFERENCE,
            KnowledgeDocumentType.PROVIDER_PLAYBOOK,
            KnowledgeDocumentType.CHANGE_POLICY);
    assertThat(documents)
        .allSatisfy(
            document -> {
              assertThat(document.language()).isEqualTo("tr");
              assertThat(document.rawContent()).doesNotContain("+90", "OTP code", "müşteri adı");
            });
  }
}
