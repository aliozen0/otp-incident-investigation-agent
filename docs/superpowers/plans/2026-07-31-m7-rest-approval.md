# M7 — REST/Approval Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire the already-implemented domain (M1), persistence (M3), agent orchestration (M5) and
validation (M6) layers into a working REST API with a human-in-the-loop incident approval flow, per
`docs/06-api-contracts.md`.

**Architecture:** Spring MVC controllers in `api` (thin, DTO-only) call a new `InvestigationOrchestrator`
Spring bean in `config` (glue: composes the framework-agnostic `IncidentInvestigationService` +
LangChain4j `AiServices` + fixture tools + JDBC repositories per request). `IncidentInvestigationService`
and `EvidenceCollector` gain additive audit-emission overloads (still plain Java, `AuditEventRepository`
is a domain port) to satisfy FR-017. Idempotency for incident-draft decisions relies solely on the
existing DB unique constraint (`uq_incident_draft_idempotency_key`) — the app catches the resulting
constraint violation and replays, it never pre-checks.

**Tech Stack:** Java 21, Spring Boot 3.3.5 (`spring-boot-starter-web`/`-jdbc`/`-validation`), LangChain4j
1.18.1, PostgreSQL/pgvector via Testcontainers for tests, JUnit 5, AssertJ, MockMvc.

## Global Constraints

- Base path `/api/v1`, JSON, timestamps ISO-8601 UTC, `X-Correlation-Id` request/response header, write
  header `Idempotency-Key`, problem-details error envelope (`docs/06-api-contracts.md`).
- question: 10–1000 chars; time window: 1 min–24 h, no future timestamps; locale allowlist
  `tr-TR`, `en-US` (not specified in docs/06 beyond "allowlist" — documented assumption, flag in report).
- Error codes: `400 INVALID_TIME_WINDOW`, `400 INVALID_REQUEST`, `422 QUESTION_NOT_ACTIONABLE`,
  `429 INVESTIGATION_RATE_LIMITED`, `502 MODEL_PROVIDER_ERROR`, `504 INVESTIGATION_TIMEOUT`.
- No LLM write tool; `createIncidentDraft` stays absent from `AgentTools` (do not touch `AgentTools.java`
  tool set).
- `IncidentDraft` can only become `CREATED` via `IncidentDraft.approve(actor)` then
  `IncidentDraft.create(externalIncidentId)` — controllers never bypass these domain methods.
- DTOs in `api` package never expose domain types (`Investigation`, `IncidentDraft`, `Evidence`, ...)
  directly — always map to/from dedicated request/response records.
- No new Spring `@Profile`s; keep using the existing `AI_MODE` env-var switch pattern from `AgentConfig`.
- Run all `mvn` and `docker` commands from the repository root.
- `mvn -B spotless:apply` then `mvn -B verify -Dsurefire.excludedGroups=local-live` must be green before
  the final commit.

---

## Design decisions carried across tasks

1. **Preview is never persisted.** `POST .../incident-draft/preview` computes a payload view from the
   persisted `Investigation` and returns it — no `IncidentDraft` row is written. This trivially satisfies
   FR-013 ("Preview kalıcı incident oluşturmamalıdır").
2. **The decision endpoint builds the draft fresh, every time.** `POST .../incident-draft/decisions` has
   no draft-id in its request body (per `docs/06`) — only `Idempotency-Key` + `decision` + `reason`. So on
   every call it re-derives the same deterministic payload from the investigation, constructs a new
   `IncidentDraft.preview(investigationId, payload, idempotencyKey)` in memory, applies `approve`/`reject`
   (+`create` on approve), and attempts `IncidentDraftRepository.save(draft)`. A second call with the same
   key generates a **new random `IncidentDraftId`** but the **same `idempotencyKey`**, so the INSERT hits
   `uq_incident_draft_idempotency_key` (not the `id` `ON CONFLICT` clause) and Spring translates it to
   `DataIntegrityViolationException`. The orchestrator catches that, re-reads
   `findByIdempotencyKey(key)`, and returns the original result with `idempotentReplay=true`. This is the
   literal reading of the M7-prompt constraint "Idempotency DB seviyesinde garanti — controller'da ayrıca
   application-level kontrol ekleme".
3. **Decision preconditions.** Before building the draft, the orchestrator requires
   `investigation.phase() == COMPLETED` and `investigation.validationReport().status() == PASSED`
   (docs/09 human-in-the-loop steps 1–2). Violating this returns `409 INVESTIGATION_NOT_ACTIONABLE` — an
   error code not literally enumerated in docs/06 (which only lists errors for the POST /investigations
   endpoint); this is a documented, necessary extension, flagged in the session report as a spec gap, not
   silently invented behavior.
4. **Stub-mode RAG.** No embedding model exists offline. A new `FixtureKnowledgeSearchPort` (in
   `rag.fixtures`) deterministically returns the same `INC-2026-041` result the M5 end-to-end test already
   hand-wired, used only when `AI_MODE=stub` (the default/offline/CI path). `AI_MODE=live` continues to use
   `JdbcKnowledgeSearchAdapter` + `NvidiaNimEmbeddingService`, unchanged.
5. **Stub-mode chat script.** `AgentConfig.chatModel()`'s placeholder "No scenario is wired" stub script is
   replaced with the real OTP-DROP-001 script (the exact one used in
   `OtpDropOneOhOneEndToEndTest`, extracted into a shared class), so `POST /api/v1/investigations` produces
   the full `docs/06` example response for the demo fixture's exact question/time-window. Any other
   question/time-window still completes the investigation lifecycle (no crash) but ends `FAILED` because
   the stub script's expected tool arguments won't match — this is accepted, documented MVP behavior
   (AGENTS.md "Complete the OTP-DROP-001 scenario before adding optional features").
6. **Actor.** No real authentication exists yet (SEC-001/docs/09 "Local profilde auth kapalı olabilir").
   The orchestrator uses a constant actor `"demo-operator"` and every response mentioning approval carries
   a `DEMO MODE` signal — for M7 this is the literal `X-Demo-Mode: true` response header set by a single
   filter, matching docs/09's explicit requirement for an open demo-mode signal. No real auth is
   implemented (out of scope per M7-prompt).

---

## File structure

- `src/main/java/com/example/otpsentinel/application/IncidentInvestigationService.java` — modify: add
  audit-emitting overload.
- `src/main/java/com/example/otpsentinel/agent/EvidenceCollector.java` — modify: add
  `correlationId`-aware constructor + TOOL_CALLED/COMPLETED/FAILED audit emission.
- `src/main/java/com/example/otpsentinel/rag/fixtures/FixtureKnowledgeSearchPort.java` — new.
- `src/main/java/com/example/otpsentinel/agent/stub/OtpDropOneOhOneScript.java` — new: shared stub script
  (extracted from the M5 test).
- `src/main/java/com/example/otpsentinel/config/AgentConfig.java` — modify: use the shared script; add
  `@Bean`s for the five fixture tools + `KnowledgeSearchPort` (mode-switched) + `ToolBudgetGuard`
  parameters.
- `src/main/java/com/example/otpsentinel/config/PersistenceConfig.java` — new: `@Bean` methods for the
  three JDBC repositories.
- `src/main/java/com/example/otpsentinel/config/InvestigationOrchestrator.java` — new: the per-request
  composition root + audit emission for REQUEST_ACCEPTED/TIME_WINDOW_RESOLVED/PREVIEW_GENERATED/
  APPROVAL_DECIDED/INCIDENT_CREATED + idempotent-replay handling.
- `src/main/java/com/example/otpsentinel/config/CorrelationIdFilter.java` — new: reads/generates
  `X-Correlation-Id`, echoes it, also sets `X-Demo-Mode: true`.
- `src/main/java/com/example/otpsentinel/api/dto/*.java` — new: request/response DTOs.
- `src/main/java/com/example/otpsentinel/api/InvestigationRequestValidator.java` — new.
- `src/main/java/com/example/otpsentinel/api/ApiException.java` + subclasses — new: typed exceptions
  carrying an HTTP status + error code.
- `src/main/java/com/example/otpsentinel/api/GlobalExceptionHandler.java` — new: `@ControllerAdvice`,
  problem-details.
- `src/main/java/com/example/otpsentinel/api/InvestigationController.java` — new: POST/GET investigations.
- `src/main/java/com/example/otpsentinel/api/IncidentDraftController.java` — new: preview/decisions.
- `src/test/java/com/example/otpsentinel/api/*Test.java` — new integration tests (MockMvc +
  `AbstractPostgresIntegrationTest`).
- `docs/17-traceability-risk-dod.md` — modify: check off Idempotency pass, US-011/012/013 rows.

---

## Task 1: FR-017 audit hooks — EvidenceCollector tool-call events

**Files:**
- Modify: `src/main/java/com/example/otpsentinel/agent/EvidenceCollector.java`
- Test: `src/test/java/com/example/otpsentinel/agent/EvidenceCollectorTest.java`

**Interfaces:**
- Consumes: `AuditEventRepository.append(AuditEvent)` (existing port), `Investigation.id()`,
  `Investigation.promptVersion()` (existing).
- Produces: `EvidenceCollector(Investigation, AuditEventRepository, String correlationId)` — new 3-arg
  constructor. `collect(ToolResult<T>)` now emits `TOOL_CALLED` then `TOOL_COMPLETED`/`TOOL_FAILED` when
  an audit repository is present. Existing 1-arg and 2-arg constructors are unchanged and pass
  `correlationId = null` (behavior-preserving for all M5/M6 call sites).

The existing `EvidenceCollector(Investigation, AuditEventRepository)` 2-arg constructor is used only for
the prompt-injection signal today; keep it, but internally delegate to the new 3-arg one with
`correlationId = null` so its audit rows keep exactly their current shape (do not touch
`PromptInjectionSignalTest` behavior).

- [ ] **Step 1: Write the failing test**

Add to `EvidenceCollectorTest.java` (open the existing file first — this is a new `@Test` method, keep the
existing ones untouched):

```java
@Test
void auditsToolCalledAndCompletedWhenAuditRepositoryPresent() {
  TimeWindow window = new TimeWindow(Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z"));
  Investigation investigation = Investigation.receive("q", window, "v1", "v1");
  investigation.startCollectingEvidence();
  List<AuditEvent> captured = new ArrayList<>();
  AuditEventRepository auditRepo = new AuditEventRepository() {
    public void append(AuditEvent event) { captured.add(event); }
    public List<AuditEvent> findByInvestigationId(InvestigationId id) { return List.of(); }
  };
  EvidenceCollector collector = new EvidenceCollector(investigation, auditRepo, "corr-1");

  ToolResult<QueueHealthResult> success = ToolResult.success(
      "exec-1", "getQueueHealth", Instant.now(),
      new QueueHealthResult(1L, 1000L, 0L, 0L, 1, 1, 0L, "NORMAL", "HEALTHY"));
  collector.collect(success);

  assertThat(captured).extracting(AuditEvent::action)
      .containsExactly(AuditEventType.TOOL_CALLED, AuditEventType.TOOL_COMPLETED);
  assertThat(captured).allSatisfy(e -> assertThat(e.correlationId()).isEqualTo("corr-1"));
  assertThat(captured.get(1).result()).contains("getQueueHealth");
}

@Test
void auditsToolFailedOnNonSuccessResult() {
  TimeWindow window = new TimeWindow(Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z"));
  Investigation investigation = Investigation.receive("q", window, "v1", "v1");
  investigation.startCollectingEvidence();
  List<AuditEvent> captured = new ArrayList<>();
  AuditEventRepository auditRepo = new AuditEventRepository() {
    public void append(AuditEvent event) { captured.add(event); }
    public List<AuditEvent> findByInvestigationId(InvestigationId id) { return List.of(); }
  };
  EvidenceCollector collector = new EvidenceCollector(investigation, auditRepo, "corr-2");

  ToolResult<QueueHealthResult> timedOut = ToolResult.timeout(
      "exec-2", "getProviderHealth", Instant.now(), new ToolError("TIMEOUT", "no response"));
  collector.collect(timedOut);

  assertThat(captured).extracting(AuditEvent::action)
      .containsExactly(AuditEventType.TOOL_CALLED, AuditEventType.TOOL_FAILED);
}
```

Add the needed imports (`AuditEvent`, `AuditEventRepository`, `AuditEventType`, `InvestigationId`,
`ToolError`, `ArrayList`) to the test file's import block.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=EvidenceCollectorTest`
Expected: compile error (`EvidenceCollector(Investigation, AuditEventRepository, String)` does not exist).

- [ ] **Step 3: Implement**

In `EvidenceCollector.java`, replace the 2-arg constructor and add the 3-arg one, and update `collect`:

```java
private final String correlationId;

public EvidenceCollector(Investigation investigation, AuditEventRepository auditEventRepository) {
  this(investigation, auditEventRepository, null);
}

public EvidenceCollector(
    Investigation investigation, AuditEventRepository auditEventRepository, String correlationId) {
  this.investigation = Objects.requireNonNull(investigation, "investigation must not be null");
  this.auditEventRepository = auditEventRepository;
  this.correlationId = correlationId;
}

public <T> AgentToolResponse<T> collect(ToolResult<T> result) {
  investigation.recordToolExecution(result.executionId());
  auditToolCalled(result.toolName());
  if (result.status() != ToolStatus.SUCCESS) {
    auditToolOutcome(AuditEventType.TOOL_FAILED, result.toolName(), result.error().message());
    return new AgentToolResponse<>(result.status(), null, List.of(), result.error().message());
  }
  List<String> ids = mintEvidence(result);
  auditToolOutcome(AuditEventType.TOOL_COMPLETED, result.toolName(), "ids=" + ids);
  return new AgentToolResponse<>(result.status(), result.data(), ids, null);
}

private void auditToolCalled(String toolName) {
  if (auditEventRepository == null) {
    return;
  }
  auditEventRepository.append(
      AuditEvent.of(
          "system", AuditEventType.TOOL_CALLED, investigation.id(), null,
          correlationId, "tool=" + toolName, investigation.promptVersion()));
}

private void auditToolOutcome(AuditEventType type, String toolName, String result) {
  if (auditEventRepository == null) {
    return;
  }
  auditEventRepository.append(
      AuditEvent.of(
          "system", type, investigation.id(), null,
          correlationId, "tool=" + toolName + " " + result, investigation.promptVersion()));
}
```

Leave `collectKnowledge`/`auditIfInstructionPattern` exactly as-is (unrelated to this task).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -B test -Dtest=EvidenceCollectorTest`
Expected: BUILD SUCCESS, all tests (old + 2 new) green.

- [ ] **Step 5: Run the full existing suite to confirm no regression**

Run: `mvn -B test -Dtest=EvidenceCollectorTest,AgentToolsTest,OtpDropOneOhOneEndToEndTest,IncidentInvestigationServiceTest,PromptInjectionSignalTest`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/otpsentinel/agent/EvidenceCollector.java src/test/java/com/example/otpsentinel/agent/EvidenceCollectorTest.java
git commit -m "feat(m7): audit TOOL_CALLED/COMPLETED/FAILED events in EvidenceCollector"
```

---

## Task 2: FR-017 audit hooks — IncidentInvestigationService RAG/LLM/validation events

**Files:**
- Modify: `src/main/java/com/example/otpsentinel/application/IncidentInvestigationService.java`
- Test: `src/test/java/com/example/otpsentinel/application/IncidentInvestigationServiceTest.java`

**Interfaces:**
- Consumes: `AuditEventRepository` (domain port), Task 1's `EvidenceCollector` 3-arg constructor.
- Produces: new overload
  `Investigation investigate(InvestigationRequest, Investigation, IncidentAnalysisAiService, ToolBudgetGuard, EvidenceCollector, AuditEventRepository auditEventRepository, String correlationId)`.
  The existing 5-arg `investigate(...)` is kept, unchanged, and delegates to the new one with
  `auditEventRepository = null, correlationId = null`.

RAG_COMPLETED cannot be emitted from inside `IncidentInvestigationService` (it never sees knowledge search
calls directly — `AgentTools`/`EvidenceCollector.collectKnowledge` does). Emit it, gated the same way,
from `EvidenceCollector.collectKnowledge` instead — extend that method in this task too (it's the same
audit-gating pattern as Task 1, small addition, same file already touched by Task 1's reviewer).

- [ ] **Step 1: Write the failing test**

Add to `EvidenceCollectorTest.java`:

```java
@Test
void auditsRagCompletedOnKnowledgeCollection() {
  TimeWindow window = new TimeWindow(Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z"));
  Investigation investigation = Investigation.receive("q", window, "v1", "v1");
  investigation.startCollectingEvidence();
  List<AuditEvent> captured = new ArrayList<>();
  AuditEventRepository auditRepo = new AuditEventRepository() {
    public void append(AuditEvent event) { captured.add(event); }
    public List<AuditEvent> findByInvestigationId(InvestigationId id) { return List.of(); }
  };
  EvidenceCollector collector = new EvidenceCollector(investigation, auditRepo, "corr-3");

  collector.collectKnowledge(List.of(new KnowledgeSearchResult(
      "INC-2026-041", "1", "title", "chunk-0", 0.9, "benign content")));

  assertThat(captured).extracting(AuditEvent::action).containsExactly(AuditEventType.RAG_COMPLETED);
}
```

Add to `IncidentInvestigationServiceTest.java` (new test method; keep existing tests untouched):

```java
@Test
void auditsLlmCompletedAndValidationPassedOnSuccessfulCompletion() {
  // Arrange exactly like the existing "completesSuccessfully"-style test in this file:
  // reuse whatever Investigation/AiService/guard/collector setup an existing passing-path
  // test already builds (copy its arrange block), but construct the collector via
  // new EvidenceCollector(investigation, auditRepo, "corr-4") and call the new 7-arg
  // investigate(...) overload with (auditRepo, "corr-4") appended.
  List<AuditEvent> captured = new ArrayList<>();
  AuditEventRepository auditRepo = new AuditEventRepository() {
    public void append(AuditEvent event) { captured.add(event); }
    public List<AuditEvent> findByInvestigationId(InvestigationId id) { return List.of(); }
  };
  // ... build investigation/guard/collector/aiService as the existing happy-path test does,
  // but pass auditRepo into the EvidenceCollector constructor ...
  Investigation outcome = new IncidentInvestigationService(1)
      .investigate(request, investigation, aiService, guard, collector, auditRepo, "corr-4");

  assertThat(outcome.phase()).isEqualTo(InvestigationPhase.COMPLETED);
  assertThat(captured).extracting(AuditEvent::action)
      .contains(AuditEventType.LLM_COMPLETED, AuditEventType.VALIDATION_PASSED);
}
```

Implementer note: open `IncidentInvestigationServiceTest.java` first and copy the exact arrange block from
its existing happy-path test (e.g. a test that reaches `InvestigationPhase.COMPLETED`) rather than
reinventing fixture wiring — this repo already has that pattern proven in
`OtpDropOneOhOneEndToEndTest`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -B test -Dtest=EvidenceCollectorTest,IncidentInvestigationServiceTest`
Expected: compile errors (new methods/overloads don't exist yet).

- [ ] **Step 3: Implement — EvidenceCollector.collectKnowledge**

```java
public List<KnowledgeReference> collectKnowledge(List<KnowledgeSearchResult> results) {
  if (auditEventRepository != null) {
    results.forEach(this::auditIfInstructionPattern);
  }
  List<KnowledgeReference> refs =
      results.stream().map(r -> new KnowledgeReference(r.documentId(), r.chunkId())).toList();
  knownKnowledgeReferences.addAll(refs);
  if (auditEventRepository != null) {
    auditEventRepository.append(
        AuditEvent.of(
            "system", AuditEventType.RAG_COMPLETED, investigation.id(), null,
            correlationId, "results=" + refs.size(), investigation.promptVersion()));
  }
  return refs;
}
```

- [ ] **Step 4: Implement — IncidentInvestigationService**

```java
public Investigation investigate(
    InvestigationRequest request,
    Investigation investigation,
    IncidentAnalysisAiService aiService,
    ToolBudgetGuard guard,
    EvidenceCollector collector) {
  return investigate(request, investigation, aiService, guard, collector, null, null);
}

public Investigation investigate(
    InvestigationRequest request,
    Investigation investigation,
    IncidentAnalysisAiService aiService,
    ToolBudgetGuard guard,
    EvidenceCollector collector,
    AuditEventRepository auditEventRepository,
    String correlationId) {
  Objects.requireNonNull(request, "request must not be null");
  Objects.requireNonNull(investigation, "investigation must not be null");
  Objects.requireNonNull(aiService, "aiService must not be null");
  Objects.requireNonNull(guard, "guard must not be null");
  Objects.requireNonNull(collector, "collector must not be null");
  requireMatchingRequest(request, investigation);

  investigation.startCollectingEvidence();
  AnalysisAttempt attempt = callWithRepair(aiService, request);
  if (attempt.policyLimitReached() || guard.policyLimitReached()) {
    investigation.partial(
        InvestigationStatus.PARTIAL_ANALYSIS,
        ValidationReport.passed(List.of("tool policy limit reached; analysis is partial")));
    return investigation;
  }
  if (attempt.analysis() == null) {
    investigation.fail("structured output invalid after " + maxRepairAttempts + " repair attempt(s)");
    return investigation;
  }
  audit(auditEventRepository, AuditEventType.LLM_COMPLETED, investigation, correlationId, "ok");

  IncidentAnalysisResult analysis = attempt.analysis();
  investigation.startGeneratingAnalysis();
  ValidationReport claimReport = claimValidator.validate(analysis, investigation.evidence());
  if (claimReport.status() == ValidationStatus.FAILED) {
    audit(auditEventRepository, AuditEventType.VALIDATION_FAILED, investigation, correlationId,
        claimReport.warnings().getFirst());
    investigation.fail(claimReport.warnings().getFirst());
    return investigation;
  }

  List<String> acceptedKnowledgeReferences =
      filterKnownKnowledgeReferences(analysis.knowledgeReferences(), collector);
  try {
    investigation.proposeAnalysis(
        analysis.severity(), analysis.hypotheses(), analysis.recommendedActions(),
        acceptedKnowledgeReferences, analysis.confidence());
    investigation.startValidating();
    finish(investigation, analysis, claimReport.warnings());
    audit(auditEventRepository, AuditEventType.VALIDATION_PASSED, investigation, correlationId, "ok");
  } catch (IllegalArgumentException | IllegalStateException e) {
    audit(auditEventRepository, AuditEventType.VALIDATION_FAILED, investigation, correlationId,
        "analysis rejected by deterministic validation");
    investigation.fail("analysis rejected by deterministic validation");
  }
  return investigation;
}

private static void audit(
    AuditEventRepository repo, AuditEventType type, Investigation investigation,
    String correlationId, String result) {
  if (repo == null) {
    return;
  }
  repo.append(
      AuditEvent.of(
          "system", type, investigation.id(), null, correlationId, result,
          investigation.promptVersion()));
}
```

Add imports: `com.example.otpsentinel.domain.AuditEvent`, `AuditEventRepository`, `AuditEventType`.

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -B test -Dtest=EvidenceCollectorTest,IncidentInvestigationServiceTest,OtpDropOneOhOneEndToEndTest`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/otpsentinel/agent/EvidenceCollector.java src/main/java/com/example/otpsentinel/application/IncidentInvestigationService.java src/test/java/com/example/otpsentinel/agent/EvidenceCollectorTest.java src/test/java/com/example/otpsentinel/application/IncidentInvestigationServiceTest.java
git commit -m "feat(m7): audit RAG_COMPLETED/LLM_COMPLETED/VALIDATION_PASSED/FAILED events"
```

---

## Task 3: Stub-mode knowledge search fixture + shared OTP-DROP-001 stub script

**Files:**
- Create: `src/main/java/com/example/otpsentinel/rag/fixtures/FixtureKnowledgeSearchPort.java`
- Create: `src/main/java/com/example/otpsentinel/agent/stub/OtpDropOneOhOneScript.java`
- Test: `src/test/java/com/example/otpsentinel/rag/fixtures/FixtureKnowledgeSearchPortTest.java`
- Test: `src/test/java/com/example/otpsentinel/agent/stub/OtpDropOneOhOneScriptTest.java`

**Interfaces:**
- Produces: `FixtureKnowledgeSearchPort implements KnowledgeSearchPort` — no-arg constructor, deterministic.
- Produces: `OtpDropOneOhOneScript.build() -> StubScript` — static factory, byte-for-byte the script
  currently inlined in `OtpDropOneOhOneEndToEndTest` (Task 4/6 will reuse it from `AgentConfig`; Task's own
  test just asserts it's non-null and has the expected step count/tool order).

- [ ] **Step 1: Write the failing tests**

`FixtureKnowledgeSearchPortTest.java`:

```java
package com.example.otpsentinel.rag.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.rag.KnowledgeSearchResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class FixtureKnowledgeSearchPortTest {

  @Test
  void returnsDeterministicIncidentPostmortemResult() {
    FixtureKnowledgeSearchPort port = new FixtureKnowledgeSearchPort();

    List<KnowledgeSearchResult> results =
        port.searchIncidentKnowledge("OTP success rate drop connection pool timeout", "OPERATOR_B", 5);

    assertThat(results).hasSize(1);
    KnowledgeSearchResult result = results.get(0);
    assertThat(result.documentId()).isEqualTo("INC-2026-041");
    assertThat(result.chunkId()).isEqualTo("INC-2026-041#v1#c0");
    assertThat(result.similarityScore()).isEqualTo(0.85);
  }
}
```

`OtpDropOneOhOneScriptTest.java`:

```java
package com.example.otpsentinel.agent.stub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OtpDropOneOhOneScriptTest {

  @Test
  void buildsSixStepScriptEndingInFinalAnswer() {
    StubScript script = OtpDropOneOhOneScript.build();

    assertThat(script).isNotNull();
  }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -B test -Dtest=FixtureKnowledgeSearchPortTest,OtpDropOneOhOneScriptTest`
Expected: compile errors (classes don't exist).

- [ ] **Step 3: Implement FixtureKnowledgeSearchPort**

```java
package com.example.otpsentinel.rag.fixtures;

import com.example.otpsentinel.rag.KnowledgeSearchPort;
import com.example.otpsentinel.rag.KnowledgeSearchResult;
import java.util.List;

/**
 * Deterministic offline substitute for {@link com.example.otpsentinel.rag.JdbcKnowledgeSearchAdapter}
 * when {@code AI_MODE=stub} (no embedding model available). Always returns the OTP-DROP-001 demo
 * fixture's single INC-2026-041 citation, matching docs/15-demo-fixtures.md.
 */
public final class FixtureKnowledgeSearchPort implements KnowledgeSearchPort {

  @Override
  public List<KnowledgeSearchResult> searchIncidentKnowledge(
      String queryText, String providerFilter, int topK) {
    return List.of(
        new KnowledgeSearchResult(
            "INC-2026-041",
            "1",
            "Connection pool exhaustion incident",
            "INC-2026-041#v1#c0",
            0.85,
            "When active connections approach max and timeout rate rises, suspect connection pool"
                + " exhaustion."));
  }
}
```

- [ ] **Step 4: Implement OtpDropOneOhOneScript**

Copy the `script` variable construction verbatim from
`src/test/java/com/example/otpsentinel/application/OtpDropOneOhOneEndToEndTest.java` (lines building
`StubScript script = new StubScript(List.of(...))`, using its private `call(...)` helper too) into a new
class:

```java
package com.example.otpsentinel.agent.stub;

import java.util.List;
import java.util.Map;

/** The OTP-DROP-001 demo fixture's deterministic model script (AI-006), shared between the M5 test and the M7 default {@code AgentConfig} chatModel bean. */
public final class OtpDropOneOhOneScript {

  private OtpDropOneOhOneScript() {}

  public static StubScript build() {
    return new StubScript(
        List.of(
            call("getOtpMetrics", Map.of(
                "startAt", "2026-07-30T11:15:00Z", "endAt", "2026-07-30T11:30:00Z",
                "includePreviousPeriod", true)),
            call("getErrorDistribution", Map.of(
                "startAt", "2026-07-30T11:15:00Z", "endAt", "2026-07-30T11:30:00Z", "provider", "")),
            call("getQueueHealth", Map.of()),
            call("getProviderHealth", Map.of(
                "provider", "OPERATOR_B", "startAt", "2026-07-30T11:15:00Z",
                "endAt", "2026-07-30T11:30:00Z")),
            call("getRecentChanges", Map.of(
                "from", "2026-07-30T11:00:00Z", "to", "2026-07-30T11:30:00Z",
                "component", "OTP_GATEWAY")),
            call("searchIncidentKnowledge", Map.of(
                "query", "OTP success rate drop connection pool timeout",
                "providerFilter", "OPERATOR_B", "topK", 5)),
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
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -B test -Dtest=FixtureKnowledgeSearchPortTest,OtpDropOneOhOneScriptTest`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Refactor OtpDropOneOhOneEndToEndTest to reuse the shared script (dedup, no behavior change)**

In `OtpDropOneOhOneEndToEndTest.java`, replace the inline `StubScript script = new StubScript(...)`
construction with `StubScript script = OtpDropOneOhOneScript.build();`, remove the now-unused private
`call(...)` helper and its `Map` import if it becomes unused, add
`import com.example.otpsentinel.agent.stub.OtpDropOneOhOneScript;`.

Run: `mvn -B test -Dtest=OtpDropOneOhOneEndToEndTest`
Expected: BUILD SUCCESS (unchanged assertions, now sourced from the shared script).

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/otpsentinel/rag/fixtures/FixtureKnowledgeSearchPort.java src/main/java/com/example/otpsentinel/agent/stub/OtpDropOneOhOneScript.java src/test/java/com/example/otpsentinel/rag/fixtures/FixtureKnowledgeSearchPortTest.java src/test/java/com/example/otpsentinel/agent/stub/OtpDropOneOhOneScriptTest.java src/test/java/com/example/otpsentinel/application/OtpDropOneOhOneEndToEndTest.java
git commit -m "feat(m7): deterministic stub-mode knowledge search + shared OTP-DROP-001 script"
```

---

## Task 4: Spring bean wiring — repositories, fixture tools, orchestrator

**Files:**
- Create: `src/main/java/com/example/otpsentinel/config/PersistenceConfig.java`
- Modify: `src/main/java/com/example/otpsentinel/config/AgentConfig.java`
- Create: `src/main/java/com/example/otpsentinel/config/InvestigationOrchestrator.java`
- Test: `src/test/java/com/example/otpsentinel/config/InvestigationOrchestratorTest.java`

**Interfaces:**
- Consumes: `JdbcTemplate` (Spring Boot autoconfigured), `ChatModel` bean (existing), Task 3's
  `FixtureKnowledgeSearchPort`/`OtpDropOneOhOneScript`, Task 1/2's audit-aware `EvidenceCollector`/
  `IncidentInvestigationService` overloads, `FixtureCatalog.forFixture(FixtureId)` (M2, unchanged),
  `application.yml`'s `otp-sentinel.ai.*`/`otp-sentinel.tool.*`/`otp-sentinel.demo.fixture` properties.
- Produces:
  `InvestigationOrchestrator.runInvestigation(String question, TimeWindow resolvedTimeWindow, String correlationId) -> Investigation`
  (persists the investigation and returns it — used by Task 6's controller).
  `InvestigationOrchestrator.findInvestigation(InvestigationId id) -> Optional<Investigation>` (used by
  Task 7).
  `InvestigationOrchestrator.previewIncidentDraft(InvestigationId id) -> IncidentDraftPreview` record
  `(String title, Severity severity, String summary, int evidenceCount, List<String> recommendedChecks, boolean requiresExplicitApproval)`
  (used by Task 9) — throws `NoSuchElementException` if investigation not found (Task 9 maps to 404).
  `InvestigationOrchestrator.decide(InvestigationId id, String decision, String reason, String idempotencyKey, String correlationId) -> DecisionOutcome`
  record `(IncidentDraftId incidentDraftId, String externalIncidentId, IncidentDraftStatus status, boolean idempotentReplay)`
  (used by Task 10).

**Design notes for this task's implementer:**
- `PersistenceConfig` just exposes the three existing JDBC classes as beans — do not modify the JDBC
  classes themselves (M3, already correct and tested):

```java
package com.example.otpsentinel.config;

import com.example.otpsentinel.adapters.persistence.JdbcAuditEventRepository;
import com.example.otpsentinel.adapters.persistence.JdbcIncidentDraftRepository;
import com.example.otpsentinel.adapters.persistence.JdbcInvestigationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PersistenceConfig {

  @Bean
  public JdbcInvestigationRepository investigationRepository(JdbcTemplate jdbcTemplate) {
    return new JdbcInvestigationRepository(jdbcTemplate);
  }

  @Bean
  public JdbcIncidentDraftRepository incidentDraftRepository(JdbcTemplate jdbcTemplate) {
    return new JdbcIncidentDraftRepository(jdbcTemplate);
  }

  @Bean
  public JdbcAuditEventRepository auditEventRepository(JdbcTemplate jdbcTemplate) {
    return new JdbcAuditEventRepository(jdbcTemplate);
  }
}
```

- `AgentConfig` changes: replace `defaultStubScript()` body with `OtpDropOneOhOneScript.build()`; add
  `KnowledgeSearchPort` bean switched the same way as `chatModel` (by `AI_MODE`); add the five fixture-tool
  beans (built once at startup from `FixtureCatalog.forFixture(FixtureId.valueOf(...))`, keyed off
  `otp-sentinel.demo.fixture`, per Design decision #5/#6 — one demo fixture for the whole app instance,
  consistent with the "Ana fixture" MVP scope):

```java
  @Bean
  public KnowledgeSearchPort knowledgeSearchPort(
      @Value("${AI_MODE:stub}") String aiMode,
      JdbcTemplate jdbcTemplate,
      @Value("${NVIDIA_BASE_URL:https://integrate.api.nvidia.com/v1}") String baseUrl,
      @Value("${NVIDIA_API_KEY:}") String apiKey,
      @Value("${NVIDIA_EMBEDDING_MODEL:}") String embeddingModel,
      @Value("${otp-sentinel.rag.top-k:5}") int topK,
      @Value("${otp-sentinel.rag.min-score:0.70}") double minScore) {
    if ("live".equalsIgnoreCase(aiMode)) {
      return new JdbcKnowledgeSearchAdapter(
          jdbcTemplate, new NvidiaNimEmbeddingService(baseUrl, apiKey, embeddingModel, 1024), topK, minScore);
    }
    return new FixtureKnowledgeSearchPort();
  }

  @Bean
  public FixtureScenario demoFixtureScenario(@Value("${otp-sentinel.demo.fixture:OTP-DROP-001}") String fixtureId) {
    return FixtureCatalog.forFixture(FixtureId.fromWireValue(fixtureId)); // see note below
  }
```

  Implementer note: check `FixtureId.java` for how it parses the wire string `"OTP-DROP-001"` (it may be a
  plain enum needing a small mapping helper, e.g. `FixtureId.valueOf(fixtureId.replace('-', '_'))` — read
  `FixtureId.java` before writing this, since the exact enum constant names/format weren't captured in this
  plan's research and must be verified against the source file).

  Then five tool beans, each `new Fixture<X>Tool(scenario)` (constructors confirmed in
  `OtpDropOneOhOneEndToEndTest`: `new FixtureOtpMetricsTool(scenario)`, `new FixtureErrorDistributionTool(scenario)`,
  `new FixtureQueueHealthTool(scenario)`, `new FixtureProviderHealthTool(scenario)`,
  `new FixtureRecentChangesTool(scenario)`).

- `InvestigationOrchestrator` (the composition root):

```java
package com.example.otpsentinel.config;

import com.example.otpsentinel.adapters.persistence.JdbcAuditEventRepository;
import com.example.otpsentinel.adapters.persistence.JdbcIncidentDraftRepository;
import com.example.otpsentinel.adapters.persistence.JdbcInvestigationRepository;
import com.example.otpsentinel.agent.AgentTools;
import com.example.otpsentinel.agent.EvidenceCollector;
import com.example.otpsentinel.agent.IncidentAnalysisAiService;
import com.example.otpsentinel.agent.ToolBudgetGuard;
import com.example.otpsentinel.application.IncidentInvestigationService;
import com.example.otpsentinel.application.InvestigationRequest;
import com.example.otpsentinel.domain.*;
import com.example.otpsentinel.rag.KnowledgeSearchPort;
import com.example.otpsentinel.tools.*;
import com.example.otpsentinel.tools.fixtures.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class InvestigationOrchestrator {

  private static final String ACTOR = "demo-operator";
  private static final String PROMPT_VERSION = "v1";
  private static final String SCHEMA_VERSION = "v1";

  private final JdbcInvestigationRepository investigationRepository;
  private final JdbcIncidentDraftRepository incidentDraftRepository;
  private final JdbcAuditEventRepository auditEventRepository;
  private final ChatModel chatModel;
  private final KnowledgeSearchPort knowledgeSearchPort;
  private final FixtureOtpMetricsTool otpMetricsTool;
  private final FixtureErrorDistributionTool errorDistributionTool;
  private final FixtureQueueHealthTool queueHealthTool;
  private final FixtureProviderHealthTool providerHealthTool;
  private final FixtureRecentChangesTool recentChangesTool;
  private final int maxToolCalls;
  private final Duration toolTimeout;
  private final int toolRetryCount;
  private final int maxRepairAttempts;

  public InvestigationOrchestrator(
      JdbcInvestigationRepository investigationRepository,
      JdbcIncidentDraftRepository incidentDraftRepository,
      JdbcAuditEventRepository auditEventRepository,
      ChatModel chatModel,
      KnowledgeSearchPort knowledgeSearchPort,
      FixtureOtpMetricsTool otpMetricsTool,
      FixtureErrorDistributionTool errorDistributionTool,
      FixtureQueueHealthTool queueHealthTool,
      FixtureProviderHealthTool providerHealthTool,
      FixtureRecentChangesTool recentChangesTool,
      @Value("${otp-sentinel.ai.max-tool-calls:8}") int maxToolCalls,
      @Value("${otp-sentinel.tool.timeout-millis:2000}") long toolTimeoutMillis,
      @Value("${otp-sentinel.tool.retry-count:1}") int toolRetryCount,
      @Value("${otp-sentinel.ai.max-repair-attempts:1}") int maxRepairAttempts) {
    this.investigationRepository = investigationRepository;
    this.incidentDraftRepository = incidentDraftRepository;
    this.auditEventRepository = auditEventRepository;
    this.chatModel = chatModel;
    this.knowledgeSearchPort = knowledgeSearchPort;
    this.otpMetricsTool = otpMetricsTool;
    this.errorDistributionTool = errorDistributionTool;
    this.queueHealthTool = queueHealthTool;
    this.providerHealthTool = providerHealthTool;
    this.recentChangesTool = recentChangesTool;
    this.maxToolCalls = maxToolCalls;
    this.toolTimeout = Duration.ofMillis(toolTimeoutMillis);
    this.toolRetryCount = toolRetryCount;
    this.maxRepairAttempts = maxRepairAttempts;
  }

  public Investigation runInvestigation(String question, TimeWindow resolvedTimeWindow, String correlationId) {
    Investigation investigation = Investigation.receive(question, resolvedTimeWindow, PROMPT_VERSION, SCHEMA_VERSION);
    audit(AuditEventType.REQUEST_ACCEPTED, investigation.id(), null, correlationId, "question accepted");
    audit(AuditEventType.TIME_WINDOW_RESOLVED, investigation.id(), null, correlationId,
        resolvedTimeWindow.startAt() + "/" + resolvedTimeWindow.endAt());

    ToolBudgetGuard guard = new ToolBudgetGuard(maxToolCalls, toolTimeout, toolRetryCount);
    EvidenceCollector collector = new EvidenceCollector(investigation, auditEventRepository, correlationId);
    AgentTools tools = new AgentTools(
        otpMetricsTool, errorDistributionTool, queueHealthTool, providerHealthTool, recentChangesTool,
        knowledgeSearchPort, guard, collector);
    IncidentAnalysisAiService aiService =
        AiServices.builder(IncidentAnalysisAiService.class).chatModel(chatModel).tools(tools).build();

    Investigation outcome = new IncidentInvestigationService(maxRepairAttempts)
        .investigate(
            new InvestigationRequest(question, resolvedTimeWindow, PROMPT_VERSION, SCHEMA_VERSION),
            investigation, aiService, guard, collector, auditEventRepository, correlationId);
    investigationRepository.save(outcome);
    return outcome;
  }

  public Optional<Investigation> findInvestigation(InvestigationId id) {
    return investigationRepository.findById(id);
  }

  public record IncidentDraftPreview(
      String title, Severity severity, String summary, int evidenceCount,
      List<String> recommendedChecks, boolean requiresExplicitApproval) {}

  public IncidentDraftPreview previewIncidentDraft(InvestigationId investigationId, String correlationId) {
    Investigation investigation = investigationRepository.findById(investigationId)
        .orElseThrow(() -> new NoSuchElementException("investigation not found: " + investigationId));
    IncidentDraftPreview preview = buildPreview(investigation);
    audit(AuditEventType.PREVIEW_GENERATED, investigationId, null, correlationId, "generated");
    return preview;
  }

  public record DecisionOutcome(
      IncidentDraftId incidentDraftId, String externalIncidentId, IncidentDraftStatus status,
      boolean idempotentReplay) {}

  public DecisionOutcome decide(
      InvestigationId investigationId, String decision, String reason, String idempotencyKey,
      String correlationId) {
    Investigation investigation = investigationRepository.findById(investigationId)
        .orElseThrow(() -> new NoSuchElementException("investigation not found: " + investigationId));
    if (investigation.phase() != InvestigationPhase.COMPLETED
        || investigation.validationReport() == null
        || investigation.validationReport().status() != ValidationStatus.PASSED) {
      throw new IllegalStateException("investigation is not ready for a decision: " + investigationId);
    }
    IncidentDraftPreview preview = buildPreview(investigation);
    IncidentDraft draft = IncidentDraft.preview(investigationId, renderPayload(preview), idempotencyKey);

    try {
      if ("APPROVE".equals(decision)) {
        draft.approve(ACTOR);
        draft.create(generateExternalIncidentId());
      } else {
        draft.reject(ACTOR, reason);
      }
      incidentDraftRepository.save(draft);
      audit(AuditEventType.APPROVAL_DECIDED, investigationId, draft.id(), correlationId, decision);
      if ("APPROVE".equals(decision)) {
        audit(AuditEventType.INCIDENT_CREATED, investigationId, draft.id(), correlationId, draft.externalIncidentId());
      }
      return new DecisionOutcome(draft.id(), draft.externalIncidentId(), draft.status(), false);
    } catch (DataIntegrityViolationException replay) {
      IncidentDraft existing = incidentDraftRepository.findByIdempotencyKey(idempotencyKey)
          .orElseThrow(() -> replay);
      return new DecisionOutcome(existing.id(), existing.externalIncidentId(), existing.status(), true);
    }
  }

  private IncidentDraftPreview buildPreview(Investigation investigation) {
    String title = "[" + investigation.severity() + "] " + investigation.question();
    List<String> checks = investigation.hypotheses().stream()
        .flatMap(h -> h.verificationSteps().stream())
        .distinct()
        .toList();
    return new IncidentDraftPreview(
        title, investigation.severity(), summaryOf(investigation), investigation.evidence().size(),
        checks, true);
  }

  private static String summaryOf(Investigation investigation) {
    return investigation.resultStatus() + " severity=" + investigation.severity();
  }

  private static String renderPayload(IncidentDraftPreview preview) {
    return preview.title() + " | " + preview.summary() + " | evidenceCount=" + preview.evidenceCount();
  }

  private static String generateExternalIncidentId() {
    return "DEMO-INC-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  private void audit(
      AuditEventType type, InvestigationId investigationId, IncidentDraftId draftId,
      String correlationId, String result) {
    auditEventRepository.append(
        AuditEvent.of(ACTOR, type, investigationId, draftId, correlationId, result, PROMPT_VERSION));
  }
}
```

Implementer note: `summaryOf`/`renderPayload` are intentionally minimal placeholders for a real
"incident draft payload" formatter — Task 8 below refines `buildPreview`'s summary text to something
closer to `docs/06`'s example (`"Son 15 dakikada OTP başarısı %72,1'e düştü."`). Keep this task's version
simple; do not gold-plate it here.

- [ ] **Step 1: Write the failing test**

`InvestigationOrchestratorTest.java` — extend `AbstractPostgresIntegrationTest` (real Postgres via
Testcontainers, since this composes real JDBC repos):

```java
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
    InvestigationOrchestrator orchestrator = new InvestigationOrchestrator(
        newInvestigationRepository(), newIncidentDraftRepository(), newAuditEventRepository(),
        new StubChatModel(OtpDropOneOhOneScript.build()), new FixtureKnowledgeSearchPort(),
        new FixtureOtpMetricsTool(scenario), new FixtureErrorDistributionTool(scenario),
        new FixtureQueueHealthTool(scenario), new FixtureProviderHealthTool(scenario),
        new FixtureRecentChangesTool(scenario), 8, 2000, 1, 1);

    TimeWindow window = new TimeWindow(
        Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z"));
    Investigation outcome = orchestrator.runInvestigation(
        "why did OTP success rate drop", window, "corr-orch-1");

    assertThat(outcome.phase()).isEqualTo(InvestigationPhase.COMPLETED);
    assertThat(outcome.resultStatus()).isEqualTo(InvestigationStatus.ANOMALY_CONFIRMED);
    assertThat(orchestrator.findInvestigation(outcome.id())).isPresent();
  }
}
```

Note this test constructs `InvestigationOrchestrator` directly with `new...(...)` (no Spring context needed
for this test — `AbstractPostgresIntegrationTest`'s `newXRepository()` helpers already give it
Testcontainers-backed JDBC repos), keeping it fast and consistent with the M3 test style.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=InvestigationOrchestratorTest`
Expected: compile error (class doesn't exist).

- [ ] **Step 3: Implement**

Create `PersistenceConfig.java`, update `AgentConfig.java`, create `InvestigationOrchestrator.java` as
specified above. Read `FixtureId.java` first to confirm the exact wire-format parsing needed for
`otp-sentinel.demo.fixture` (documented as an open implementation detail above).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -B test -Dtest=InvestigationOrchestratorTest`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Run the full main-source compile + previous suites**

Run: `mvn -B test -Dtest=InvestigationOrchestratorTest,OtpDropOneOhOneEndToEndTest,EvidenceCollectorTest,IncidentInvestigationServiceTest`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/otpsentinel/config/PersistenceConfig.java src/main/java/com/example/otpsentinel/config/AgentConfig.java src/main/java/com/example/otpsentinel/config/InvestigationOrchestrator.java src/test/java/com/example/otpsentinel/config/InvestigationOrchestratorTest.java
git commit -m "feat(m7): wire repositories, fixture tools and InvestigationOrchestrator as Spring beans"
```

---

## Task 5: API DTOs, validator, problem-details error handling, correlation-id filter

**Files:**
- Create: `src/main/java/com/example/otpsentinel/api/dto/TimeWindowDto.java`
- Create: `src/main/java/com/example/otpsentinel/api/dto/InvestigationRequestDto.java`
- Create: `src/main/java/com/example/otpsentinel/api/dto/InvestigationResponseDto.java` (+ nested
  `EvidenceDto`, `HypothesisDto`, `RecommendedActionDto`, `KnowledgeReferenceDto`, `ValidationDto`)
- Create: `src/main/java/com/example/otpsentinel/api/dto/IncidentDraftPreviewDto.java`
- Create: `src/main/java/com/example/otpsentinel/api/dto/IncidentDraftDecisionRequestDto.java`
- Create: `src/main/java/com/example/otpsentinel/api/dto/IncidentDraftDecisionResponseDto.java`
- Create: `src/main/java/com/example/otpsentinel/api/dto/ProblemDetailsDto.java`
- Create: `src/main/java/com/example/otpsentinel/api/InvestigationRequestValidator.java`
- Create: `src/main/java/com/example/otpsentinel/api/ApiException.java`
- Create: `src/main/java/com/example/otpsentinel/api/GlobalExceptionHandler.java`
- Create: `src/main/java/com/example/otpsentinel/config/CorrelationIdFilter.java`
- Test: `src/test/java/com/example/otpsentinel/api/InvestigationRequestValidatorTest.java`

**Interfaces:**
- Produces: `ProblemDetailsDto(String type, String title, int status, String detail, String instance, String correlationId, String errorCode)`.
- Produces: `ApiException(int status, String errorCode, String title, String detail)` — a `RuntimeException`
  subclass; `GlobalExceptionHandler` catches it and maps to `ProblemDetailsDto`.
- Produces: `InvestigationRequestValidator.validate(InvestigationRequestDto dto) -> TimeWindow` (resolved,
  UTC) — throws `ApiException` with the matching error code on any violation. Consumed by Task 6.

```java
// api/dto/TimeWindowDto.java
package com.example.otpsentinel.api.dto;

import java.time.Instant;

public record TimeWindowDto(Instant startAt, Instant endAt, String timezone) {}
```

```java
// api/dto/InvestigationRequestDto.java
package com.example.otpsentinel.api.dto;

public record InvestigationRequestDto(String question, TimeWindowRangeDto timeWindow, String locale) {
  public record TimeWindowRangeDto(java.time.Instant startAt, java.time.Instant endAt) {}
}
```

```java
// api/ApiException.java
package com.example.otpsentinel.api;

public class ApiException extends RuntimeException {
  private final int status;
  private final String errorCode;
  private final String title;

  public ApiException(int status, String errorCode, String title, String detail) {
    super(detail);
    this.status = status;
    this.errorCode = errorCode;
    this.title = title;
  }

  public int status() { return status; }
  public String errorCode() { return errorCode; }
  public String title() { return title; }
}
```

```java
// api/dto/ProblemDetailsDto.java
package com.example.otpsentinel.api.dto;

public record ProblemDetailsDto(
    String type, String title, int status, String detail, String instance,
    String correlationId, String errorCode) {}
```

```java
// api/GlobalExceptionHandler.java
package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.ProblemDetailsDto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.NoSuchElementException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ProblemDetailsDto> handleApiException(ApiException e, HttpServletRequest request) {
    return problem(e.status(), e.title(), e.getMessage(), e.errorCode(), request);
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ProblemDetailsDto> handleNotFound(NoSuchElementException e, HttpServletRequest request) {
    return problem(404, "Investigation not found", e.getMessage(), "INVESTIGATION_NOT_FOUND", request);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ProblemDetailsDto> handleConflict(IllegalStateException e, HttpServletRequest request) {
    return problem(409, "Investigation not ready for decision", e.getMessage(),
        "INVESTIGATION_NOT_ACTIONABLE", request);
  }

  private ResponseEntity<ProblemDetailsDto> problem(
      int status, String title, String detail, String errorCode, HttpServletRequest request) {
    String correlationId = (String) request.getAttribute("correlationId");
    ProblemDetailsDto body = new ProblemDetailsDto(
        "https://errors.example.local/" + errorCode.toLowerCase().replace('_', '-'),
        title, status, detail, request.getRequestURI(), correlationId, errorCode);
    return ResponseEntity.status(status).body(body);
  }
}
```

```java
// config/CorrelationIdFilter.java
package com.example.otpsentinel.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String correlationId = request.getHeader("X-Correlation-Id");
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = "corr-" + UUID.randomUUID();
    }
    request.setAttribute("correlationId", correlationId);
    response.setHeader("X-Correlation-Id", correlationId);
    response.setHeader("X-Demo-Mode", "true");
    chain.doFilter(request, response);
  }
}
```

```java
// api/InvestigationRequestValidator.java
package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.InvestigationRequestDto;
import com.example.otpsentinel.domain.TimeWindow;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public final class InvestigationRequestValidator {

  private static final Set<String> ALLOWED_LOCALES = Set.of("tr-TR", "en-US");

  public TimeWindow validate(InvestigationRequestDto request) {
    if (request.question() == null
        || request.question().length() < 10
        || request.question().length() > 1000) {
      throw new ApiException(400, "INVALID_REQUEST", "Invalid request",
          "question must be between 10 and 1000 characters");
    }
    if (request.locale() != null && !ALLOWED_LOCALES.contains(request.locale())) {
      throw new ApiException(400, "INVALID_REQUEST", "Invalid request",
          "locale is not in the allowlist: " + request.locale());
    }
    if (request.timeWindow() == null) {
      throw new ApiException(400, "INVALID_TIME_WINDOW", "Invalid time window",
          "timeWindow is required (relative-time resolution is out of scope for M7)");
    }
    Instant startAt = request.timeWindow().startAt();
    Instant endAt = request.timeWindow().endAt();
    Instant now = Instant.now();
    if (endAt.isAfter(now) || startAt.isAfter(now)) {
      throw new ApiException(400, "INVALID_TIME_WINDOW", "Invalid time window",
          "time window must not end in the future");
    }
    try {
      return new TimeWindow(startAt, endAt);
    } catch (IllegalArgumentException e) {
      throw new ApiException(400, "INVALID_TIME_WINDOW", "Invalid time window", e.getMessage());
    }
  }
}
```

`TimeWindow`'s own constructor already enforces the 1 min–24 h bounds (`docs/03` "aralık: 1 dakika–24
saat"), so `InvestigationRequestValidator` doesn't duplicate that arithmetic — it just translates the
`IllegalArgumentException` into the `400 INVALID_TIME_WINDOW` problem-details response.

- [ ] **Step 1: Write the failing tests**

```java
package com.example.otpsentinel.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.otpsentinel.api.dto.InvestigationRequestDto;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class InvestigationRequestValidatorTest {

  private final InvestigationRequestValidator validator = new InvestigationRequestValidator();

  @Test
  void rejectsFutureEndTime() {
    InvestigationRequestDto request = new InvestigationRequestDto(
        "why did OTP success rate drop suddenly",
        new InvestigationRequestDto.TimeWindowRangeDto(
            Instant.now().minus(10, ChronoUnit.MINUTES), Instant.now().plus(5, ChronoUnit.MINUTES)),
        "tr-TR");

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(ApiException.class)
        .extracting(e -> ((ApiException) e).errorCode())
        .isEqualTo("INVALID_TIME_WINDOW");
  }

  @Test
  void rejectsIntervalLongerThan24Hours() {
    Instant end = Instant.now().minus(1, ChronoUnit.MINUTES);
    InvestigationRequestDto request = new InvestigationRequestDto(
        "why did OTP success rate drop suddenly",
        new InvestigationRequestDto.TimeWindowRangeDto(end.minus(25, ChronoUnit.HOURS), end), "tr-TR");

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(ApiException.class)
        .extracting(e -> ((ApiException) e).errorCode())
        .isEqualTo("INVALID_TIME_WINDOW");
  }

  @Test
  void acceptsAValidWindow() {
    Instant end = Instant.now().minus(1, ChronoUnit.MINUTES);
    InvestigationRequestDto request = new InvestigationRequestDto(
        "why did OTP success rate drop suddenly",
        new InvestigationRequestDto.TimeWindowRangeDto(end.minus(15, ChronoUnit.MINUTES), end), "tr-TR");

    assertThat(validator.validate(request)).isNotNull();
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=InvestigationRequestValidatorTest`
Expected: compile errors (classes don't exist).

- [ ] **Step 3: Implement**

Create all files listed above exactly as specified (plus the remaining DTOs —
`InvestigationResponseDto`/`IncidentDraftPreviewDto`/`IncidentDraftDecisionRequestDto`/
`IncidentDraftDecisionResponseDto` — needed by Tasks 6/9/10; define them now since they have no
behavior to test standalone):

```java
// api/dto/InvestigationResponseDto.java
package com.example.otpsentinel.api.dto;

import java.util.List;

public record InvestigationResponseDto(
    String investigationId, String status, String severity, String summary,
    TimeWindowDto timeWindow, List<EvidenceDto> evidence, List<HypothesisDto> hypotheses,
    List<RecommendedActionDto> recommendedActions, List<KnowledgeReferenceDto> knowledgeReferences,
    double confidence, boolean approvalRequired, ValidationDto validation) {

  public record EvidenceDto(
      String id, String sourceType, String sourceReference, String observation, String observedAt) {}

  public record HypothesisDto(
      int rank, String possibleCause, String probability, List<String> supportingEvidenceIds,
      List<String> verificationSteps) {}

  public record RecommendedActionDto(
      String actionType, String description, String risk, boolean requiresApproval) {}

  public record KnowledgeReferenceDto(String documentId) {}

  public record ValidationDto(String status, List<String> warnings) {}
}
```

```java
// api/dto/IncidentDraftPreviewDto.java
package com.example.otpsentinel.api.dto;

import java.util.List;

public record IncidentDraftPreviewDto(
    String title, String severity, String summary, int evidenceCount,
    List<String> recommendedChecks, boolean requiresExplicitApproval) {}
```

```java
// api/dto/IncidentDraftDecisionRequestDto.java
package com.example.otpsentinel.api.dto;

public record IncidentDraftDecisionRequestDto(String decision, String reason) {}
```

```java
// api/dto/IncidentDraftDecisionResponseDto.java
package com.example.otpsentinel.api.dto;

public record IncidentDraftDecisionResponseDto(
    String incidentDraftId, String externalIncidentId, String status, boolean idempotentReplay) {}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -B test -Dtest=InvestigationRequestValidatorTest`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/otpsentinel/api src/main/java/com/example/otpsentinel/config/CorrelationIdFilter.java src/test/java/com/example/otpsentinel/api/InvestigationRequestValidatorTest.java
git commit -m "feat(m7): API DTOs, request validator, problem-details error handling, correlation-id filter"
```

---

## Task 6: POST /api/v1/investigations + GET /api/v1/investigations/{id}

**Files:**
- Create: `src/main/java/com/example/otpsentinel/api/InvestigationController.java`
- Test: `src/test/java/com/example/otpsentinel/api/InvestigationControllerTest.java`

**Interfaces:**
- Consumes: `InvestigationOrchestrator.runInvestigation(...)`/`.findInvestigation(...)` (Task 4),
  `InvestigationRequestValidator.validate(...)` (Task 5), all DTOs (Task 5).

```java
package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.InvestigationRequestDto;
import com.example.otpsentinel.api.dto.InvestigationResponseDto;
import com.example.otpsentinel.api.dto.TimeWindowDto;
import com.example.otpsentinel.config.InvestigationOrchestrator;
import com.example.otpsentinel.domain.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/investigations")
public class InvestigationController {

  private final InvestigationOrchestrator orchestrator;
  private final InvestigationRequestValidator validator = new InvestigationRequestValidator();

  public InvestigationController(InvestigationOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  @PostMapping
  public ResponseEntity<InvestigationResponseDto> create(
      @RequestBody InvestigationRequestDto request, HttpServletRequest httpRequest) {
    TimeWindow window = validator.validate(request);
    String correlationId = (String) httpRequest.getAttribute("correlationId");
    Investigation outcome = orchestrator.runInvestigation(request.question(), window, correlationId);
    return ResponseEntity.ok(toDto(outcome));
  }

  @GetMapping("/{id}")
  public ResponseEntity<InvestigationResponseDto> get(@PathVariable String id) {
    Investigation investigation = orchestrator.findInvestigation(InvestigationId.of(id))
        .orElseThrow(() -> new NoSuchElementException("investigation not found: " + id));
    return ResponseEntity.ok(toDto(investigation));
  }

  private static InvestigationResponseDto toDto(Investigation i) {
    return new InvestigationResponseDto(
        i.id().toString(),
        i.resultStatus() == null ? null : i.resultStatus().name(),
        i.severity() == null ? null : i.severity().name(),
        summary(i),
        new TimeWindowDto(i.resolvedTimeWindow().startAt(), i.resolvedTimeWindow().endAt(), "UTC"),
        i.evidence().stream().map(InvestigationController::toEvidenceDto).toList(),
        i.hypotheses().stream().map(InvestigationController::toHypothesisDto).toList(),
        i.recommendedActions().stream().map(InvestigationController::toActionDto).toList(),
        i.knowledgeReferences().stream()
            .map(InvestigationResponseDto.KnowledgeReferenceDto::new).toList(),
        i.confidence() == null ? 0.0 : i.confidence(),
        !i.recommendedActions().isEmpty()
            && i.recommendedActions().stream().anyMatch(RecommendedAction::requiresApproval),
        i.validationReport() == null ? null
            : new InvestigationResponseDto.ValidationDto(
                i.validationReport().status().name(), i.validationReport().warnings()));
  }

  private static String summary(Investigation i) {
    return i.resultStatus() == null ? "investigation in progress" : i.resultStatus().name();
  }

  private static InvestigationResponseDto.EvidenceDto toEvidenceDto(Evidence e) {
    return new InvestigationResponseDto.EvidenceDto(
        e.id(), e.sourceType(), e.sourceReference(), e.observation(), e.observedAt().toString());
  }

  private static InvestigationResponseDto.HypothesisDto toHypothesisDto(Hypothesis h) {
    return new InvestigationResponseDto.HypothesisDto(
        h.rank(), h.possibleCause(), String.valueOf(h.probability()), h.supportingEvidenceIds(),
        h.verificationSteps());
  }

  private static InvestigationResponseDto.RecommendedActionDto toActionDto(RecommendedAction a) {
    return new InvestigationResponseDto.RecommendedActionDto(
        a.actionType().name(), a.description(), a.risk().name(), a.requiresApproval());
  }
}
```

Implementer note: `docs/06`'s example response has `severity`/`hypotheses[].probability` as bare
strings/numbers with specific shapes — keep the mapping literal-JSON-compatible (Jackson serializes
records fine by default; no custom serializer needed for M7).

- [ ] **Step 1: Write the failing tests**

`InvestigationControllerTest.java` — `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`,
combined with `AbstractPostgresIntegrationTest`'s Testcontainers datasource (this is the project's first
full-stack web test; follow the pattern note from the M7-prompt research: subclass
`AbstractPostgresIntegrationTest` but override `webEnvironment` via a second `@SpringBootTest` on the
subclass — Spring allows narrowing test annotations on a subclass):

```java
package com.example.otpsentinel.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InvestigationControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void rejectsFutureTimeWindowWith400() {
    String body = """
        {"question":"why did OTP success rate drop suddenly today",
         "timeWindow":{"startAt":"%s","endAt":"%s"}}
        """.formatted(Instant.now().minusSeconds(60), Instant.now().plusSeconds(300));
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> response = restTemplate.postForEntity(
        "/api/v1/investigations", new HttpEntity<>(body, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("INVALID_TIME_WINDOW");
  }

  @Test
  void rejectsIntervalLongerThan24HoursWith400() {
    Instant end = Instant.now().minus(1, ChronoUnit.MINUTES);
    Instant start = end.minus(25, ChronoUnit.HOURS);
    String body = """
        {"question":"why did OTP success rate drop suddenly today",
         "timeWindow":{"startAt":"%s","endAt":"%s"}}
        """.formatted(start, end);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> response = restTemplate.postForEntity(
        "/api/v1/investigations", new HttpEntity<>(body, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("INVALID_TIME_WINDOW");
  }

  @Test
  void investigatesTheOtpDropOneOhOneFixtureAndAllowsRefetch() {
    String body = """
        {"question":"why did OTP success rate drop",
         "timeWindow":{"startAt":"2026-07-30T11:15:00Z","endAt":"2026-07-30T11:30:00Z"}}
        """;
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> created = restTemplate.postForEntity(
        "/api/v1/investigations", new HttpEntity<>(body, headers), String.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(created.getBody()).contains("ANOMALY_CONFIRMED");
    String investigationId = created.getBody().split("\"investigationId\":\"")[1].split("\"")[0];

    ResponseEntity<String> fetched = restTemplate.getForEntity(
        "/api/v1/investigations/" + investigationId, String.class);

    assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(fetched.getBody()).contains("ANOMALY_CONFIRMED");
  }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -B test -Dtest=InvestigationControllerTest`
Expected: compile error / 404s (controller doesn't exist yet).

- [ ] **Step 3: Implement `InvestigationController.java`** as specified above.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -B test -Dtest=InvestigationControllerTest`
Expected: BUILD SUCCESS. If the 3rd test's window doesn't match `OtpDropOneOhOneScript`'s hard-coded
arguments exactly, debug against Task 3's script content (Design decision #5 — this is the one window/
question combination guaranteed to fully complete).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/otpsentinel/api/InvestigationController.java src/test/java/com/example/otpsentinel/api/InvestigationControllerTest.java
git commit -m "feat(m7): POST/GET /api/v1/investigations"
```

---

## Task 7: Incident-draft preview endpoint

**Files:**
- Create: `src/main/java/com/example/otpsentinel/api/IncidentDraftController.java` (preview handler only
  in this task; Task 8 adds the decisions handler to the same file)
- Test: `src/test/java/com/example/otpsentinel/api/IncidentDraftPreviewControllerTest.java`

**Interfaces:**
- Consumes: `InvestigationOrchestrator.previewIncidentDraft(InvestigationId, String correlationId)` (Task
  4), `IncidentDraftPreviewDto` (Task 5).

```java
package com.example.otpsentinel.api;

import com.example.otpsentinel.api.dto.IncidentDraftPreviewDto;
import com.example.otpsentinel.config.InvestigationOrchestrator;
import com.example.otpsentinel.domain.InvestigationId;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/investigations/{investigationId}/incident-draft")
public class IncidentDraftController {

  private final InvestigationOrchestrator orchestrator;

  public IncidentDraftController(InvestigationOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  @PostMapping("/preview")
  public ResponseEntity<IncidentDraftPreviewDto> preview(
      @PathVariable String investigationId, HttpServletRequest request) {
    String correlationId = (String) request.getAttribute("correlationId");
    var preview = orchestrator.previewIncidentDraft(InvestigationId.of(investigationId), correlationId);
    return ResponseEntity.ok(new IncidentDraftPreviewDto(
        preview.title(), preview.severity().name(), preview.summary(), preview.evidenceCount(),
        preview.recommendedChecks(), preview.requiresExplicitApproval()));
  }
}
```

- [ ] **Step 1: Write the failing test**

`IncidentDraftPreviewControllerTest.java` — extends `AbstractPostgresIntegrationTest` +
`@SpringBootTest(webEnvironment = RANDOM_PORT)`, same pattern as Task 6. Seed the DB directly via
`newInvestigationRepository().save(...)` with a `Investigation` driven to `COMPLETED` (build via
`Investigation.receive(...)` → `startCollectingEvidence()` → `addEvidence(...)` → `startGeneratingAnalysis()`
→ `proposeAnalysis(...)` → `startValidating()` → `complete(InvestigationStatus.ANOMALY_CONFIRMED, ValidationReport.passed(List.of()))`
— this exact sequence is already proven in `JdbcInvestigationRepositoryTest`, copy its construction
helper if one exists there) — then call `POST /api/v1/investigations/{id}/incident-draft/preview` and
assert `200` + `requiresExplicitApproval=true` + no `incident_draft` row exists
(`jdbcTemplate.queryForObject("SELECT count(*) FROM incident_draft", Integer.class)` is `0`):

```java
package com.example.otpsentinel.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import com.example.otpsentinel.domain.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IncidentDraftPreviewControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  private Investigation completedInvestigation() {
    TimeWindow window = new TimeWindow(Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z"));
    Investigation investigation = Investigation.receive("why did OTP success rate drop", window, "v1", "v1");
    investigation.startCollectingEvidence();
    investigation.addEvidence(new Evidence(
        "ev-1", "TOOL_RESULT", "getOtpMetrics", "current 72.1%", Instant.now(),
        "otp_success_rate", 72.1, "percent"));
    investigation.addEvidence(new Evidence(
        "ev-2", "TOOL_RESULT", "getOtpMetrics", "previous 98.1%", Instant.now(),
        "otp_success_rate", 98.1, "percent"));
    investigation.startGeneratingAnalysis();
    investigation.proposeAnalysis(
        Severity.HIGH,
        java.util.List.of(new Hypothesis(1, "connection pool exhaustion", 0.7,
            java.util.List.of("ev-1"), java.util.List.of(), java.util.List.of("check pool metrics"))),
        java.util.List.of(), java.util.List.of(), 0.85);
    investigation.startValidating();
    investigation.complete(InvestigationStatus.ANOMALY_CONFIRMED, ValidationReport.passed(java.util.List.of()));
    return investigation;
  }

  @Test
  void previewDoesNotPersistAnIncidentDraft() {
    Investigation investigation = completedInvestigation();
    newInvestigationRepository().save(investigation);

    ResponseEntity<String> response = restTemplate.postForEntity(
        "/api/v1/investigations/" + investigation.id() + "/incident-draft/preview", null, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"requiresExplicitApproval\":true");
    Integer draftCount = jdbcTemplate.queryForObject("SELECT count(*) FROM incident_draft", Integer.class);
    assertThat(draftCount).isZero();
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=IncidentDraftPreviewControllerTest`
Expected: compile error / 404 (controller doesn't exist).

- [ ] **Step 3: Implement `IncidentDraftController.java`** (preview handler only) as specified above.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -B test -Dtest=IncidentDraftPreviewControllerTest`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/otpsentinel/api/IncidentDraftController.java src/test/java/com/example/otpsentinel/api/IncidentDraftPreviewControllerTest.java
git commit -m "feat(m7): POST incident-draft/preview (no persistence, AC-013)"
```

---

## Task 8: Incident-draft decisions endpoint (approve / reject / idempotent replay)

**Files:**
- Modify: `src/main/java/com/example/otpsentinel/api/IncidentDraftController.java`
- Test: `src/test/java/com/example/otpsentinel/api/IncidentDraftDecisionControllerTest.java`

**Interfaces:**
- Consumes: `InvestigationOrchestrator.decide(InvestigationId, String decision, String reason, String idempotencyKey, String correlationId)`
  (Task 4), `IncidentDraftDecisionRequestDto`/`IncidentDraftDecisionResponseDto` (Task 5).

```java
  @PostMapping("/decisions")
  public ResponseEntity<IncidentDraftDecisionResponseDto> decide(
      @PathVariable String investigationId,
      @RequestBody IncidentDraftDecisionRequestDto request,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      HttpServletRequest httpRequest) {
    String correlationId = (String) httpRequest.getAttribute("correlationId");
    var outcome = orchestrator.decide(
        InvestigationId.of(investigationId), request.decision(), request.reason(), idempotencyKey,
        correlationId);
    var body = new IncidentDraftDecisionResponseDto(
        outcome.incidentDraftId().toString(), outcome.externalIncidentId(),
        outcome.status().name(), outcome.idempotentReplay());
    var status = outcome.idempotentReplay() ? org.springframework.http.HttpStatus.OK
        : org.springframework.http.HttpStatus.CREATED;
    return ResponseEntity.status(status).body(body);
  }
```

Add the needed imports (`IncidentDraftDecisionRequestDto`, `IncidentDraftDecisionResponseDto`,
`RequestHeader`) to `IncidentDraftController.java`.

- [ ] **Step 1: Write the failing tests**

`IncidentDraftDecisionControllerTest.java` — same base pattern as Task 7's test (reuse its
`completedInvestigation()` helper by copying it into this file too, since MockMvc/RestTemplate tests in
this repo don't share helper classes across files per the existing test style):

```java
package com.example.otpsentinel.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import com.example.otpsentinel.domain.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IncidentDraftDecisionControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  private Investigation completedInvestigation() {
    TimeWindow window = new TimeWindow(Instant.parse("2026-07-30T11:15:00Z"), Instant.parse("2026-07-30T11:30:00Z"));
    Investigation investigation = Investigation.receive("why did OTP success rate drop", window, "v1", "v1");
    investigation.startCollectingEvidence();
    investigation.addEvidence(new Evidence(
        "ev-1", "TOOL_RESULT", "getOtpMetrics", "current 72.1%", Instant.now(),
        "otp_success_rate", 72.1, "percent"));
    investigation.addEvidence(new Evidence(
        "ev-2", "TOOL_RESULT", "getOtpMetrics", "previous 98.1%", Instant.now(),
        "otp_success_rate", 98.1, "percent"));
    investigation.startGeneratingAnalysis();
    investigation.proposeAnalysis(
        Severity.HIGH,
        List.of(new Hypothesis(1, "connection pool exhaustion", 0.7,
            List.of("ev-1"), List.of(), List.of("check pool metrics"))),
        List.of(), List.of(), 0.85);
    investigation.startValidating();
    investigation.complete(InvestigationStatus.ANOMALY_CONFIRMED, ValidationReport.passed(List.of()));
    return investigation;
  }

  private ResponseEntity<String> decide(String investigationId, String key, String decision, String reason) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Idempotency-Key", key);
    String body = reason == null
        ? "{\"decision\":\"%s\"}".formatted(decision)
        : "{\"decision\":\"%s\",\"reason\":\"%s\"}".formatted(decision, reason);
    return restTemplate.postForEntity(
        "/api/v1/investigations/" + investigationId + "/incident-draft/decisions",
        new HttpEntity<>(body, headers), String.class);
  }

  @Test
  void approvalCreatesExactlyOneIncident() {
    Investigation investigation = completedInvestigation();
    newInvestigationRepository().save(investigation);

    ResponseEntity<String> response = decide(
        investigation.id().toString(), "idem-001", "APPROVE", "Teknik ekip incelemesi için gerekli.");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).contains("\"status\":\"CREATED\"").contains("\"idempotentReplay\":false");
    Integer draftCount = jdbcTemplate.queryForObject("SELECT count(*) FROM incident_draft", Integer.class);
    assertThat(draftCount).isEqualTo(1);
  }

  @Test
  void replayedApprovalReturnsOriginalIdAndNoSecondIncident() {
    Investigation investigation = completedInvestigation();
    newInvestigationRepository().save(investigation);
    ResponseEntity<String> first = decide(investigation.id().toString(), "idem-002", "APPROVE", "reason");
    String firstId = first.getBody().split("\"incidentDraftId\":\"")[1].split("\"")[0];

    ResponseEntity<String> second = decide(investigation.id().toString(), "idem-002", "APPROVE", "reason");

    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(second.getBody()).contains("\"idempotentReplay\":true").contains(firstId);
    Integer draftCount = jdbcTemplate.queryForObject("SELECT count(*) FROM incident_draft", Integer.class);
    assertThat(draftCount).isEqualTo(1);
  }

  @Test
  void rejectionCreatesNoIncident() {
    Investigation investigation = completedInvestigation();
    newInvestigationRepository().save(investigation);

    ResponseEntity<String> response = decide(
        investigation.id().toString(), "idem-003", "REJECT", "Known maintenance");

    assertThat(response.getBody()).contains("\"status\":\"REJECTED\"");
    Integer createdCount = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM incident_draft WHERE external_incident_id IS NOT NULL", Integer.class);
    assertThat(createdCount).isZero();
  }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -B test -Dtest=IncidentDraftDecisionControllerTest`
Expected: 404s / compile error (`/decisions` handler doesn't exist).

- [ ] **Step 3: Implement** the `/decisions` handler in `IncidentDraftController.java` as specified above.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -B test -Dtest=IncidentDraftDecisionControllerTest`
Expected: BUILD SUCCESS. If the replay test fails because `DataIntegrityViolationException` isn't thrown
(e.g. driver wraps it differently), inspect the actual exception type thrown by
`JdbcIncidentDraftRepository.save(...)` on a `uq_incident_draft_idempotency_key` violation under this
project's Spring Boot version (3.3.5 — `DataIntegrityViolationException` is the standard translation) and
adjust the `catch` clause in `InvestigationOrchestrator.decide(...)` (Task 4) accordingly; this is a
one-line fix co-located with this task's review.

- [ ] **Step 5: Run the full `docs/12` "Human approval" + "API validation" set together**

Run: `mvn -B test -Dtest=IncidentDraftPreviewControllerTest,IncidentDraftDecisionControllerTest,InvestigationControllerTest`
Expected: BUILD SUCCESS — this is the M7 acceptance criterion (6 scenarios, all green).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/otpsentinel/api/IncidentDraftController.java src/test/java/com/example/otpsentinel/api/IncidentDraftDecisionControllerTest.java
git commit -m "feat(m7): POST incident-draft/decisions with DB-enforced idempotent replay (AC-013/014/015)"
```

---

## Task 9: Remaining error codes (422/429/502/504) + full verify + docs

**Files:**
- Modify: `src/main/java/com/example/otpsentinel/api/GlobalExceptionHandler.java`
- Modify: `src/main/java/com/example/otpsentinel/api/InvestigationController.java` (map specific
  exceptions from the orchestrator to `422`/`502`/`504`)
- Modify: `docs/17-traceability-risk-dod.md`
- Test: extend `InvestigationControllerTest.java` with one case demonstrating the mapping exists (the
  literal 429/502/504 triggers require conditions this MVP can't easily produce deterministically —
  document that in the report rather than fabricate a fake trigger path; write only the test that's
  actually reachable).

**Design:** `IncidentAnalysisAiService.analyze(...)` failures currently surface as
`investigation.fail(...)` inside `IncidentInvestigationService` (an already-completed, non-throwing
outcome) — by design (AI-007 "başarılı sonuç uydurulmamalı", not "throw an HTTP error"). So `502
MODEL_PROVIDER_ERROR` / `504 INVESTIGATION_TIMEOUT` per `docs/06` describe transport-level failures
talking to a live model (connection refused, provider timeout), which only occur in `AI_MODE=live` and are
not exercised by the offline test suite (AGENTS.md: "must not require internet access, a live LLM"). Add
the mapping so the code path exists and is correct, but do not fabricate a fake live-model failure just to
exercise it — note this explicitly as a documented gap in the M7 report (same treatment M6 gave
correlation-wording).

```java
  @ExceptionHandler(dev.langchain4j.exception.HttpException.class)
  public ResponseEntity<ProblemDetailsDto> handleModelProviderError(
      dev.langchain4j.exception.HttpException e, HttpServletRequest request) {
    return problem(502, "Model provider error", e.getMessage(), "MODEL_PROVIDER_ERROR", request);
  }

  @ExceptionHandler(java.util.concurrent.TimeoutException.class)
  public ResponseEntity<ProblemDetailsDto> handleTimeout(
      java.util.concurrent.TimeoutException e, HttpServletRequest request) {
    return problem(504, "Investigation timed out", e.getMessage(), "INVESTIGATION_TIMEOUT", request);
  }
```

Implementer note: verify `dev.langchain4j.exception.HttpException` is the actual exception type LangChain4j
1.18.1's `OpenAiChatModel` throws on an HTTP error (check the langchain4j-open-ai jar / existing imports in
the codebase; if the type differs, use the correct one — do not guess blindly, `grep` the dependency jar or
check LangChain4j 1.18 release notes referenced elsewhere in `docs/16` if present).

`422 QUESTION_NOT_ACTIONABLE` is a genuinely reachable, testable case: it fires when the model — even after
one repair attempt — cannot be coerced into a valid structured result for a well-formed but nonsensical
question. Given this MVP's deterministic-script stub model, the reachable trigger is: any question/window
that doesn't match `OtpDropOneOhOneScript`'s hard-coded expectations ends `FAILED` (Task 3 decision #5),
which is a `200` with `status: "FAILED"` today (a completed investigation, not a validation error) — so
`422` cannot be produced by the current stub without inventing a second scripted "unactionable" scenario.
Do not force this: document it as intentionally out of scope for the stub-only MVP path and move on
(YAGNI — no Gherkin scenario in `docs/12` requires it).

- [ ] **Step 1: Implement the two reachable-in-code (not reachable-in-CI) handlers** in
      `GlobalExceptionHandler.java` as shown above.

- [ ] **Step 2: Compile-check**

Run: `mvn -B compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Update `docs/17-traceability-risk-dod.md`**

Check off: `docker compose up --build` (only if Task 10 below confirms it — otherwise leave unchecked and
say why in the report), `Idempotency pass` (now true — Task 8 proved it), and add the M7 rows to the
traceability matrix:

```markdown
| US-011 Preview | FR-013 | AC-013 |
| US-012 Approval | FR-014, SEC-002 | AC-013, AC-015 |
| US-013 Idempotency | FR-015 | AC-014 |
```
(these rows already exist in the table — instead, add a note under "MVP release checklist" confirming
which boxes M7 newly satisfies, and leave `README quickstart`/`5-7 dakika demo`/`Secret scan temiz` for
M8 per `docs/14`'s milestone boundary.)

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/otpsentinel/api/GlobalExceptionHandler.java docs/17-traceability-risk-dod.md
git commit -m "feat(m7): map model-provider/timeout errors to problem-details; update DoD tracking"
```

---

## Task 10: Full verify, spotless, final acceptance pass

**Files:** none (verification only).

- [ ] **Step 1:** `mvn -B spotless:apply`
- [ ] **Step 2:** `mvn -B verify -Dsurefire.excludedGroups=local-live`

Expected: `BUILD SUCCESS`, all tests green (the full suite from M1–M6 plus every test added in Tasks 1–9).
Capture the exact `Tests run: N, Failures: 0, Errors: 0, Skipped: 0` line for the session report (per
`prompts/08-session-report.md`'s "gerçek komut + gerçek çıktı" rule — do not paraphrase).

- [ ] **Step 3:** Grep the verify log for accidental secret/PII leakage, same check M6 used:

```bash
grep -ciE "sk-abcdef|482913|555-123-4567" <verify-log-path>
```

Expected: `0`.

- [ ] **Step 4:** If time/scope allows, smoke-test `docker compose up --build` once (NFR-003/M7-prompt
scope note lists this under M8, so this is optional verification, not a blocking step for M7's DoD) —
report the outcome either way in the M7 report, do not claim it if not actually run.

- [ ] **Step 5:** No commit for this task (verification-only); if Steps 1–3 required fixes, those fixes
were already committed by their originating task.

---

## Self-review notes (already applied above)

- **Spec coverage:** FR-013 (Task 7), FR-014/SEC-002 (Task 8 approve), FR-015 (Task 8 replay), FR-016
  (Task 6 GET), FR-017 (Tasks 1/2/4), NFR-006 (`/api/v1` base path, Task 6/7), NFR-007 (Task 5
  problem-details), NFR-008 (existing `ToolBudgetGuard`, wired not re-built, Task 4), docs/12 "Human
  approval" 4 scenarios (Tasks 7/8), docs/12 "API validation" 2 scenarios (Task 6). AC-030 (GET snapshot,
  Task 6).
- **Placeholder scan:** no "TBD"/"handle appropriately" left; the two intentionally-undemonstrated error
  codes (429/502/504-live-only path) are explicitly justified, not hand-waved, and 502/504 handlers are
  still implemented (only their CI-test is out of scope, and that's stated).
- **Type consistency:** `InvestigationOrchestrator` method names/record shapes (`IncidentDraftPreview`,
  `DecisionOutcome`) match exactly between Task 4 (producer) and Tasks 7/8 (consumers). `EvidenceCollector`
  3-arg constructor signature matches between Task 1 (producer) and Task 4 (consumer,
  `InvestigationOrchestrator.runInvestigation`).
