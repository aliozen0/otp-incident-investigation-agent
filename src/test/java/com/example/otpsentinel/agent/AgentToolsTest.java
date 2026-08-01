package com.example.otpsentinel.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.domain.TimeWindow;
import com.example.otpsentinel.rag.KnowledgeSearchPort;
import com.example.otpsentinel.rag.KnowledgeSearchResult;
import com.example.otpsentinel.tools.ToolStatus;
import com.example.otpsentinel.tools.fixtures.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentToolsTest {

  @Test
  void getOtpMetricsDelegatesThroughGuardAndCollector() {
    FixtureScenario scenario = FixtureCatalog.forFixture(FixtureId.OTP_DROP_001);
    Investigation investigation =
        Investigation.receive(
            "why did OTP success rate drop",
            new TimeWindow(
                Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z")),
            "v1",
            "v1");
    investigation.startCollectingEvidence();
    ToolBudgetGuard guard = new ToolBudgetGuard(8, Duration.ofSeconds(2), 1);
    EvidenceCollector collector = new EvidenceCollector(investigation);
    KnowledgeSearchPort noResults = (query, provider, topK) -> List.of();

    AgentTools tools =
        new AgentTools(
            new FixtureOtpMetricsTool(scenario),
            new FixtureErrorDistributionTool(scenario),
            new FixtureQueueHealthTool(scenario),
            new FixtureProviderHealthTool(scenario),
            new FixtureRecentChangesTool(scenario),
            noResults,
            guard,
            collector);

    AgentToolResponse<?> response =
        tools.getOtpMetrics("2026-07-30T11:15:00Z", "2026-07-30T11:30:00Z", "true");

    assertThat(response.status()).isEqualTo(ToolStatus.SUCCESS);
    assertThat(response.evidenceIds()).contains("ev-otp-success-rate-current");
    assertThat(guard.callCount()).isEqualTo(1);
    assertThatThrownBy(() -> tools.getOtpMetrics("not-an-instant", "2026-07-30T11:30:00Z", "true"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("startAt must be an ISO-8601 UTC instant");
    assertThat(guard.callCount()).isEqualTo(1);
  }

  @Test
  void searchIncidentKnowledgeReturnsReferencesNotEvidence() {
    Investigation investigation =
        Investigation.receive(
            "why did OTP success rate drop",
            new TimeWindow(
                Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z")),
            "v1",
            "v1");
    investigation.startCollectingEvidence();
    ToolBudgetGuard guard = new ToolBudgetGuard(8, Duration.ofSeconds(2), 1);
    EvidenceCollector collector = new EvidenceCollector(investigation);
    FixtureScenario scenario = FixtureCatalog.forFixture(FixtureId.OTP_DROP_001);
    KnowledgeSearchPort port =
        (query, provider, topK) ->
            List.of(
                new KnowledgeSearchResult(
                    "KB-1", "1", "Connection pool runbook", "KB-1#v1#c0", 0.82, "content"));

    AgentTools tools =
        new AgentTools(
            new FixtureOtpMetricsTool(scenario),
            new FixtureErrorDistributionTool(scenario),
            new FixtureQueueHealthTool(scenario),
            new FixtureProviderHealthTool(scenario),
            new FixtureRecentChangesTool(scenario),
            port,
            guard,
            collector);

    List<KnowledgeReference> refs =
        tools.searchIncidentKnowledge("connection pool timeout", "OPERATOR_B", 5);

    assertThat(refs).containsExactly(new KnowledgeReference("KB-1", "KB-1#v1#c0"));
    assertThat(investigation.evidence()).isEmpty();
  }
}
