package com.example.otpsentinel.agent.stub;

import java.util.List;
import java.util.Map;

/**
 * The OTP-DROP-001 demo fixture's deterministic model script (AI-006), shared between the M5 test
 * and the M7 default {@code AgentConfig} chatModel bean.
 */
public final class OtpDropOneOhOneScript {

  private OtpDropOneOhOneScript() {}

  public static StubScript build() {
    return new StubScript(
        List.of(
            call(
                "getOtpMetrics",
                Map.of(
                    "startAt",
                    "2026-07-30T11:15:00Z",
                    "endAt",
                    "2026-07-30T11:30:00Z",
                    "includePreviousPeriod",
                    "true")),
            call(
                "getErrorDistribution",
                Map.of(
                    "startAt",
                    "2026-07-30T11:15:00Z",
                    "endAt",
                    "2026-07-30T11:30:00Z",
                    "provider",
                    "")),
            call("getQueueHealth", Map.of()),
            call(
                "getProviderHealth",
                Map.of(
                    "provider",
                    "OPERATOR_B",
                    "startAt",
                    "2026-07-30T11:15:00Z",
                    "endAt",
                    "2026-07-30T11:30:00Z")),
            call(
                "getRecentChanges",
                Map.of(
                    "from",
                    "2026-07-30T11:00:00Z",
                    "to",
                    "2026-07-30T11:30:00Z",
                    "component",
                    "OTP_GATEWAY")),
            call(
                "searchIncidentKnowledge",
                Map.of(
                    "query",
                    "OTP success rate drop connection pool timeout",
                    "providerFilter",
                    "OPERATOR_B",
                    "topK",
                    5)),
            StubScriptStep.finalAnswer(
                """
                {"status":"ANOMALY_CONFIRMED","severity":"HIGH",
                 "summary":"OTP success rate dropped to 72.10% with OPERATOR_B timeouts near connection pool capacity; gateway v2.4 deployment timing is correlated, not proven causal.",
                 "evidence":[{"evidenceId":"ev-otp-success-rate-current"},{"evidenceId":"ev-otp-success-rate-previous"},
                   {"evidenceId":"ev-timeout-rate"},{"evidenceId":"ev-connection-capacity"},{"evidenceId":"ev-change-chg-102"}],
                 "hypotheses":[
                   {"rank":1,"possibleCause":"OPERATOR_B connection pool exhaustion","probability":0.7,
                    "supportingEvidenceIds":["ev-timeout-rate","ev-connection-capacity"],
                    "contradictingEvidenceIds":[],"verificationSteps":["check pool metrics dashboard"]},
                   {"rank":2,"possibleCause":"Gateway v2.4 deploy is correlated in time, not a confirmed cause","probability":0.3,
                    "supportingEvidenceIds":["ev-change-chg-102"],"contradictingEvidenceIds":[],
                    "verificationSteps":["compare deploy configuration"]}
                 ],
                 "recommendedActions":[{"actionType":"MANUAL_CHECK",
                   "description":"Inspect OPERATOR_B connection pool sizing","risk":"MEDIUM",
                   "requiresApproval":false,"executionMode":"MANUAL_CHECK"}],
                 "knowledgeReferences":[{"documentId":"INC-2026-041","chunkId":"INC-2026-041#v1#c0"}],
                 "confidence":0.85}
                """)));
  }

  private static StubScriptStep call(String toolName, Map<String, Object> arguments) {
    return StubScriptStep.callTools(new StubScriptStep.PlannedToolCall(toolName, arguments));
  }
}
