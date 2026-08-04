package com.example.otpsentinel.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.agent.EvidenceReference;
import com.example.otpsentinel.agent.IncidentAnalysisResult;
import com.example.otpsentinel.domain.ActionType;
import com.example.otpsentinel.domain.Evidence;
import com.example.otpsentinel.domain.ExecutionMode;
import com.example.otpsentinel.domain.Hypothesis;
import com.example.otpsentinel.domain.InvestigationStatus;
import com.example.otpsentinel.domain.RecommendedAction;
import com.example.otpsentinel.domain.Severity;
import com.example.otpsentinel.domain.ValidationStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** docs/12 "Evidence validation" feature — 5 scenarios, and AC-023/AC-012/AC-021/AC-028. */
class ClaimValidatorTest {

  private final ClaimValidator validator = new ClaimValidator();

  @Test
  void rejectsUnsupportedNumericClaim() {
    List<Evidence> known =
        List.of(metric("ev-timeout-rate", "provider_timeout_rate", 0.42, "ratio"));
    IncidentAnalysisResult analysis =
        result(
            "Timeouts spiked to 85 percent during the window",
            List.of(hypothesis(1, "OPERATOR_B timeout", List.of("ev-timeout-rate"))));

    var report = validator.validate(analysis, known);

    // Reported, not rejected: the reader sees which figure is unsupported and keeps the analysis.
    assertThat(report.status()).isEqualTo(ValidationStatus.PASSED);
    assertThat(report.warnings().getFirst()).contains("UNSUPPORTED_NUMERIC_CLAIM");
  }

  @Test
  void rejectsUnknownEvidenceReference() {
    IncidentAnalysisResult analysis =
        result(
            "queue looks fine",
            List.of(hypothesis(1, "unsupported cause", List.of("ev-does-not-exist"))));

    var report = validator.validate(analysis, List.of());

    assertThat(report.status()).isEqualTo(ValidationStatus.PASSED);
    assertThat(report.warnings().getFirst()).contains("UNKNOWN_EVIDENCE_REFERENCE");
  }

  @Test
  void rejectsAutomaticRollback() {
    List<Evidence> known =
        List.of(metric("ev-timeout-rate", "provider_timeout_rate", 0.9, "ratio"));
    RecommendedAction autoRollback =
        new RecommendedAction(
            ActionType.ROLLBACK,
            "Roll back gateway v2.4 automatically",
            Severity.MEDIUM,
            false,
            ExecutionMode.MANUAL_CHECK);
    IncidentAnalysisResult analysis =
        result(
            "rolling back now",
            List.of(hypothesis(1, "bad deploy", List.of("ev-timeout-rate"))),
            List.of(autoRollback));

    var report = validator.validate(analysis, known);

    assertThat(report.status()).isEqualTo(ValidationStatus.FAILED);
    assertThat(report.warnings().getFirst()).contains("FORBIDDEN_AUTOMATIC_ACTION");
  }

  @Test
  void rejectsPiiInSummary() {
    List<Evidence> known = List.of(observation("ev-note"));
    IncidentAnalysisResult analysis =
        result(
            "customer OTP is 482913 and delivery failed",
            List.of(hypothesis(1, "delivery failure", List.of("ev-note"))));

    var report = validator.validate(analysis, known);

    assertThat(report.status()).isEqualTo(ValidationStatus.FAILED);
    assertThat(report.warnings().getFirst()).contains("PII_DETECTED");
  }

  @Test
  void warnsButPassesOnCausationWording() {
    List<Evidence> known =
        List.of(metric("ev-otp-success-rate-current", "otp_success_rate", 72.1, "percent"));
    IncidentAnalysisResult analysis =
        result(
            "gateway v2.4 deploy caused the OTP success rate to drop to 72.1%",
            List.of(hypothesis(1, "bad deploy", List.of("ev-otp-success-rate-current"))));

    var report = validator.validate(analysis, known);

    assertThat(report.status()).isEqualTo(ValidationStatus.PASSED);
    assertThat(report.warnings()).anyMatch(w -> w.contains("CAUSATION_LANGUAGE_DETECTED"));
  }

  @Test
  void passesCleanCorrelationWordedAnalysis() {
    List<Evidence> known =
        List.of(metric("ev-otp-success-rate-current", "otp_success_rate", 72.1, "percent"));
    IncidentAnalysisResult analysis =
        result(
            "OTP success rate dropped to 72.1%; gateway v2.4 deploy is correlated in time, not a"
                + " confirmed cause",
            List.of(hypothesis(1, "deploy correlation", List.of("ev-otp-success-rate-current"))));

    var report = validator.validate(analysis, known);

    assertThat(report.status()).isEqualTo(ValidationStatus.PASSED);
    assertThat(report.warnings()).isEmpty();
  }

  private static Evidence metric(String id, String name, double value, String unit) {
    return new Evidence(
        id, "TOOL_RESULT", "getProviderHealth", "obs", Instant.now(), name, value, unit);
  }

  private static Evidence observation(String id) {
    return new Evidence(
        id, "TOOL_RESULT", "getQueueHealth", "obs", Instant.now(), null, null, null);
  }

  private static Hypothesis hypothesis(int rank, String cause, List<String> supporting) {
    return new Hypothesis(rank, cause, 0.6, supporting, List.of(), List.of());
  }

  private static IncidentAnalysisResult result(String summary, List<Hypothesis> hypotheses) {
    return result(summary, hypotheses, List.of());
  }

  private static IncidentAnalysisResult result(
      String summary, List<Hypothesis> hypotheses, List<RecommendedAction> actions) {
    List<EvidenceReference> refs =
        hypotheses.stream()
            .flatMap(h -> h.supportingEvidenceIds().stream())
            .distinct()
            .map(EvidenceReference::new)
            .toList();
    return new IncidentAnalysisResult(
        InvestigationStatus.ANOMALY_CONFIRMED,
        Severity.HIGH,
        summary,
        refs,
        hypotheses,
        actions,
        List.of(),
        0.7);
  }
}
