package com.example.otpsentinel.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.domain.Evidence;
import com.example.otpsentinel.domain.Hypothesis;
import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.domain.InvestigationId;
import com.example.otpsentinel.domain.InvestigationPhase;
import com.example.otpsentinel.domain.InvestigationStatus;
import com.example.otpsentinel.domain.KnowledgeCitation;
import com.example.otpsentinel.domain.Severity;
import com.example.otpsentinel.domain.TimeWindow;
import com.example.otpsentinel.domain.ValidationReport;
import com.example.otpsentinel.domain.VisualizationSpec;
import com.example.otpsentinel.domain.VisualizationType;
import com.example.otpsentinel.domain.VisualizationUnit;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** FR-016/AC-030: a persisted Investigation is re-fetched, byte-for-byte, after a "restart". */
class JdbcInvestigationRepositoryTest extends AbstractPostgresIntegrationTest {

  private static final Instant BASE = Instant.parse("2026-07-30T11:15:00Z");
  private static final TimeWindow WINDOW = new TimeWindow(BASE, BASE.plusSeconds(900));

  @Test
  void survivesRestartAtEveryLifecyclePhase() {
    Investigation investigation =
        Investigation.receive("Why did OTP delivery drop?", WINDOW, "v1", "v1");
    newInvestigationRepository().save(investigation);

    investigation.startCollectingEvidence();
    investigation.addEvidence(metricEvidence("ev-1", "successRate.current"));
    investigation.addEvidence(metricEvidence("ev-2", "successRate.previous"));
    newInvestigationRepository().save(investigation);

    investigation.startGeneratingAnalysis();
    Hypothesis hypothesis =
        new Hypothesis(
            1,
            "Connection pool exhaustion",
            0.85,
            List.of("ev-1", "ev-2"),
            List.of(),
            List.of("Inspect pool metrics"));
    KnowledgeCitation citation =
        new KnowledgeCitation(
            "INC-2026-041", "1", "Connection pool incident", "INC-2026-041#v1#c0", 0.85);
    VisualizationSpec visualization =
        new VisualizationSpec(
            "success-comparison",
            VisualizationType.BAR,
            "Başarı karşılaştırması",
            "Dönem",
            "Başarı",
            VisualizationUnit.PERCENT,
            List.of(new VisualizationSpec.Series("success", "Başarı")),
            List.of(
                new VisualizationSpec.Point("Mevcut", "success", 72.1, "ev-1"),
                new VisualizationSpec.Point("Önceki", "success", 72.1, "ev-2")));
    investigation.proposeAnalysis(
        "OTP başarısı düştü ve Operatör B üzerinde yoğunlaştı.",
        Severity.HIGH,
        List.of(hypothesis),
        List.of(),
        List.of(citation),
        0.87,
        List.of(visualization));
    investigation.startValidating();
    investigation.complete(
        InvestigationStatus.ANOMALY_CONFIRMED, ValidationReport.passed(List.of("minor warning")));
    newInvestigationRepository().save(investigation);

    Optional<Investigation> reloaded = newInvestigationRepository().findById(investigation.id());

    assertThat(reloaded).isPresent();
    Investigation restarted = reloaded.get();
    assertThat(restarted.id()).isEqualTo(investigation.id());
    assertThat(restarted.question()).isEqualTo(investigation.question());
    assertThat(restarted.resolvedTimeWindow()).isEqualTo(investigation.resolvedTimeWindow());
    assertThat(restarted.phase()).isEqualTo(InvestigationPhase.COMPLETED);
    assertThat(restarted.resultStatus()).isEqualTo(InvestigationStatus.ANOMALY_CONFIRMED);
    assertThat(restarted.severity()).isEqualTo(Severity.HIGH);
    assertThat(restarted.confidence()).isEqualTo(0.87);
    assertThat(restarted.summary())
        .isEqualTo("OTP başarısı düştü ve Operatör B üzerinde yoğunlaştı.");
    assertThat(restarted.knowledgeCitations()).containsExactly(citation);
    assertThat(restarted.evidence()).containsExactlyElementsOf(investigation.evidence());
    assertThat(restarted.hypotheses()).containsExactly(hypothesis);
    assertThat(restarted.validationReport()).isEqualTo(investigation.validationReport());
    assertThat(restarted.visualizations()).containsExactly(visualization);
  }

  @Test
  void findBySessionIdReturnsOnlyThatSessionsInvestigationsInChronologicalOrder() {
    JdbcInvestigationRepository repository = newInvestigationRepository();
    Investigation first = Investigation.receive("first question", WINDOW, "v1", "v1", "thread-A");
    Investigation second = Investigation.receive("second question", WINDOW, "v1", "v1", "thread-A");
    Investigation other =
        Investigation.receive("unrelated question", WINDOW, "v1", "v1", "thread-B");
    repository.save(first);
    repository.save(second);
    repository.save(other);

    List<Investigation> threadA = repository.findBySessionId("thread-A");

    assertThat(threadA).extracting(Investigation::id).containsExactly(first.id(), second.id());
  }

  @Test
  void findByIdReturnsEmptyForUnknownId() {
    Optional<Investigation> reloaded =
        newInvestigationRepository().findById(InvestigationId.generate());

    assertThat(reloaded).isEmpty();
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
}
