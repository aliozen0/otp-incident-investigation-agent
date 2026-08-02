package com.example.otpsentinel.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * M11 quick mode: same live-signal evidence as thorough mode, minus the RAG knowledge lookup. It
 * must NOT be a truncated run — no tool-budget exhaustion, no {@code PARTIAL_ANALYSIS}, same
 * evidence count as thorough, hypotheses present; only {@code knowledgeReferences} differs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuickModeControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void quickModeReturnsACompleteResultWithoutKnowledgeReferences() {
    ResponseEntity<String> thorough = investigate("thorough");
    ResponseEntity<String> quick = investigate("quick");

    assertThat(thorough.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(quick.getStatusCode()).isEqualTo(HttpStatus.OK);

    String quickBody = quick.getBody();
    assertThat(quickBody).doesNotContain("PARTIAL_ANALYSIS");
    assertThat(quickBody).doesNotContain("tool policy limit reached");
    assertThat(quickBody).contains("\"status\":\"ANOMALY_CONFIRMED\"");
    // Every non-RAG tool ran to completion, so quick mode sees exactly as much live evidence as
    // thorough mode does (RAG never mints evidence, only knowledge references).
    assertThat(countEvidence(quickBody)).isEqualTo(countEvidence(thorough.getBody()));
    assertThat(count(quickBody, "\"possibleCause\"")).isGreaterThan(0);

    // The only difference: quick mode skipped the knowledge lookup entirely.
    assertThat(thorough.getBody()).contains("INC-2026-041");
    assertThat(quickBody).doesNotContain("INC-2026-041");
    assertThat(quickBody).contains("\"knowledgeReferences\":[]");
  }

  private ResponseEntity<String> investigate(String mode) {
    String body =
        """
        {"question":"why did OTP success rate drop",
         "timeWindow":{"startAt":"2026-07-30T11:15:00Z","endAt":"2026-07-30T11:30:00Z"},
         "mode":"%s"}
        """
            .formatted(mode);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.postForEntity(
        "/api/v1/investigations", new HttpEntity<>(body, headers), String.class);
  }

  private int countEvidence(String responseBody) {
    return count(responseBody, "\"sourceType\"");
  }

  private int count(String body, String needle) {
    return body.split(java.util.regex.Pattern.quote(needle), -1).length - 1;
  }
}
