package com.example.otpsentinel.agent;

import com.example.otpsentinel.domain.ActionType;
import com.example.otpsentinel.domain.ExecutionMode;
import com.example.otpsentinel.domain.Hypothesis;
import com.example.otpsentinel.domain.InvestigationStatus;
import com.example.otpsentinel.domain.RecommendedAction;
import com.example.otpsentinel.domain.Severity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Model-facing structured output (docs/07 "Structured result"). Deliberately drops {@code
 * timeWindow} and {@code approvalRequired} from the literal docs/07 record — both are
 * deterministically derivable by the application without asking the model to restate them (see plan
 * "Design decisions", #2).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IncidentAnalysisResult(
    InvestigationStatus status,
    Severity severity,
    String summary,
    List<EvidenceReference> evidence,
    List<Hypothesis> hypotheses,
    List<RecommendedAction> recommendedActions,
    List<KnowledgeReference> knowledgeReferences,
    double confidence,
    List<VisualizationProposal> visualizations) {

  public IncidentAnalysisResult(
      InvestigationStatus status,
      Severity severity,
      String summary,
      List<EvidenceReference> evidence,
      List<Hypothesis> hypotheses,
      List<RecommendedAction> recommendedActions,
      List<KnowledgeReference> knowledgeReferences,
      double confidence) {
    this(
        status,
        severity,
        summary,
        evidence,
        hypotheses,
        recommendedActions,
        knowledgeReferences,
        confidence,
        List.of());
  }

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
    visualizations = visualizations == null ? List.of() : List.copyOf(visualizations);
    if (confidence < 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException("confidence must be within 0..1");
    }
    evidence = List.copyOf(evidence);
    hypotheses = List.copyOf(hypotheses);
    recommendedActions = List.copyOf(recommendedActions);
    knowledgeReferences = List.copyOf(knowledgeReferences);
  }

  /**
   * Reads the model's answer defensively. Hypotheses and actions are domain records with their own
   * invariants (a hypothesis must cite evidence, a high-risk action must require approval), and a
   * live model breaks one of them in a single list entry often enough that strict binding threw
   * away entire correct investigations. An entry that violates an invariant is dropped here — the
   * remaining, valid analysis still goes through ClaimValidator and the aggregate's own checks.
   */
  @JsonCreator
  public static IncidentAnalysisResult of(
      @JsonProperty("status") String status,
      @JsonProperty("severity") String severity,
      @JsonProperty("summary") String summary,
      @JsonProperty("evidence") List<EvidenceReference> evidence,
      @JsonProperty("hypotheses") List<Map<String, Object>> hypotheses,
      @JsonProperty("recommendedActions") List<Map<String, Object>> recommendedActions,
      @JsonProperty("knowledgeReferences") List<KnowledgeReference> knowledgeReferences,
      @JsonProperty("confidence") Object confidence,
      @JsonProperty("visualizations") List<VisualizationProposal> visualizations) {
    return new IncidentAnalysisResult(
        enumValue(InvestigationStatus.class, status, InvestigationStatus.PARTIAL_ANALYSIS),
        enumValue(Severity.class, severity, Severity.MEDIUM),
        summary,
        evidence == null ? List.of() : evidence,
        readHypotheses(hypotheses),
        readActions(recommendedActions),
        knowledgeReferences == null ? List.of() : knowledgeReferences,
        clampConfidence(confidence),
        visualizations == null ? List.of() : visualizations);
  }

  private static List<Hypothesis> readHypotheses(List<Map<String, Object>> raw) {
    if (raw == null) {
      return List.of();
    }
    List<Hypothesis> parsed = new ArrayList<>();
    for (Map<String, Object> item : raw) {
      if (item == null || parsed.size() == 3) {
        continue;
      }
      try {
        parsed.add(
            new Hypothesis(
                (int) number(item.get("rank"), parsed.size() + 1),
                text(item.get("possibleCause")),
                number(item.get("probability"), 0.5),
                strings(item.get("supportingEvidenceIds")),
                strings(item.get("contradictingEvidenceIds")),
                strings(item.get("verificationSteps"))));
      } catch (IllegalArgumentException invalid) {
        // Skip only the offending hypothesis, keep the rest of the analysis.
      }
    }
    return parsed;
  }

  private static List<RecommendedAction> readActions(List<Map<String, Object>> raw) {
    if (raw == null) {
      return List.of();
    }
    List<RecommendedAction> parsed = new ArrayList<>();
    for (Map<String, Object> item : raw) {
      if (item == null) {
        continue;
      }
      try {
        Severity risk = enumValue(Severity.class, text(item.get("risk")), Severity.MEDIUM);
        boolean requiresApproval = Boolean.parseBoolean(String.valueOf(item.get("requiresApproval")));
        parsed.add(
            new RecommendedAction(
                enumValue(ActionType.class, text(item.get("actionType")), ActionType.MANUAL_CHECK),
                text(item.get("description")),
                risk,
                // Invariant 6 rather than a rejection: a high-risk action always needs approval, so
                // a model that forgot the flag gets the safe value instead of losing the action.
                requiresApproval || risk == Severity.HIGH || risk == Severity.CRITICAL,
                enumValue(
                    ExecutionMode.class,
                    text(item.get("executionMode")),
                    ExecutionMode.MANUAL_CHECK)));
      } catch (IllegalArgumentException invalid) {
        // Skip only the offending action.
      }
    }
    return parsed;
  }

  private static <E extends Enum<E>> E enumValue(Class<E> type, String raw, E fallback) {
    if (raw == null || raw.isBlank()) {
      return fallback;
    }
    try {
      return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
    } catch (IllegalArgumentException unknown) {
      return fallback;
    }
  }

  private static double clampConfidence(Object raw) {
    double value = number(raw, 0.5);
    // Models sometimes answer in percent; 0..1 is the schema.
    if (value > 1.0 && value <= 100.0) {
      value = value / 100.0;
    }
    return Math.clamp(value, 0.0, 1.0);
  }

  private static double number(Object raw, double fallback) {
    if (raw instanceof Number value) {
      return value.doubleValue();
    }
    if (raw instanceof String text) {
      try {
        return Double.parseDouble(text.trim());
      } catch (NumberFormatException notANumber) {
        return fallback;
      }
    }
    return fallback;
  }

  private static String text(Object raw) {
    return raw == null ? null : raw.toString();
  }

  private static List<String> strings(Object raw) {
    if (!(raw instanceof List<?> items)) {
      return List.of();
    }
    return items.stream().filter(Objects::nonNull).map(Object::toString).toList();
  }
}
