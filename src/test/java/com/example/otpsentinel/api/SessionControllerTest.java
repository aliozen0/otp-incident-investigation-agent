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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void returnsEmptyListForUnknownSession() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/api/v1/sessions/no-such-session/investigations", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("[]");
  }

  @Test
  void listsInvestigationsCreatedWithTheSameSessionId() {
    String sessionId = "console-thread-1";
    String body =
        """
        {"question":"why did OTP success rate drop",
         "timeWindow":{"startAt":"2026-07-30T11:15:00Z","endAt":"2026-07-30T11:30:00Z"},
         "sessionId":"%s"}
        """
            .formatted(sessionId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    restTemplate.postForEntity(
        "/api/v1/investigations", new HttpEntity<>(body, headers), String.class);

    ResponseEntity<String> listed =
        restTemplate.getForEntity(
            "/api/v1/sessions/" + sessionId + "/investigations", String.class);

    assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listed.getBody()).contains("ANOMALY_CONFIRMED");
  }
}
