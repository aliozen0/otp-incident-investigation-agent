package com.example.otpsentinel.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Investigation aggregate root. Lifecycle: RECEIVED -&gt; COLLECTING_EVIDENCE -&gt;
 * GENERATING_ANALYSIS -&gt; VALIDATING -&gt; COMPLETED (or PARTIAL/FAILED).
 */
public final class Investigation {

  private final InvestigationId id;
  private final String question;
  private final TimeWindow resolvedTimeWindow;
  private final String promptVersion;
  private final String schemaVersion;
  private final List<Evidence> evidence = new ArrayList<>();
  private final List<String> toolExecutions = new ArrayList<>();

  private InvestigationPhase phase;
  private InvestigationStatus resultStatus;
  private Severity severity;
  private List<Hypothesis> hypotheses = List.of();
  private List<RecommendedAction> recommendedActions = List.of();
  private List<String> knowledgeReferences = List.of();
  private Double confidence;
  private ValidationReport validationReport;

  private Investigation(
      InvestigationId id,
      String question,
      TimeWindow resolvedTimeWindow,
      String promptVersion,
      String schemaVersion) {
    this.id = id;
    this.question = question;
    this.resolvedTimeWindow = resolvedTimeWindow;
    this.promptVersion = promptVersion;
    this.schemaVersion = schemaVersion;
    this.phase = InvestigationPhase.RECEIVED;
  }

  public static Investigation receive(
      String question, TimeWindow resolvedTimeWindow, String promptVersion, String schemaVersion) {
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("question must not be blank");
    }
    if (resolvedTimeWindow == null) {
      throw new IllegalArgumentException("resolvedTimeWindow must not be null");
    }
    return new Investigation(
        InvestigationId.generate(), question, resolvedTimeWindow, promptVersion, schemaVersion);
  }

  /**
   * Rehydrates an aggregate from persisted state. For repository adapters only: bypasses
   * lifecycle/invariant checks since the state was already validated when it was first persisted.
   */
  public static Investigation reconstitute(
      InvestigationId id,
      String question,
      TimeWindow resolvedTimeWindow,
      String promptVersion,
      String schemaVersion,
      InvestigationPhase phase,
      InvestigationStatus resultStatus,
      Severity severity,
      List<Evidence> evidence,
      List<Hypothesis> hypotheses,
      List<RecommendedAction> recommendedActions,
      List<String> knowledgeReferences,
      Double confidence,
      ValidationReport validationReport,
      List<String> toolExecutions) {
    Investigation investigation =
        new Investigation(id, question, resolvedTimeWindow, promptVersion, schemaVersion);
    investigation.phase = phase;
    investigation.resultStatus = resultStatus;
    investigation.severity = severity;
    investigation.evidence.addAll(evidence);
    investigation.hypotheses = List.copyOf(hypotheses);
    investigation.recommendedActions = List.copyOf(recommendedActions);
    investigation.knowledgeReferences = List.copyOf(knowledgeReferences);
    investigation.confidence = confidence;
    investigation.validationReport = validationReport;
    investigation.toolExecutions.addAll(toolExecutions);
    return investigation;
  }

  public void startCollectingEvidence() {
    requirePhase(InvestigationPhase.RECEIVED);
    phase = InvestigationPhase.COLLECTING_EVIDENCE;
  }

  public void recordToolExecution(String executionId) {
    requirePhase(InvestigationPhase.COLLECTING_EVIDENCE);
    toolExecutions.add(executionId);
  }

  public void addEvidence(Evidence item) {
    requirePhase(InvestigationPhase.COLLECTING_EVIDENCE);
    evidence.add(item);
  }

  public void startGeneratingAnalysis() {
    requirePhase(InvestigationPhase.COLLECTING_EVIDENCE);
    phase = InvestigationPhase.GENERATING_ANALYSIS;
  }

  /** Invariants 4 (max 3 hypotheses), 5 (confidence range) and 9 (evidence id existence). */
  public void proposeAnalysis(
      Severity severity,
      List<Hypothesis> hypotheses,
      List<RecommendedAction> recommendedActions,
      List<String> knowledgeReferences,
      double confidence) {
    requirePhase(InvestigationPhase.GENERATING_ANALYSIS);
    if (hypotheses.size() > 3) {
      throw new IllegalArgumentException("at most 3 hypotheses are allowed");
    }
    if (confidence < 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException("confidence must be within 0..1");
    }
    Set<String> knownEvidenceIds = evidence.stream().map(Evidence::id).collect(Collectors.toSet());
    for (Hypothesis hypothesis : hypotheses) {
      boolean allKnown =
          Stream.concat(
                  hypothesis.supportingEvidenceIds().stream(),
                  hypothesis.contradictingEvidenceIds().stream())
              .allMatch(knownEvidenceIds::contains);
      if (!allKnown) {
        throw new IllegalArgumentException(
            "hypothesis references an evidence id that was not collected");
      }
    }
    this.severity = severity;
    this.hypotheses = List.copyOf(hypotheses);
    this.recommendedActions = List.copyOf(recommendedActions);
    this.knowledgeReferences = List.copyOf(knowledgeReferences);
    this.confidence = confidence;
  }

  public void startValidating() {
    requirePhase(InvestigationPhase.GENERATING_ANALYSIS);
    phase = InvestigationPhase.VALIDATING;
  }

  /**
   * Invariants 1 (evidence required) and 2 (ANOMALY_CONFIRMED needs current+previous metric
   * evidence).
   */
  public void complete(InvestigationStatus resultStatus, ValidationReport validationReport) {
    requirePhase(InvestigationPhase.VALIDATING);
    if (evidence.isEmpty()) {
      throw new IllegalArgumentException(
          "a completed analysis must contain at least one evidence item");
    }
    if (resultStatus == InvestigationStatus.ANOMALY_CONFIRMED) {
      long metricEvidenceCount = evidence.stream().filter(Evidence::isMetric).count();
      if (metricEvidenceCount < 2) {
        throw new IllegalArgumentException(
            "ANOMALY_CONFIRMED requires current and previous period metric evidence");
      }
    }
    this.resultStatus = resultStatus;
    this.validationReport = validationReport;
    this.phase = InvestigationPhase.COMPLETED;
  }

  public void partial(InvestigationStatus resultStatus, ValidationReport validationReport) {
    requireAnyPhase(
        InvestigationPhase.COLLECTING_EVIDENCE,
        InvestigationPhase.GENERATING_ANALYSIS,
        InvestigationPhase.VALIDATING);
    this.resultStatus = resultStatus;
    this.validationReport = validationReport;
    this.phase = InvestigationPhase.PARTIAL;
  }

  public void fail(String reason) {
    requireAnyPhase(
        InvestigationPhase.RECEIVED,
        InvestigationPhase.COLLECTING_EVIDENCE,
        InvestigationPhase.GENERATING_ANALYSIS,
        InvestigationPhase.VALIDATING);
    this.resultStatus = InvestigationStatus.FAILED;
    this.validationReport = ValidationReport.failed(List.of(reason));
    this.phase = InvestigationPhase.FAILED;
  }

  private void requirePhase(InvestigationPhase expected) {
    if (phase != expected) {
      throw new IllegalStateException("expected phase " + expected + " but was " + phase);
    }
  }

  private void requireAnyPhase(InvestigationPhase... expected) {
    for (InvestigationPhase candidate : expected) {
      if (phase == candidate) {
        return;
      }
    }
    throw new IllegalStateException("phase " + phase + " is not one of " + List.of(expected));
  }

  public InvestigationId id() {
    return id;
  }

  public String question() {
    return question;
  }

  public TimeWindow resolvedTimeWindow() {
    return resolvedTimeWindow;
  }

  public InvestigationPhase phase() {
    return phase;
  }

  public InvestigationStatus resultStatus() {
    return resultStatus;
  }

  public Severity severity() {
    return severity;
  }

  public List<Evidence> evidence() {
    return List.copyOf(evidence);
  }

  public List<Hypothesis> hypotheses() {
    return hypotheses;
  }

  public List<RecommendedAction> recommendedActions() {
    return recommendedActions;
  }

  public List<String> knowledgeReferences() {
    return knowledgeReferences;
  }

  public Double confidence() {
    return confidence;
  }

  public ValidationReport validationReport() {
    return validationReport;
  }

  public List<String> toolExecutions() {
    return List.copyOf(toolExecutions);
  }

  public String promptVersion() {
    return promptVersion;
  }

  public String schemaVersion() {
    return schemaVersion;
  }
}
