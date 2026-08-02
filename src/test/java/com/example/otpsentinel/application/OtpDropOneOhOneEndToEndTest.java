package com.example.otpsentinel.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.agent.AgentTools;
import com.example.otpsentinel.agent.EvidenceCollector;
import com.example.otpsentinel.agent.IncidentAnalysisAiService;
import com.example.otpsentinel.agent.ToolBudgetGuard;
import com.example.otpsentinel.agent.stub.OtpDropOneOhOneScript;
import com.example.otpsentinel.agent.stub.StubChatModel;
import com.example.otpsentinel.agent.stub.StubScript;
import com.example.otpsentinel.domain.Evidence;
import com.example.otpsentinel.domain.Hypothesis;
import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.domain.InvestigationPhase;
import com.example.otpsentinel.domain.InvestigationStatus;
import com.example.otpsentinel.domain.Severity;
import com.example.otpsentinel.domain.TimeWindow;
import com.example.otpsentinel.rag.KnowledgeSearchResult;
import com.example.otpsentinel.tools.fixtures.FixtureCatalog;
import com.example.otpsentinel.tools.fixtures.FixtureErrorDistributionTool;
import com.example.otpsentinel.tools.fixtures.FixtureId;
import com.example.otpsentinel.tools.fixtures.FixtureOtpMetricsTool;
import com.example.otpsentinel.tools.fixtures.FixtureProviderHealthTool;
import com.example.otpsentinel.tools.fixtures.FixtureQueueHealthTool;
import com.example.otpsentinel.tools.fixtures.FixtureRecentChangesTool;
import com.example.otpsentinel.tools.fixtures.FixtureScenario;
import dev.langchain4j.service.AiServices;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * M5 acceptance: OTP-DROP-001 traverses the real fixture tools, evidence mapping, RAG adapter seam,
 * structured-output service, and aggregate lifecycle using only the deterministic stub model.
 */
class OtpDropOneOhOneEndToEndTest {

  @Test
  void investigatesOtpDropWithExpectedToolOrderAndEvidenceBasedHypotheses() {
    TimeWindow window =
        new TimeWindow(
            Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z"));
    FixtureScenario scenario = FixtureCatalog.forFixture(FixtureId.OTP_DROP_001);
    Investigation investigation =
        Investigation.receive("why did OTP success rate drop", window, "v1", "v1");
    ToolBudgetGuard guard = new ToolBudgetGuard(8, Duration.ofSeconds(2), 1);
    EvidenceCollector collector = new EvidenceCollector(investigation);
    AgentTools tools =
        new AgentTools(
            new FixtureOtpMetricsTool(scenario),
            new FixtureErrorDistributionTool(scenario),
            new FixtureQueueHealthTool(scenario),
            new FixtureProviderHealthTool(scenario),
            new FixtureRecentChangesTool(scenario),
            (query, provider, topK) ->
                List.of(
                    new KnowledgeSearchResult(
                        "INC-2026-041",
                        "1",
                        "Connection pool exhaustion incident",
                        "INC-2026-041#v1#c0",
                        0.85,
                        "When active connections approach max and timeout rate rises, suspect connection pool exhaustion.")),
            guard,
            collector);
    StubScript script = OtpDropOneOhOneScript.build();
    IncidentAnalysisAiService aiService =
        AiServices.builder(IncidentAnalysisAiService.class)
            .chatModel(new StubChatModel(script))
            .tools(tools)
            .chatMemoryProvider(
                id -> dev.langchain4j.memory.chat.MessageWindowChatMemory.withMaxMessages(10))
            .build();

    Investigation outcome =
        new IncidentInvestigationService(1)
            .investigate(
                new InvestigationRequest("why did OTP success rate drop", window, "v1", "v1"),
                investigation,
                aiService,
                guard,
                collector);

    assertThat(guard.toolNames())
        .containsExactly(
            "getOtpMetrics",
            "getErrorDistribution",
            "getQueueHealth",
            "getProviderHealth",
            "getRecentChanges",
            "searchIncidentKnowledge");
    assertThat(guard.callCount()).isLessThanOrEqualTo(8);
    assertThat(guard.policyLimitReached()).isFalse();
    assertThat(outcome.phase()).isEqualTo(InvestigationPhase.COMPLETED);
    assertThat(outcome.resultStatus()).isEqualTo(InvestigationStatus.ANOMALY_CONFIRMED);
    assertThat(outcome.severity()).isEqualTo(Severity.HIGH);
    assertThat(outcome.confidence()).isBetween(0.80, 0.92);
    assertThat(metric(outcome, "ev-otp-success-rate-current")).isEqualTo(72.10);
    assertThat(metric(outcome, "ev-otp-success-rate-previous")).isEqualTo(98.10);
    assertThat(outcome.knowledgeReferences()).containsExactly("INC-2026-041");

    Hypothesis primary = outcome.hypotheses().getFirst();
    assertThat(primary.rank()).isEqualTo(1);
    assertThat(primary.probability()).isGreaterThanOrEqualTo(0.7);
    assertThat(primary.possibleCause()).containsIgnoringCase("OPERATOR_B");
    assertThat(primary.possibleCause()).containsIgnoringCase("connection pool");
    assertThat(primary.possibleCause()).doesNotContainIgnoringCase("queue");
    assertThat(primary.supportingEvidenceIds())
        .contains("ev-timeout-rate", "ev-connection-capacity");

    Hypothesis deploy =
        outcome.hypotheses().stream()
            .filter(hypothesis -> lower(hypothesis.possibleCause()).contains("deploy"))
            .findFirst()
            .orElseThrow();
    assertThat(deploy.possibleCause()).contains("v2.4");
    assertThat(lower(deploy.possibleCause())).contains("correlat");
    assertThat(lower(deploy.possibleCause())).contains("not a confirmed cause");
    assertThat(lower(deploy.possibleCause())).doesNotContain("caused", "neden oldu");
  }

  private static double metric(Investigation investigation, String evidenceId) {
    return investigation.evidence().stream()
        .filter(evidence -> evidence.id().equals(evidenceId))
        .map(Evidence::metricValue)
        .findFirst()
        .orElseThrow();
  }

  private static String lower(String value) {
    return value.toLowerCase(Locale.ROOT);
  }
}
