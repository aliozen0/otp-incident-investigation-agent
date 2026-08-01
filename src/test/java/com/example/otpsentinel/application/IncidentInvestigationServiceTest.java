package com.example.otpsentinel.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.otpsentinel.agent.AgentTools;
import com.example.otpsentinel.agent.EvidenceCollector;
import com.example.otpsentinel.agent.IncidentAnalysisAiService;
import com.example.otpsentinel.agent.ToolBudgetGuard;
import com.example.otpsentinel.agent.stub.StubChatModel;
import com.example.otpsentinel.agent.stub.StubScript;
import com.example.otpsentinel.agent.stub.StubScriptStep;
import com.example.otpsentinel.domain.AuditEvent;
import com.example.otpsentinel.domain.AuditEventRepository;
import com.example.otpsentinel.domain.AuditEventType;
import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.domain.InvestigationId;
import com.example.otpsentinel.domain.InvestigationPhase;
import com.example.otpsentinel.domain.InvestigationStatus;
import com.example.otpsentinel.domain.TimeWindow;
import com.example.otpsentinel.rag.KnowledgeSearchPort;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IncidentInvestigationServiceTest {

  @Test
  void drivesLifecycleAndRepairsOnceOnInvalidJson() {
    TestContext context =
        context(
            8,
            (query, provider, topK) -> List.of(),
            StubScriptStep.callTools(toolCall("getQueueHealth", Map.of())),
            StubScriptStep.finalAnswer("not json"),
            StubScriptStep.finalAnswer(noAnomalyJson()));

    Investigation outcome = investigate(context);

    assertThat(outcome.phase()).isEqualTo(InvestigationPhase.COMPLETED);
    assertThat(outcome.resultStatus()).isEqualTo(InvestigationStatus.NO_ANOMALY);
    assertThat(outcome.toolExecutions()).hasSize(1);
  }

  @Test
  void failsAfterSecondInvalidStructuredOutput() {
    TestContext context =
        context(
            8,
            (query, provider, topK) -> List.of(),
            StubScriptStep.finalAnswer("not json"),
            StubScriptStep.finalAnswer("still not json"));

    Investigation outcome = investigate(context);

    assertThat(outcome.phase()).isEqualTo(InvestigationPhase.FAILED);
    assertThat(outcome.resultStatus()).isEqualTo(InvestigationStatus.FAILED);
    assertThat(outcome.validationReport().warnings().getFirst())
        .contains("invalid after 1 repair attempt");
  }

  @Test
  void toolBudgetExceededYieldsPartialWithoutUsingRepairAttempt() {
    TestContext context =
        context(
            1,
            (query, provider, topK) -> List.of(),
            StubScriptStep.callTools(toolCall("getQueueHealth", Map.of())),
            StubScriptStep.callTools(
                toolCall(
                    "searchIncidentKnowledge",
                    Map.of("query", "connection pool", "providerFilter", "OPERATOR_B", "topK", 5))),
            StubScriptStep.finalAnswer(noAnomalyJson()));

    Investigation outcome = investigate(context);

    assertThat(outcome.phase()).isEqualTo(InvestigationPhase.PARTIAL);
    assertThat(outcome.resultStatus()).isEqualTo(InvestigationStatus.PARTIAL_ANALYSIS);
    assertThat(context.guard().callCount()).isEqualTo(1);
  }

  @Test
  void hallucinatedEvidenceIdIsRejectedAsFailure() {
    String hallucinated =
        """
        {"status":"ANOMALY_CONFIRMED","severity":"HIGH","summary":"unsupported",
         "evidence":[{"evidenceId":"ev-does-not-exist"}],
         "hypotheses":[{"rank":1,"possibleCause":"unsupported cause","probability":0.5,
          "supportingEvidenceIds":["ev-does-not-exist"],"contradictingEvidenceIds":[],
          "verificationSteps":[]}],"recommendedActions":[],"knowledgeReferences":[],"confidence":0.5}
        """;
    TestContext context =
        context(
            8,
            (query, provider, topK) -> List.of(),
            StubScriptStep.callTools(toolCall("getQueueHealth", Map.of())),
            StubScriptStep.finalAnswer(hallucinated));

    Investigation outcome = investigate(context);

    assertThat(outcome.phase()).isEqualTo(InvestigationPhase.FAILED);
    assertThat(outcome.validationReport().warnings().getFirst()).contains("never collected");
  }

  @Test
  void dropsKnowledgeReferenceThatWasNotReturnedBySearch() {
    KnowledgeSearchPort knowledge =
        (query, provider, topK) ->
            List.of(
                new KnowledgeSearchResult(
                    "KB-1", "1", "Pool runbook", "KB-1#v1#c0", 0.82, "reference data"));
    String answer =
        """
        {"status":"NO_ANOMALY","severity":"LOW","summary":"queue is healthy",
         "evidence":[{"evidenceId":"ev-queue-health"}],"hypotheses":[],
         "recommendedActions":[],"knowledgeReferences":[
          {"documentId":"KB-1","chunkId":"KB-1#v1#c0"},
          {"documentId":"KB-FAKE","chunkId":"KB-FAKE#v1#c0"}],"confidence":0.9}
        """;
    TestContext context =
        context(
            8,
            knowledge,
            StubScriptStep.callTools(toolCall("getQueueHealth", Map.of())),
            StubScriptStep.callTools(
                toolCall(
                    "searchIncidentKnowledge",
                    Map.of("query", "connection pool", "providerFilter", "OPERATOR_B", "topK", 5))),
            StubScriptStep.finalAnswer(answer));

    Investigation outcome = investigate(context);

    assertThat(outcome.phase()).isEqualTo(InvestigationPhase.COMPLETED);
    assertThat(outcome.knowledgeReferences()).containsExactly("KB-1");
  }

  @Test
  void forbiddenAutomaticActionIsRejectedAsFailure() {
    String autoRollback =
        """
        {"status":"ANOMALY_CONFIRMED","severity":"MEDIUM","summary":"queue is degraded",
         "evidence":[{"evidenceId":"ev-queue-health"}],"hypotheses":[
          {"rank":1,"possibleCause":"queue backlog","probability":0.6,
           "supportingEvidenceIds":["ev-queue-health"],"contradictingEvidenceIds":[],
           "verificationSteps":[]}],
         "recommendedActions":[{"actionType":"ROLLBACK","description":"roll back now",
           "risk":"MEDIUM","requiresApproval":false,"executionMode":"MANUAL_CHECK"}],
         "knowledgeReferences":[],"confidence":0.6}
        """;
    TestContext context =
        context(
            8,
            (query, provider, topK) -> List.of(),
            StubScriptStep.callTools(toolCall("getQueueHealth", Map.of())),
            StubScriptStep.finalAnswer(autoRollback));

    Investigation outcome = investigate(context);

    assertThat(outcome.phase()).isEqualTo(InvestigationPhase.FAILED);
    assertThat(outcome.validationReport().warnings().getFirst())
        .contains("FORBIDDEN_AUTOMATIC_ACTION");
  }

  @Test
  void auditsLlmCompletedAndValidationPassedOnSuccessfulCompletion() {
    List<AuditEvent> captured = new ArrayList<>();
    AuditEventRepository auditRepo =
        new AuditEventRepository() {
          public void append(AuditEvent event) {
            captured.add(event);
          }

          public List<AuditEvent> findByInvestigationId(InvestigationId id) {
            return List.of();
          }
        };
    Investigation investigation = Investigation.receive("q", window(), "v1", "v1");
    ToolBudgetGuard guard = new ToolBudgetGuard(8, Duration.ofSeconds(2), 1);
    EvidenceCollector collector = new EvidenceCollector(investigation, auditRepo, "corr-4");
    FixtureScenario scenario = FixtureCatalog.forFixture(FixtureId.OTP_NORMAL_001);
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
    IncidentAnalysisAiService aiService =
        AiServices.builder(IncidentAnalysisAiService.class)
            .chatModel(
                new StubChatModel(
                    new StubScript(
                        List.of(
                            StubScriptStep.callTools(toolCall("getQueueHealth", Map.of())),
                            StubScriptStep.finalAnswer(noAnomalyJson())))))
            .tools(tools)
            .build();

    Investigation outcome =
        new IncidentInvestigationService(1)
            .investigate(
                new InvestigationRequest("q", window(), "v1", "v1"),
                investigation,
                aiService,
                guard,
                collector,
                auditRepo,
                "corr-4");

    assertThat(outcome.phase()).isEqualTo(InvestigationPhase.COMPLETED);
    assertThat(captured)
        .extracting(AuditEvent::action)
        .contains(AuditEventType.LLM_COMPLETED, AuditEventType.VALIDATION_PASSED);
  }

  @Test
  void requestRejectsBlankQuestion() {
    assertThatThrownBy(() -> new InvestigationRequest(" ", window(), "v1", "v1"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void mismatchedRequestIsAnInternalWiringBugNotAClientError() {
    TestContext ctx =
        context(
            8, (query, provider, topK) -> List.of(), StubScriptStep.finalAnswer(noAnomalyJson()));
    IncidentInvestigationService service = new IncidentInvestigationService(1);
    InvestigationRequest mismatched =
        new InvestigationRequest("a different question", window(), "v1", "v1");
    assertThatThrownBy(
            () ->
                service.investigate(
                    mismatched, ctx.investigation(), ctx.aiService(), ctx.guard(), ctx.collector()))
        .isInstanceOf(IllegalStateException.class)
        .isNotInstanceOf(IllegalArgumentException.class);
  }

  private static Investigation investigate(TestContext context) {
    IncidentInvestigationService service = new IncidentInvestigationService(1);
    return service.investigate(
        new InvestigationRequest("q", window(), "v1", "v1"),
        context.investigation(),
        context.aiService(),
        context.guard(),
        context.collector());
  }

  private static TestContext context(
      int maxCalls, KnowledgeSearchPort knowledge, StubScriptStep... steps) {
    Investigation investigation = Investigation.receive("q", window(), "v1", "v1");
    ToolBudgetGuard guard = new ToolBudgetGuard(maxCalls, Duration.ofSeconds(2), 1);
    EvidenceCollector collector = new EvidenceCollector(investigation);
    FixtureScenario scenario = FixtureCatalog.forFixture(FixtureId.OTP_NORMAL_001);
    AgentTools tools =
        new AgentTools(
            new FixtureOtpMetricsTool(scenario),
            new FixtureErrorDistributionTool(scenario),
            new FixtureQueueHealthTool(scenario),
            new FixtureProviderHealthTool(scenario),
            new FixtureRecentChangesTool(scenario),
            knowledge,
            guard,
            collector);
    IncidentAnalysisAiService aiService =
        AiServices.builder(IncidentAnalysisAiService.class)
            .chatModel(new StubChatModel(new StubScript(List.of(steps))))
            .tools(tools)
            .build();
    return new TestContext(investigation, guard, collector, aiService);
  }

  private static StubScriptStep.PlannedToolCall toolCall(
      String toolName, Map<String, Object> arguments) {
    return new StubScriptStep.PlannedToolCall(toolName, arguments);
  }

  private static String noAnomalyJson() {
    return """
        {"status":"NO_ANOMALY","severity":"LOW","summary":"queue is healthy",
         "evidence":[{"evidenceId":"ev-queue-health"}],"hypotheses":[],
         "recommendedActions":[],"knowledgeReferences":[],"confidence":0.9}
        """;
  }

  private static TimeWindow window() {
    return new TimeWindow(
        Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z"));
  }

  private record TestContext(
      Investigation investigation,
      ToolBudgetGuard guard,
      EvidenceCollector collector,
      IncidentAnalysisAiService aiService) {}
}
