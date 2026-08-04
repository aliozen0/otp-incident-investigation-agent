package com.example.otpsentinel.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.agent.VisualizationProposal;
import com.example.otpsentinel.domain.Evidence;
import com.example.otpsentinel.domain.VisualizationType;
import com.example.otpsentinel.domain.VisualizationUnit;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class VisualizationValidatorTest {

  private final VisualizationValidator validator = new VisualizationValidator();
  private final List<Evidence> evidence =
      List.of(
          metric("ev-current", 72.1, "percent"),
          metric("ev-timeout", 0.31, "ratio"),
          prose("ev-prose"));

  @Test
  void acceptsKnownEvidenceAndDeterministicRatioToPercentConversion() {
    VisualizationProposal proposal =
        proposal(
            "GROUPED_BAR",
            "PERCENT",
            List.of(
                new VisualizationProposal.Point("Current", "rate", 72.1, "ev-current"),
                new VisualizationProposal.Point("Timeout", "rate", 31.0, "ev-timeout")));

    VisualizationValidationResult result = validator.validate(List.of(proposal), evidence);

    assertThat(result.accepted()).hasSize(1);
    assertThat(result.accepted().getFirst().type()).isEqualTo(VisualizationType.GROUPED_BAR);
    assertThat(result.accepted().getFirst().unit()).isEqualTo(VisualizationUnit.PERCENT);
    assertThat(result.warnings()).isEmpty();
  }

  @Test
  void dropsUnknownEvidenceFabricatedValueProseAndUnknownType() {
    List<VisualizationProposal> proposals =
        List.of(
            proposal(
                "BAR",
                "PERCENT",
                List.of(new VisualizationProposal.Point("X", "rate", 85.0, "ev-current"))),
            proposal(
                "BAR",
                "PERCENT",
                List.of(new VisualizationProposal.Point("X", "rate", 72.1, "ev-unknown"))),
            proposal(
                "BAR",
                "COUNT",
                List.of(new VisualizationProposal.Point("X", "rate", 1.0, "ev-prose"))),
            proposal(
                "SCRIPT",
                "PERCENT",
                List.of(new VisualizationProposal.Point("X", "rate", 72.1, "ev-current"))));

    VisualizationValidationResult result = validator.validate(proposals, evidence);

    assertThat(result.accepted()).isEmpty();
    assertThat(result.warnings()).hasSize(4).allMatch(w -> w.startsWith("VISUALIZATION_REJECTED"));
  }

  @Test
  void keepsTheAnalysisWhenTheModelSendsATextualPointValue() {
    // Regression: a live model put a TABLE cell string in "value". A strict double component made
    // Jackson reject the entire IncidentAnalysisResult, so the whole investigation failed.
    VisualizationProposal.Point textual =
        VisualizationProposal.Point.of("chg-101", "rate", "Retry count changed from 3 to 2", "ev-current");
    VisualizationProposal.Point numericText =
        VisualizationProposal.Point.of("Current", "rate", "72.1", "ev-current");

    assertThat(textual.value()).isNull();
    assertThat(numericText.value()).isEqualTo(72.1);

    VisualizationValidationResult result =
        validator.validate(List.of(proposal("TABLE", "NONE", List.of(textual))), evidence);

    assertThat(result.accepted()).isEmpty();
    assertThat(result.warnings())
        .singleElement()
        .asString()
        .contains("point value must be a finite number");
  }

  @Test
  void acceptsHumanWrittenUnitSynonyms() {
    VisualizationValidationResult result =
        validator.validate(
            List.of(
                proposal(
                    "line",
                    "%",
                    List.of(new VisualizationProposal.Point("Current", "rate", 72.1, "ev-current")))),
            evidence);

    assertThat(result.accepted()).singleElement().satisfies(spec -> {
      assertThat(spec.type()).isEqualTo(VisualizationType.LINE);
      assertThat(spec.unit()).isEqualTo(VisualizationUnit.PERCENT);
    });
  }

  private static VisualizationProposal proposal(
      String type, String unit, List<VisualizationProposal.Point> points) {
    return new VisualizationProposal(
        "chart",
        type,
        "Comparison",
        null,
        null,
        unit,
        List.of(new VisualizationProposal.Series("rate", "Rate")),
        points);
  }

  private static Evidence metric(String id, double value, String unit) {
    return new Evidence(id, "TOOL_RESULT", "tool", "metric", Instant.EPOCH, "metric", value, unit);
  }

  private static Evidence prose(String id) {
    return new Evidence(id, "TOOL_RESULT", "tool", "prose", Instant.EPOCH, null, null, null);
  }
}
