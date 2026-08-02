package com.example.otpsentinel.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.agent.stub.StubChatModel;
import com.example.otpsentinel.agent.stub.StubScript;
import com.example.otpsentinel.agent.stub.StubScriptStep;
import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.domain.TimeWindow;
import com.example.otpsentinel.tools.fixtures.FixtureCatalog;
import com.example.otpsentinel.tools.fixtures.FixtureErrorDistributionTool;
import com.example.otpsentinel.tools.fixtures.FixtureId;
import com.example.otpsentinel.tools.fixtures.FixtureOtpMetricsTool;
import com.example.otpsentinel.tools.fixtures.FixtureProviderHealthTool;
import com.example.otpsentinel.tools.fixtures.FixtureQueueHealthTool;
import com.example.otpsentinel.tools.fixtures.FixtureRecentChangesTool;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.service.AiServices;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * M11 session-memory acceptance test (docs/16 ADR-017): drives the real {@code AiServices}-built
 * {@link IncidentAnalysisAiService} proxy — {@code @MemoryId} and all — against a shared {@link
 * SessionChatMemoryStore}, and proves the second call with the SAME session id actually sees the
 * first call's turn in the model request, while a different session id does not.
 */
class SessionMemoryAiServiceTest {

  private static final String FIRST_QUESTION = "why did OTP success rate drop at 11:15";
  private static final String SECOND_QUESTION = "and what about the queue depth";

  private static final String ANSWER =
      """
      {"status":"NO_ANOMALY","severity":"LOW","summary":"nothing anomalous",
       "evidence":[],"hypotheses":[],"recommendedActions":[],
       "knowledgeReferences":[],"confidence":0.9}
      """;

  @Test
  void secondCallWithSameSessionIdSeesTheFirstTurnAndAnotherSessionDoesNot() {
    StubChatModel stubChatModel =
        new StubChatModel(
            new StubScript(
                List.of(StubScriptStep.finalAnswer(ANSWER), StubScriptStep.finalAnswer(ANSWER))));
    SessionChatMemoryStore sharedStore = new SessionChatMemoryStore(40);
    IncidentAnalysisAiService service =
        AiServices.builder(IncidentAnalysisAiService.class)
            .chatModel(stubChatModel)
            .tools(agentTools())
            .chatMemoryProvider(id -> sharedStore.get((String) id))
            .build();

    service.analyze(FIRST_QUESTION, "2026-07-30T11:15Z/11:30Z", "session-X");
    // After turn 1 the store holds that turn; turn 2 must replay it into the model request.
    assertThat(textOf(sharedStore.get("session-X").messages())).contains(FIRST_QUESTION);

    service.analyze(SECOND_QUESTION, "2026-07-30T11:15Z/11:30Z", "session-X");

    String secondRequest = textOf(stubChatModel.lastRequest().messages());
    assertThat(secondRequest).contains(FIRST_QUESTION).contains(SECOND_QUESTION);
    assertThat(textOf(sharedStore.get("session-Y").messages())).doesNotContain(FIRST_QUESTION);
    assertThat(sharedStore.get("session-Y").messages()).isEmpty();
  }

  private static String textOf(List<ChatMessage> messages) {
    return messages.stream().map(Object::toString).reduce("", (a, b) -> a + "\n" + b);
  }

  private static AgentTools agentTools() {
    var scenario = FixtureCatalog.forFixture(FixtureId.OTP_NORMAL_001);
    Investigation investigation =
        Investigation.receive(
            FIRST_QUESTION,
            new TimeWindow(
                Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z")),
            "v1",
            "v1");
    investigation.startCollectingEvidence();
    return new AgentTools(
        new FixtureOtpMetricsTool(scenario),
        new FixtureErrorDistributionTool(scenario),
        new FixtureQueueHealthTool(scenario),
        new FixtureProviderHealthTool(scenario),
        new FixtureRecentChangesTool(scenario),
        (query, provider, topK) -> List.of(),
        new ToolBudgetGuard(8, Duration.ofSeconds(2), 1),
        new EvidenceCollector(investigation));
  }
}
