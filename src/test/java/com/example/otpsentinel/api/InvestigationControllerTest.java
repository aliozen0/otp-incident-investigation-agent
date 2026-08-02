package com.example.otpsentinel.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class InvestigationControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void rejectsFutureTimeWindowWith400() {
    String body =
        """
        {"question":"why did OTP success rate drop suddenly today",
         "timeWindow":{"startAt":"%s","endAt":"%s"}}
        """
            .formatted(Instant.now().minusSeconds(60), Instant.now().plusSeconds(300));
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/api/v1/investigations", new HttpEntity<>(body, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("INVALID_TIME_WINDOW");
  }

  @Test
  void rejectsIntervalLongerThan24HoursWith400() {
    Instant end = Instant.now().minus(1, ChronoUnit.MINUTES);
    Instant start = end.minus(25, ChronoUnit.HOURS);
    String body =
        """
        {"question":"why did OTP success rate drop suddenly today",
         "timeWindow":{"startAt":"%s","endAt":"%s"}}
        """
            .formatted(start, end);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/api/v1/investigations", new HttpEntity<>(body, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("INVALID_TIME_WINDOW");
  }

  @Test
  void rejectsMalformedIdWith400NotServerError() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/api/v1/investigations/not-a-uuid", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("INVALID_REQUEST");
  }

  @Test
  void investigatesTheOtpDropOneOhOneFixtureAndAllowsRefetch() {
    String body =
        """
        {"question":"why did OTP success rate drop",
         "timeWindow":{"startAt":"2026-07-30T11:15:00Z","endAt":"2026-07-30T11:30:00Z"}}
        """;
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> created =
        restTemplate.postForEntity(
            "/api/v1/investigations", new HttpEntity<>(body, headers), String.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(created.getBody()).contains("ANOMALY_CONFIRMED");
    assertThat(created.getBody())
        .contains("OTP success rate dropped to 72.10%")
        .contains("\"version\":\"1\"")
        .contains("\"chunkId\":\"INC-2026-041#v1#c0\"")
        .contains("\"similarityScore\":0.85");
    String investigationId = created.getBody().split("\"investigationId\":\"")[1].split("\"")[0];

    ResponseEntity<String> fetched =
        restTemplate.getForEntity("/api/v1/investigations/" + investigationId, String.class);

    assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(fetched.getBody())
        .contains("ANOMALY_CONFIRMED")
        .contains("OTP success rate dropped to 72.10%")
        .contains("\"chunkId\":\"INC-2026-041#v1#c0\"");
  }
}
