package com.example.otpsentinel.agent;

import com.example.otpsentinel.domain.Hypothesis;
import com.example.otpsentinel.domain.InvestigationStatus;
import com.example.otpsentinel.domain.RecommendedAction;
import com.example.otpsentinel.domain.Severity;
import java.util.List;
import java.util.Objects;

/**
 * Model-facing structured output (docs/07 "Structured result"). Deliberately drops {@code
 * timeWindow} and {@code approvalRequired} from the literal docs/07 record — both are
 * deterministically derivable by the application without asking the model to restate them (see
 * plan "Design decisions", #2).
 */
public record IncidentAnalysisResult(
    InvestigationStatus status,
    Severity severity,
    String summary,
    List<EvidenceReference> evidence,
    List<Hypothesis> hypotheses,
    List<RecommendedAction> recommendedActions,
    List<KnowledgeReference> knowledgeReferences,
    double confidence) {

  public IncidentAnalysisResult {
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(severity, "severity must not be null");
    if (summary == null || summary.isBlank()) {
      throw new IllegalArgumentException("summary must not be blank");
    }
    Objects.requireNonNull(evidence, "evidence must not be null");
    Objects.requireNonNull(hypotheses, "hypotheses must not be null");
    if (hypotheses.size() > 3) {
      throw new IllegalArgumentException("at most 3 hypotheses are allowed");
    }
    Objects.requireNonNull(recommendedActions, "recommendedActions must not be null");
    Objects.requireNonNull(knowledgeReferences, "knowledgeReferences must not be null");
    if (confidence < 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException("confidence must be within 0..1");
    }
    evidence = List.copyOf(evidence);
    hypotheses = List.copyOf(hypotheses);
    recommendedActions = List.copyOf(recommendedActions);
    knowledgeReferences = List.copyOf(knowledgeReferences);
  }
}
