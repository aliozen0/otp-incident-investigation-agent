package com.example.otpsentinel.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import com.example.otpsentinel.agent.stub.OtpDropOneOhOneScript;
import com.example.otpsentinel.agent.stub.StubChatModel;
import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.domain.InvestigationPhase;
import com.example.otpsentinel.domain.InvestigationStatus;
import com.example.otpsentinel.domain.TimeWindow;
import com.example.otpsentinel.rag.fixtures.FixtureKnowledgeSearchPort;
import com.example.otpsentinel.tools.fixtures.FixtureCatalog;
import com.example.otpsentinel.tools.fixtures.FixtureErrorDistributionTool;
import com.example.otpsentinel.tools.fixtures.FixtureId;
import com.example.otpsentinel.tools.fixtures.FixtureOtpMetricsTool;
import com.example.otpsentinel.tools.fixtures.FixtureProviderHealthTool;
import com.example.otpsentinel.tools.fixtures.FixtureQueueHealthTool;
import com.example.otpsentinel.tools.fixtures.FixtureRecentChangesTool;
import com.example.otpsentinel.tools.fixtures.FixtureScenario;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InvestigationOrchestratorTest extends AbstractPostgresIntegrationTest {

  @Test
  void runsAndPersistsTheOtpDropOneOhOneFixture() {
    FixtureScenario scenario = FixtureCatalog.forFixture(FixtureId.OTP_DROP_001);
    InvestigationOrchestrator orchestrator =
        new InvestigationOrchestrator(
            newInvestigationRepository(),
            newIncidentDraftRepository(),
            newAuditEventRepository(),
            new StubChatModel(OtpDropOneOhOneScript.build()),
            new FixtureKnowledgeSearchPort(),
            new FixtureOtpMetricsTool(scenario),
            new FixtureErrorDistributionTool(scenario),
            new FixtureQueueHealthTool(scenario),
            new FixtureProviderHealthTool(scenario),
            new FixtureRecentChangesTool(scenario),
            8,
            2000,
            1,
            1);

    TimeWindow window =
        new TimeWindow(
            Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z"));
    Investigation outcome =
        orchestrator.runInvestigation("why did OTP success rate drop", window, "corr-orch-1");

    assertThat(outcome.phase()).isEqualTo(InvestigationPhase.COMPLETED);
    assertThat(outcome.resultStatus()).isEqualTo(InvestigationStatus.ANOMALY_CONFIRMED);
    assertThat(orchestrator.findInvestigation(outcome.id())).isPresent();
  }
}
