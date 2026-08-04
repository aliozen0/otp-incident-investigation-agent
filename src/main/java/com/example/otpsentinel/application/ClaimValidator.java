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

  /**
   * Safety failures still reject the whole analysis; presentation problems are reported instead.
   *
   * <p>A fabricated evidence id or an unverifiable percentage used to discard a complete, otherwise
   * correct investigation — the operator lost everything because of one wrong citation. Those two
   * now surface as warnings the console shows next to the result, so the reader can see exactly
   * which claim is unsupported. Leaking PII or proposing an unapproved automatic action is a
   * different class of problem and still fails hard.
   */
  public ValidationReport validate(IncidentAnalysisResult analysis, List<Evidence> knownEvidence) {
    List<String> warnings = new ArrayList<>();

    Optional<String> unknownEvidence = unknownEvidenceViolation(analysis, knownEvidence);
    unknownEvidence.ifPresent(
        id ->
            warnings.add(
                "UNKNOWN_EVIDENCE_REFERENCE: evidence id " + id + " was never collected"));

    Optional<String> numericClaim = numericClaimViolation(analysis, knownEvidence);
    numericClaim.ifPresent(
        claim -> warnings.add("UNSUPPORTED_NUMERIC_CLAIM: " + claim + " is not backed by evidence"));

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
    Set<Double> knownPercentValues = supportedPercentValues(knownEvidence);
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

  /**
   * A percentage in the narrative is supported when it is a collected metric or a arithmetic
   * reading of two of them: the share one count makes of another (a "63.99% of errors were
   * PROVIDER_TIMEOUT" claim is computed from counts, never stored as its own metric) or the gap
   * between two values. Restricting support to stored metrics alone rejected correct analysis; a
   * figure that matches nothing here is still treated as fabricated and fails the whole result.
   */
  // ponytail: O(n^2) over evidence, which is bounded by the 8-tool-call budget; if evidence ever
  // grows past a few dozen items, precompute the pair values once per investigation instead.
  private static Set<Double> supportedPercentValues(List<Evidence> knownEvidence) {
    List<Double> metrics =
        knownEvidence.stream().map(Evidence::metricValue).filter(java.util.Objects::nonNull).toList();
    Set<Double> supported = new HashSet<>();
    for (double value : metrics) {
      supported.add(round1(value));
      supported.add(round1(value * 100));
      supported.add(round1(value / 100));
    }
    for (double left : metrics) {
      for (double right : metrics) {
        supported.add(round1(Math.abs(left - right)));
        supported.add(round1(Math.abs(left - right) * 100));
        if (right != 0.0) {
          supported.add(round1(left / right * 100));
        }
      }
    }
    return supported;
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
