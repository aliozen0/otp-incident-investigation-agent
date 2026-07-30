package com.example.otpsentinel.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InvestigationTest {

  private static final Instant BASE = Instant.parse("2026-07-30T11:15:00Z");
  private static final TimeWindow WINDOW = new TimeWindow(BASE, BASE.plusSeconds(900));

  private Investigation received() {
    return Investigation.receive("Why did OTP delivery drop?", WINDOW, "v1", "v1");
  }

  private Investigation collectingEvidence() {
    Investigation investigation = received();
    investigation.startCollectingEvidence();
    return investigation;
  }

  private Evidence metricEvidence(String id, String metricName) {
    return new Evidence(
        id,
        "OTP_METRICS",
        "tool:getOtpMetrics:exec-1",
        "observed",
        BASE,
        metricName,
        72.1,
        "percent");
  }

  private Evidence plainEvidence(String id) {
    return new Evidence(
        id, "QUEUE_HEALTH", "tool:getQueueHealth:exec-1", "queue healthy", BASE, null, null, null);
  }

  @Test
  void startsInReceivedPhase() {
    assertThat(received().phase()).isEqualTo(InvestigationPhase.RECEIVED);
  }

  @Test
  void followsTheHappyPathLifecycle() {
    Investigation investigation = collectingEvidence();
    investigation.addEvidence(metricEvidence("ev-1", "successRate.current"));
    investigation.addEvidence(metricEvidence("ev-2", "successRate.previous"));
    investigation.startGeneratingAnalysis();

    Hypothesis hypothesis =
        new Hypothesis(
            1,
            "Connection pool exhaustion",
            0.85,
            List.of("ev-1", "ev-2"),
            List.of(),
            List.of("Inspect pool metrics"));
    investigation.proposeAnalysis(Severity.HIGH, List.of(hypothesis), List.of(), List.of(), 0.87);
    investigation.startValidating();
    investigation.complete(
        InvestigationStatus.ANOMALY_CONFIRMED, ValidationReport.passed(List.of()));

    assertThat(investigation.phase()).isEqualTo(InvestigationPhase.COMPLETED);
    assertThat(investigation.resultStatus()).isEqualTo(InvestigationStatus.ANOMALY_CONFIRMED);
  }

  @Test
  void rejectsEvidenceAddedOutsideCollectingPhase() {
    Investigation investigation = received();

    assertThatThrownBy(() -> investigation.addEvidence(plainEvidence("ev-1")))
        .isInstanceOf(IllegalStateException.class);
  }

  // Invariant 1: a completed analysis contains at least one evidence item.
  @Test
  void rejectsCompletionWithoutEvidence() {
    Investigation investigation = collectingEvidence();
    investigation.startGeneratingAnalysis();
    investigation.proposeAnalysis(Severity.LOW, List.of(), List.of(), List.of(), 0.5);
    investigation.startValidating();

    assertThatThrownBy(
            () ->
                investigation.complete(
                    InvestigationStatus.NO_ANOMALY, ValidationReport.passed(List.of())))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsCompletionWithAtLeastOneEvidence() {
    Investigation investigation = collectingEvidence();
    investigation.addEvidence(plainEvidence("ev-1"));
    investigation.startGeneratingAnalysis();
    investigation.proposeAnalysis(Severity.LOW, List.of(), List.of(), List.of(), 0.4);
    investigation.startValidating();
    investigation.complete(InvestigationStatus.NO_ANOMALY, ValidationReport.passed(List.of()));

    assertThat(investigation.resultStatus()).isEqualTo(InvestigationStatus.NO_ANOMALY);
  }

  // Invariant 2: ANOMALY_CONFIRMED requires current and previous metric evidence.
  @Test
  void rejectsAnomalyConfirmedWithoutCurrentAndPreviousMetricEvidence() {
    Investigation investigation = collectingEvidence();
    investigation.addEvidence(plainEvidence("ev-1"));
    investigation.startGeneratingAnalysis();
    investigation.proposeAnalysis(Severity.HIGH, List.of(), List.of(), List.of(), 0.8);
    investigation.startValidating();

    assertThatThrownBy(
            () ->
                investigation.complete(
                    InvestigationStatus.ANOMALY_CONFIRMED, ValidationReport.passed(List.of())))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsAnomalyConfirmedWithCurrentAndPreviousMetricEvidence() {
    Investigation investigation = collectingEvidence();
    investigation.addEvidence(metricEvidence("ev-1", "successRate.current"));
    investigation.addEvidence(metricEvidence("ev-2", "successRate.previous"));
    investigation.startGeneratingAnalysis();
    investigation.proposeAnalysis(Severity.HIGH, List.of(), List.of(), List.of(), 0.87);
    investigation.startValidating();
    investigation.complete(
        InvestigationStatus.ANOMALY_CONFIRMED, ValidationReport.passed(List.of()));

    assertThat(investigation.resultStatus()).isEqualTo(InvestigationStatus.ANOMALY_CONFIRMED);
  }

  // Invariant 4: at most three hypotheses.
  @Test
  void rejectsMoreThanThreeHypotheses() {
    Investigation investigation = collectingEvidence();
    investigation.addEvidence(plainEvidence("ev-1"));
    investigation.startGeneratingAnalysis();

    List<Hypothesis> fourHypotheses =
        List.of(
            new Hypothesis(1, "cause 1", 0.5, List.of("ev-1"), List.of(), List.of()),
            new Hypothesis(2, "cause 2", 0.4, List.of("ev-1"), List.of(), List.of()),
            new Hypothesis(3, "cause 3", 0.3, List.of("ev-1"), List.of(), List.of()),
            new Hypothesis(4, "cause 4", 0.2, List.of("ev-1"), List.of(), List.of()));

    assertThatThrownBy(
            () ->
                investigation.proposeAnalysis(
                    Severity.LOW, fourHypotheses, List.of(), List.of(), 0.5))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsExactlyThreeHypotheses() {
    Investigation investigation = collectingEvidence();
    investigation.addEvidence(plainEvidence("ev-1"));
    investigation.startGeneratingAnalysis();

    List<Hypothesis> threeHypotheses =
        List.of(
            new Hypothesis(1, "cause 1", 0.5, List.of("ev-1"), List.of(), List.of()),
            new Hypothesis(2, "cause 2", 0.4, List.of("ev-1"), List.of(), List.of()),
            new Hypothesis(3, "cause 3", 0.3, List.of("ev-1"), List.of(), List.of()));

    investigation.proposeAnalysis(Severity.LOW, threeHypotheses, List.of(), List.of(), 0.5);

    assertThat(investigation.hypotheses()).hasSize(3);
  }

  // Invariant 5: confidence must be within 0..1.
  @Test
  void rejectsConfidenceAboveOne() {
    Investigation investigation = collectingEvidence();
    investigation.addEvidence(plainEvidence("ev-1"));
    investigation.startGeneratingAnalysis();

    assertThatThrownBy(
            () -> investigation.proposeAnalysis(Severity.LOW, List.of(), List.of(), List.of(), 1.5))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsConfidenceWithinRange() {
    Investigation investigation = collectingEvidence();
    investigation.addEvidence(plainEvidence("ev-1"));
    investigation.startGeneratingAnalysis();

    investigation.proposeAnalysis(Severity.LOW, List.of(), List.of(), List.of(), 0.87);

    assertThat(investigation.confidence()).isEqualTo(0.87);
  }

  // Invariant 9: a hypothesis cannot reference an evidence id that was not collected.
  @Test
  void rejectsHypothesisReferencingUnknownEvidenceId() {
    Investigation investigation = collectingEvidence();
    investigation.addEvidence(plainEvidence("ev-1"));
    investigation.startGeneratingAnalysis();

    Hypothesis hypothesis =
        new Hypothesis(1, "cause", 0.5, List.of("ev-unknown"), List.of(), List.of());

    assertThatThrownBy(
            () ->
                investigation.proposeAnalysis(
                    Severity.LOW, List.of(hypothesis), List.of(), List.of(), 0.5))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsHypothesisReferencingCollectedEvidenceId() {
    Investigation investigation = collectingEvidence();
    investigation.addEvidence(plainEvidence("ev-1"));
    investigation.startGeneratingAnalysis();

    Hypothesis hypothesis = new Hypothesis(1, "cause", 0.5, List.of("ev-1"), List.of(), List.of());
    investigation.proposeAnalysis(Severity.LOW, List.of(hypothesis), List.of(), List.of(), 0.5);

    assertThat(investigation.hypotheses()).containsExactly(hypothesis);
  }

  @Test
  void failCanHappenFromAnyNonTerminalPhase() {
    Investigation investigation = collectingEvidence();

    investigation.fail("getOtpMetrics failed");

    assertThat(investigation.phase()).isEqualTo(InvestigationPhase.FAILED);
    assertThat(investigation.resultStatus()).isEqualTo(InvestigationStatus.FAILED);
  }

  @Test
  void rejectsCompletionFromWrongPhase() {
    Investigation investigation = received();

    assertThatThrownBy(
            () ->
                investigation.complete(
                    InvestigationStatus.NO_ANOMALY, ValidationReport.passed(List.of())))
        .isInstanceOf(IllegalStateException.class);
  }
}
