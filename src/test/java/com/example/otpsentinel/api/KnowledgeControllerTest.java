package com.example.otpsentinel.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import com.example.otpsentinel.rag.KnowledgeSearchPort;
import com.example.otpsentinel.rag.KnowledgeSearchResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KnowledgeControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  /** The application's really-wired search port — the same bean the agent's RAG tool uses. */
  @Autowired private KnowledgeSearchPort knowledgeSearchPort;

  @Test
  void uploadedDocumentAppearsInListAndIsFindableThroughTheWiredSearchPort() {
    String body =
        """
        {"title":"Zeta Provider timeout runbook",
         "documentType":"RUNBOOK",
         "provider":"ZetaProvider",
         "tags":["zeta","timeout"],
         "effectiveFrom":"2026-01-01",
         "language":"en",
         "content":"When ZetaProvider timeout rate exceeds 20 percent, check its connection pool and circuit breaker state before escalating."}
        """;
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> uploaded =
        restTemplate.postForEntity(
            "/api/v1/knowledge/documents", new HttpEntity<>(body, headers), String.class);
    assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(uploaded.getBody())
        .contains("\"documentId\":\"UPLOAD-")
        .contains("\"version\":\"1\"");

    ResponseEntity<String> listed =
        restTemplate.getForEntity("/api/v1/knowledge/documents", String.class);
    assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listed.getBody())
        .contains("Zeta Provider timeout runbook")
        .contains("\"provider\":\"ZetaProvider\"")
        .contains("\"tags\":[\"zeta\",\"timeout\"]")
        .contains("\"chunkCount\":");

    String documentId = uploaded.getBody().split("\"documentId\":\"")[1].split("\"")[0];
    ResponseEntity<String> detail =
        restTemplate.getForEntity(
            "/api/v1/knowledge/documents/" + documentId + "/versions/1", String.class);
    assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(detail.getBody())
        .contains("connection pool and circuit breaker")
        .contains("\"chunks\"")
        .contains("\"embeddingModel\":\"hash-embedding-v1\"");

    String previewBody =
        """
        {"query":"ZetaProvider timeout connection pool","topK":5}
        """;
    ResponseEntity<String> preview =
        restTemplate.postForEntity(
            "/api/v1/knowledge/search-preview",
            new HttpEntity<>(previewBody, headers),
            String.class);
    assertThat(preview.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(preview.getBody())
        .contains(documentId)
        .contains("\"version\":\"1\"")
        .contains("\"chunkId\"")
        .contains("\"similarityScore\"");

    // M11 finding 4: the wired (non-live) port must merge ingested content, not only the fixture.
    List<KnowledgeSearchResult> results =
        knowledgeSearchPort.searchIncidentKnowledge(
            "ZetaProvider timeout connection pool", null, 5);

    assertThat(results).anyMatch(r -> r.title().contains("Zeta Provider timeout runbook"));
    // ...while the deterministic demo citation is still returned (stub script depends on it).
    assertThat(results).anyMatch(r -> r.documentId().equals("INC-2026-041"));
  }

  @Test
  void rejectsUnknownDocumentTypeWith400() {
    String body =
        """
        {"title":"Marketing blast",
         "documentType":"MARKETING",
         "tags":[],
         "effectiveFrom":"2026-01-01",
         "language":"en",
         "content":"buy now"}
        """;
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/api/v1/knowledge/documents", new HttpEntity<>(body, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("KNOWLEDGE_DOCUMENT_REJECTED");
  }

  @Test
  void rejectsMissingEffectiveFromWith400NotAnUnmapped500() {
    String body =
        """
        {"title":"Runbook without a validity date",
         "documentType":"RUNBOOK",
         "tags":[],
         "language":"en",
         "content":"check the connection pool before escalating to the provider"}
        """;
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/api/v1/knowledge/documents", new HttpEntity<>(body, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("INVALID_REQUEST").contains("effectiveFrom");
  }

  @Test
  void rejectsRetrievalPreviewTopKAboveFive() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/api/v1/knowledge/search-preview",
            new HttpEntity<>("{\"query\":\"connection pool\",\"topK\":6}", headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("INVALID_KNOWLEDGE_SEARCH");
  }
}
