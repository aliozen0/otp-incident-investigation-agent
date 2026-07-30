package com.example.otpsentinel.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.otpsentinel.domain.Hypothesis;
import com.example.otpsentinel.domain.InvestigationStatus;
import com.example.otpsentinel.domain.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class IncidentAnalysisResultTest {

  @Test
  void rejectsConfidenceOutsideZeroToOne() {
    assertThatThrownBy(
            () ->
                new IncidentAnalysisResult(
                    InvestigationStatus.ANOMALY_CONFIRMED,
                    Severity.HIGH,
                    "summary",
                    List.of(new EvidenceReference("ev-1")),
                    List.of(),
                    List.of(),
                    List.of(),
                    1.5))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsMoreThanThreeHypotheses() {
    Hypothesis h = new Hypothesis(1, "cause", 0.5, List.of("ev-1"), List.of(), List.of());
    assertThatThrownBy(
            () ->
                new IncidentAnalysisResult(
                    InvestigationStatus.ANOMALY_CONFIRMED,
                    Severity.HIGH,
                    "summary",
                    List.of(new EvidenceReference("ev-1")),
                    List.of(h, h, h, h),
                    List.of(),
                    List.of(),
                    0.8))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsValidResult() {
    Hypothesis h = new Hypothesis(1, "cause", 0.5, List.of("ev-1"), List.of(), List.of());
    IncidentAnalysisResult result =
        new IncidentAnalysisResult(
            InvestigationStatus.ANOMALY_CONFIRMED,
            Severity.HIGH,
            "summary",
            List.of(new EvidenceReference("ev-1")),
            List.of(h),
            List.of(),
            List.of(new KnowledgeReference("KB-1", "KB-1#v1#c0")),
            0.8);
    assertThat(result.confidence()).isEqualTo(0.8);
  }
}
