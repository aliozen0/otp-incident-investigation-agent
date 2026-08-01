package com.example.otpsentinel.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import com.example.otpsentinel.domain.Evidence;
import com.example.otpsentinel.domain.Hypothesis;
import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.domain.InvestigationStatus;
import com.example.otpsentinel.domain.Severity;
import com.example.otpsentinel.domain.TimeWindow;
import com.example.otpsentinel.domain.ValidationReport;
import java.time.Instant;
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
class IncidentDraftDecisionControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  private Investigation completedInvestigation() {
    TimeWindow window =
        new TimeWindow(
            Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z"));
    Investigation investigation =
        Investigation.receive("why did OTP success rate drop", window, "v1", "v1");
    investigation.startCollectingEvidence();
    investigation.addEvidence(
        new Evidence(
            "ev-1",
            "TOOL_RESULT",
            "getOtpMetrics",
            "current 72.1%",
            Instant.now(),
            "otp_success_rate",
            72.1,
            "percent"));
    investigation.addEvidence(
        new Evidence(
            "ev-2",
            "TOOL_RESULT",
            "getOtpMetrics",
            "previous 98.1%",
            Instant.now(),
            "otp_success_rate",
            98.1,
            "percent"));
    investigation.startGeneratingAnalysis();
    investigation.proposeAnalysis(
        Severity.HIGH,
        List.of(
            new Hypothesis(
                1,
                "connection pool exhaustion",
                0.7,
                List.of("ev-1"),
                List.of(),
                List.of("check pool metrics"))),
        List.of(),
        List.of(),
        0.85);
    investigation.startValidating();
    investigation.complete(
        InvestigationStatus.ANOMALY_CONFIRMED, ValidationReport.passed(List.of()));
    return investigation;
  }

  private ResponseEntity<String> decide(
      String investigationId, String key, String decision, String reason) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Idempotency-Key", key);
    String body =
        reason == null
            ? "{\"decision\":\"%s\"}".formatted(decision)
            : "{\"decision\":\"%s\",\"reason\":\"%s\"}".formatted(decision, reason);
    return restTemplate.postForEntity(
        "/api/v1/investigations/" + investigationId + "/incident-draft/decisions",
        new HttpEntity<>(body, headers),
        String.class);
  }

  @Test
  void approvalCreatesExactlyOneIncident() {
    Investigation investigation = completedInvestigation();
    newInvestigationRepository().save(investigation);

    ResponseEntity<String> response =
        decide(
            investigation.id().toString(),
            "idem-001",
            "APPROVE",
            "Teknik ekip incelemesi için gerekli.");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody())
        .contains("\"status\":\"CREATED\"")
        .contains("\"idempotentReplay\":false");
    Integer draftCount =
        jdbcTemplate.queryForObject("SELECT count(*) FROM incident_draft", Integer.class);
    assertThat(draftCount).isEqualTo(1);
    var actions =
        jdbcTemplate.queryForList(
            "SELECT action FROM audit_event WHERE investigation_id = ?",
            String.class,
            investigation.id().value());
    assertThat(actions).contains("APPROVAL_DECIDED", "INCIDENT_CREATED");
  }

  @Test
  void invalidDecisionValueIsRejectedWith400AndPersistsNoDraft() {
    Investigation investigation = completedInvestigation();
    newInvestigationRepository().save(investigation);

    ResponseEntity<String> response =
        decide(investigation.id().toString(), "idem-invalid", "BANANA", "reason");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("INVALID_REQUEST");
    Integer draftCount =
        jdbcTemplate.queryForObject("SELECT count(*) FROM incident_draft", Integer.class);
    assertThat(draftCount).isZero();
  }

  @Test
  void replayedApprovalReturnsOriginalIdAndNoSecondIncident() {
    Investigation investigation = completedInvestigation();
    newInvestigationRepository().save(investigation);
    ResponseEntity<String> first =
        decide(investigation.id().toString(), "idem-002", "APPROVE", "reason");
    String firstId = first.getBody().split("\"incidentDraftId\":\"")[1].split("\"")[0];

    ResponseEntity<String> second =
        decide(investigation.id().toString(), "idem-002", "APPROVE", "reason");

    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(second.getBody()).contains("\"idempotentReplay\":true").contains(firstId);
    Integer draftCount =
        jdbcTemplate.queryForObject("SELECT count(*) FROM incident_draft", Integer.class);
    assertThat(draftCount).isEqualTo(1);
  }

  @Test
  void rejectionCreatesNoIncident() {
    Investigation investigation = completedInvestigation();
    newInvestigationRepository().save(investigation);

    ResponseEntity<String> response =
        decide(investigation.id().toString(), "idem-003", "REJECT", "Known maintenance");

    assertThat(response.getBody()).contains("\"status\":\"REJECTED\"");
    Integer createdCount =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM incident_draft WHERE external_incident_id IS NOT NULL",
            Integer.class);
    assertThat(createdCount).isZero();
    Integer rejectedCount =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM incident_draft WHERE status = 'REJECTED'", Integer.class);
    assertThat(rejectedCount).isEqualTo(1);
  }
}
