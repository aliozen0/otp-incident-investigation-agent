package com.example.otpsentinel.application;

import com.example.otpsentinel.agent.EvidenceReference;
import com.example.otpsentinel.agent.IncidentAnalysisResult;
import com.example.otpsentinel.domain.ActionType;
import com.example.otpsentinel.domain.Evidence;
import com.example.otpsentinel.domain.Hypothesis;
import com.example.otpsentinel.domain.RecommendedAction;
import com.example.otpsentinel.domain.ValidationReport;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Deterministic checks from docs/07 "Validation pipeline" steps 3, 6, 7 and 8 (evidence existence,
 * numeric claim source, forbidden automatic action, correlation wording), plus the docs/09 PII
 * scan. Runs after the model returns structured output and before it is handed to the {@link
 * com.example.otpsentinel.domain.Investigation} aggregate, which still enforces its own invariants
 * (steps 4/5/9) as the last safety net.
 */
public final class ClaimValidator {

  private static final Set<ActionType> RESTRICTED_ACTIONS =
      EnumSet.of(ActionType.RESTART, ActionType.ROLLBACK, ActionType.CONFIG_CHANGE);

  private static final Pattern PERCENT_CLAIM =
      Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:%|percent)", Pattern.CASE_INSENSITIVE);

  private static final List<Pattern> CAUSATION_PATTERNS =
      List.of(
          Pattern.compile("\\bcaused\\b", Pattern.CASE_INSENSITIVE),
          Pattern.compile("\\bis the root cause\\b", Pattern.CASE_INSENSITIVE),
          Pattern.compile("neden oldu", Pattern.CASE_INSENSITIVE));

  private final PiiScanner piiScanner = new PiiScanner();

  public ValidationReport validate(IncidentAnalysisResult analysis, List<Evidence> knownEvidence) {
    Optional<String> unknownEvidence = unknownEvidenceViolation(analysis, knownEvidence);
    if (unknownEvidence.isPresent()) {
      return ValidationReport.failed(
          List.of(
              "UNKNOWN_EVIDENCE_REFERENCE: evidence id "
                  + unknownEvidence.get()
                  + " was never collected"));
    }

    Optional<String> numericClaim = numericClaimViolation(analysis, knownEvidence);
    if (numericClaim.isPresent()) {
      return ValidationReport.failed(List.of("UNSUPPORTED_NUMERIC_CLAIM: " + numericClaim.get()));
    }

    Optional<RecommendedAction> forbiddenAction = forbiddenActionViolation(analysis);
    if (forbiddenAction.isPresent()) {
      return ValidationReport.failed(
          List.of(
              "FORBIDDEN_AUTOMATIC_ACTION: "
                  + forbiddenAction.get().actionType()
                  + " without approval"));
    }

    Optional<String> pii = piiViolation(analysis);
    if (pii.isPresent()) {
      return ValidationReport.failed(List.of("PII_DETECTED: " + pii.get()));
    }

    List<String> warnings = new ArrayList<>();
    if (hasCausationLanguage(analysis)) {
      warnings.add(
          "CAUSATION_LANGUAGE_DETECTED: wording implies definite causation instead of"
              + " correlation");
    }
    return ValidationReport.passed(warnings);
  }

  private static Optional<String> unknownEvidenceViolation(
      IncidentAnalysisResult analysis, List<Evidence> knownEvidence) {
    Set<String> known = new HashSet<>(knownEvidence.stream().map(Evidence::id).toList());
    Stream<String> resultCitations =
        analysis.evidence().stream().map(EvidenceReference::evidenceId);
    Stream<String> hypothesisCitations =
        analysis.hypotheses().stream()
            .flatMap(
                hypothesis ->
                    Stream.concat(
                        hypothesis.supportingEvidenceIds().stream(),
                        hypothesis.contradictingEvidenceIds().stream()));
    return Stream.concat(resultCitations, hypothesisCitations)
        .filter(id -> !known.contains(id))
        .findFirst();
  }

  private static Optional<String> numericClaimViolation(
      IncidentAnalysisResult analysis, List<Evidence> knownEvidence) {
    Set<Double> knownPercentValues = new HashSet<>();
    for (Evidence evidence : knownEvidence) {
      if (evidence.metricValue() == null) {
        continue;
      }
      knownPercentValues.add(round1(evidence.metricValue()));
      knownPercentValues.add(round1(evidence.metricValue() * 100));
    }
    for (String text : claimTexts(analysis)) {
      Matcher matcher = PERCENT_CLAIM.matcher(text);
      while (matcher.find()) {
        double claimed = round1(Double.parseDouble(matcher.group(1)));
        boolean supported =
            knownPercentValues.stream().anyMatch(known -> Math.abs(known - claimed) < 0.15);
        if (!supported) {
          return Optional.of(matcher.group().trim());
        }
      }
    }
    return Optional.empty();
  }

  private static Optional<RecommendedAction> forbiddenActionViolation(
      IncidentAnalysisResult analysis) {
    return analysis.recommendedActions().stream()
        .filter(
            action ->
                RESTRICTED_ACTIONS.contains(action.actionType()) && !action.requiresApproval())
        .findFirst();
  }

  private Optional<String> piiViolation(IncidentAnalysisResult analysis) {
    for (String text : claimTexts(analysis)) {
      Optional<String> hit = piiScanner.scan(text);
      if (hit.isPresent()) {
        return hit;
      }
    }
    return Optional.empty();
  }

  private static boolean hasCausationLanguage(IncidentAnalysisResult analysis) {
    return claimTexts(analysis).stream()
        .anyMatch(text -> CAUSATION_PATTERNS.stream().anyMatch(p -> p.matcher(text).find()));
  }

  private static List<String> claimTexts(IncidentAnalysisResult analysis) {
    List<String> texts = new ArrayList<>();
    texts.add(analysis.summary());
    for (Hypothesis hypothesis : analysis.hypotheses()) {
      texts.add(hypothesis.possibleCause());
      texts.addAll(hypothesis.verificationSteps());
    }
    return texts;
  }

  private static double round1(double value) {
    return Math.round(value * 10) / 10.0;
  }
}
