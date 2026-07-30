package com.example.otpsentinel.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.domain.Evidence;
import com.example.otpsentinel.domain.Investigation;
import com.example.otpsentinel.domain.TimeWindow;
import com.example.otpsentinel.rag.KnowledgeSearchResult;
import com.example.otpsentinel.tools.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvidenceCollectorTest {

  private Investigation newInvestigation() {
    Investigation investigation =
        Investigation.receive(
            "why did OTP success rate drop",
            new TimeWindow(Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z")),
            "v1",
            "v1");
    investigation.startCollectingEvidence();
    return investigation;
  }

  @Test
  void mapsOtpMetricsToCurrentAndPreviousEvidence() {
    Investigation investigation = newInvestigation();
    EvidenceCollector collector = new EvidenceCollector(investigation);

    OtpMetricsResult data =
        new OtpMetricsResult(
            new TimeWindow(Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z")),
            12480, 8998, 3482, 72.10, 8.7,
            new PeriodComparison(
                new TimeWindow(Instant.parse("2026-07-30T11:00:00Z"), Instant.parse("2026-07-30T11:15:00Z")),
                11940, 98.10, 2.2));
    ToolResult<OtpMetricsResult> result =
        ToolResult.success("exec-1", "getOtpMetrics", Instant.now(), data);

    AgentToolResponse<OtpMetricsResult> response = collector.collect(result);

    assertThat(response.evidenceIds()).containsExactly("ev-otp-success-rate-current", "ev-otp-success-rate-previous");
    assertThat(investigation.evidence()).hasSize(2);
    assertThat(investigation.evidence().get(0).metricValue()).isEqualTo(72.10);
    assertThat(investigation.toolExecutions()).containsExactly("exec-1");
  }

  @Test
  void mapsProviderHealthSuccessToTimeoutAndCapacityEvidence() {
    Investigation investigation = newInvestigation();
    EvidenceCollector collector = new EvidenceCollector(investigation);

    ProviderHealthResult data =
        new ProviderHealthResult(
            "OPERATOR_B", "DEGRADED", 13.9, 0.31, Instant.now(), "HALF_OPEN", 48, 50);
    ToolResult<ProviderHealthResult> result =
        ToolResult.success("exec-4", "getProviderHealth", Instant.now(), data);

    AgentToolResponse<ProviderHealthResult> response = collector.collect(result);

    assertThat(response.evidenceIds()).containsExactly("ev-timeout-rate", "ev-connection-capacity");
    Evidence capacity = investigation.evidence().get(1);
    assertThat(capacity.metricValue()).isEqualTo(48.0 / 50.0);
  }

  @Test
  void mapsProviderHealthTimeoutToNoEvidence() {
    Investigation investigation = newInvestigation();
    EvidenceCollector collector = new EvidenceCollector(investigation);

    ToolResult<ProviderHealthResult> result =
        ToolResult.timeout(
            "exec-4", "getProviderHealth", Instant.now(), new ToolError("TIMEOUT", "no response"));

    AgentToolResponse<ProviderHealthResult> response = collector.collect(result);

    assertThat(response.evidenceIds()).isEmpty();
    assertThat(response.status()).isEqualTo(ToolStatus.TIMEOUT);
    assertThat(response.errorMessage()).isEqualTo("no response");
    assertThat(investigation.evidence()).isEmpty();
  }

  @Test
  void mapsRecentChangesToOneEvidencePerConfigOrDeployEvent() {
    Investigation investigation = newInvestigation();
    EvidenceCollector collector = new EvidenceCollector(investigation);

    RecentChangesResult data =
        new RecentChangesResult(
            List.of(
                new ChangeEvent("chg-101", Instant.now(), "CONFIG", "OTP_GATEWAY", "retry 3->2", null, true),
                new ChangeEvent("chg-102", Instant.now(), "DEPLOY", "OTP_GATEWAY", "v2.4 deployed", "v2.4", true),
                new ChangeEvent("obs-103", Instant.now(), "OBSERVATION", "OPERATOR_B_ADAPTER", "latency up", null, null)));
    ToolResult<RecentChangesResult> result =
        ToolResult.success("exec-5", "getRecentChanges", Instant.now(), data);

    AgentToolResponse<RecentChangesResult> response = collector.collect(result);

    assertThat(response.evidenceIds()).containsExactly("ev-change-chg-101", "ev-change-chg-102");
  }

  @Test
  void mapsKnowledgeSearchResultsToReferencesWithoutMintingEvidence() {
    Investigation investigation = newInvestigation();
    EvidenceCollector collector = new EvidenceCollector(investigation);

    List<KnowledgeSearchResult> results =
        List.of(new KnowledgeSearchResult("KB-1", "1", "Connection pool runbook", "KB-1#v1#c0", 0.82, "content"));

    List<KnowledgeReference> refs = collector.collectKnowledge(results);

    assertThat(refs).containsExactly(new KnowledgeReference("KB-1", "KB-1#v1#c0"));
    assertThat(investigation.evidence()).isEmpty();
    assertThat(collector.knownKnowledgeReferences()).containsExactly(new KnowledgeReference("KB-1", "KB-1#v1#c0"));
  }
}
