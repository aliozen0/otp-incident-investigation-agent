package com.example.otpsentinel.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import com.example.otpsentinel.rag.HashEmbeddingService;
import com.example.otpsentinel.rag.JdbcKnowledgeSearchAdapter;
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

  @Test
  void uploadedDocumentAppearsInListAndIsFindableBySearch() {
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

    ResponseEntity<String> listed =
        restTemplate.getForEntity("/api/v1/knowledge/documents", String.class);
    assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listed.getBody()).contains("Zeta Provider timeout runbook");

    JdbcKnowledgeSearchAdapter searchAdapter =
        new JdbcKnowledgeSearchAdapter(jdbcTemplate, new HashEmbeddingService(1024), 5, 0.10);
    List<KnowledgeSearchResult> results =
        searchAdapter.searchIncidentKnowledge("ZetaProvider timeout connection pool", null, 5);

    assertThat(results).anyMatch(r -> r.title().contains("Zeta Provider timeout runbook"));
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
}
