package com.example.otpsentinel.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.agent.stub.StubChatModel;
import com.example.otpsentinel.agent.stub.StubScript;
import com.example.otpsentinel.agent.stub.StubScriptStep;
import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.domain.InvestigationStatus;
import com.example.otpsentinel.domain.TimeWindow;
import com.example.otpsentinel.tools.fixtures.FixtureCatalog;
import com.example.otpsentinel.tools.fixtures.FixtureErrorDistributionTool;
import com.example.otpsentinel.tools.fixtures.FixtureId;
import com.example.otpsentinel.tools.fixtures.FixtureOtpMetricsTool;
import com.example.otpsentinel.tools.fixtures.FixtureProviderHealthTool;
import com.example.otpsentinel.tools.fixtures.FixtureQueueHealthTool;
import com.example.otpsentinel.tools.fixtures.FixtureRecentChangesTool;
import dev.langchain4j.service.AiServices;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IncidentAnalysisAiServiceStubTest {

  @Test
  void wiresStubModelAndToolsAndReturnsStructuredResult() {
    StubScript script =
        new StubScript(
            List.of(
                StubScriptStep.callTools(
                    new StubScriptStep.PlannedToolCall("getQueueHealth", Map.of())),
                StubScriptStep.finalAnswer(
                    """
                    {"status":"NO_ANOMALY","severity":"LOW","summary":"queue is healthy",
                     "evidence":[{"evidenceId":"ev-queue-health"}],"hypotheses":[],
                     "recommendedActions":[],"knowledgeReferences":[],"confidence":0.9}
                    """)));
    StubChatModel stubChatModel = new StubChatModel(script);
    var scenario = FixtureCatalog.forFixture(FixtureId.OTP_NORMAL_001);
    Investigation investigation =
        Investigation.receive(
            "is anything wrong",
            new TimeWindow(
                Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z")),
            "v1",
            "v1");
    investigation.startCollectingEvidence();
    ToolBudgetGuard guard = new ToolBudgetGuard(8, Duration.ofSeconds(2), 1);
    EvidenceCollector collector = new EvidenceCollector(investigation);
    AgentTools tools =
        new AgentTools(
            new FixtureOtpMetricsTool(scenario),
            new FixtureErrorDistributionTool(scenario),
            new FixtureQueueHealthTool(scenario),
            new FixtureProviderHealthTool(scenario),
            new FixtureRecentChangesTool(scenario),
            (query, provider, topK) -> List.of(),
            guard,
            collector);

    IncidentAnalysisAiService service =
        AiServices.builder(IncidentAnalysisAiService.class)
            .chatModel(stubChatModel)
            .tools(tools)
            .build();

    IncidentAnalysisResult result =
        service.analyze("is anything wrong", "2026-07-30T11:15Z/11:30Z");

    assertThat(result.status()).isEqualTo(InvestigationStatus.NO_ANOMALY);
    assertThat(result.evidence()).containsExactly(new EvidenceReference("ev-queue-health"));
  }
}
