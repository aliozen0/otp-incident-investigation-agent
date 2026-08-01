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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IncidentDraftPreviewControllerTest extends AbstractPostgresIntegrationTest {

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

  @Test
  void previewDoesNotPersistAnIncidentDraft() {
    Investigation investigation = completedInvestigation();
    newInvestigationRepository().save(investigation);

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/api/v1/investigations/" + investigation.id() + "/incident-draft/preview",
            null,
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"requiresExplicitApproval\":true");
    Integer draftCount =
        jdbcTemplate.queryForObject("SELECT count(*) FROM incident_draft", Integer.class);
    assertThat(draftCount).isZero();
  }
}
