package com.example.otpsentinel.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuickModeControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void quickModeCollectsFewerEvidenceItemsThanThoroughMode() {
    ResponseEntity<String> thorough = investigate("thorough");
    ResponseEntity<String> quick = investigate("quick");

    int thoroughEvidenceCount = countEvidence(thorough.getBody());
    int quickEvidenceCount = countEvidence(quick.getBody());

    assertThat(quickEvidenceCount).isLessThan(thoroughEvidenceCount);
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
    return responseBody.split("\"sourceType\"", -1).length - 1;
  }
}
