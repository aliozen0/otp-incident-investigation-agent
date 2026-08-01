# M8 Bugfix: Stub Investigation Script Exhausted After One Use Per Running App

**Parked finding from:** M8 Task 4 live docker-compose verification
**Commit:** a23af20

## Symptom

Running the investigation-creation curl from the README's API walkthrough against a live `docker compose` stack succeeded on the first call but failed on every subsequent call against the same running container, returning `status: FAILED` ("structured output invalid...") instead of `ANOMALY_CONFIRMED` — with no indication that this was an app-lifecycle issue rather than a real investigation failure.

## Root Cause

`AgentConfig.chatModel(...)` was a singleton Spring `@Bean`. In stub mode (the default) it constructed one `StubChatModel` instance at app startup, wrapping a fixed `StubScript` with a mutable `stepIndex` field that only ever advances, never resets (`StubChatModel.java`). `InvestigationOrchestrator`, itself a singleton `@Service`, received that one `ChatModel` via constructor injection and reused the same instance for every `runInvestigation(...)` call for the app's entire lifetime. The first investigation against a running container consumed the whole scripted conversation; every later investigation on that same container threw `IllegalStateException("StubScript exhausted after N steps")`, which `IncidentInvestigationService.callWithRepair` caught as a generic failure.

This contradicted ADR-011's own stated reason for the deterministic stub (CI, offline demo, and reproducibility) and risked M8's core acceptance criterion: a demo must be re-runnable (rehearsal, or the interviewer asking to see it again) without breaking silently on the second run against a long-lived container. It was invisible to the existing 140-test suite because every test either constructs its own fresh `StubChatModel` directly, or runs in a short-lived Spring context that never serves two investigations. `ToolBudgetGuard` and `EvidenceCollector` were already constructed fresh per call inside `runInvestigation`; the `ChatModel` was the one shared exception.

## Fix

Replaced the shared `ChatModel` instance with a `java.util.function.Supplier<ChatModel>` factory, resolved once per `runInvestigation(...)` call:

- `InvestigationOrchestrator`: field `chatModel: ChatModel` -> `chatModelFactory: Supplier<ChatModel>`; constructor parameter renamed to match; inside `runInvestigation(...)`, `ChatModel chatModel = chatModelFactory.get();` is called right before `AiServices.builder(...).chatModel(chatModel)...` so every investigation gets its own instance (and, in stub mode, its own fresh `StubScript` with `stepIndex` reset to zero).
- `AgentConfig`: bean method `chatModel(...)` renamed to `chatModelFactory(...)`, return type changed from `ChatModel` to `Supplier<ChatModel>`; stub mode returns `() -> new StubChatModel(OtpDropOneOhOneScript.build())`, live mode returns `() -> OpenAiChatModel.builder()...build()`.

## Files Changed

1. `src/main/java/com/example/otpsentinel/config/InvestigationOrchestrator.java`
   - Added `import java.util.function.Supplier;`
   - Field and constructor parameter `chatModel` -> `chatModelFactory` (`Supplier<ChatModel>`)
   - `runInvestigation(...)`: added `ChatModel chatModel = chatModelFactory.get();` before the `AiServices.builder(...)` call

2. `src/main/java/com/example/otpsentinel/config/AgentConfig.java`
   - Added `import java.util.function.Supplier;`
   - `chatModel(...)` bean renamed to `chatModelFactory(...)`, returns `Supplier<ChatModel>` with lambda-wrapped construction for both stub and live modes

3. `src/test/java/com/example/otpsentinel/config/InvestigationOrchestratorTest.java`
   - Existing test updated to pass `() -> new StubChatModel(OtpDropOneOhOneScript.build())` instead of a plain instance
   - Added test: `secondInvestigationOnTheSameOrchestratorInstanceStillSucceeds()` — runs two investigations on the same orchestrator instance and asserts both reach `InvestigationPhase.COMPLETED`

4. `src/test/java/com/example/otpsentinel/config/AgentConfigTest.java` (unavoidable compile-time consequence of the bean rename, not separately in scope)
   - `selectsOfflineStubModelByDefaultMode()` / `selectsNvidiaCompatibleOpenAiModelInLiveMode()` updated to call `config.chatModelFactory(...).get()` instead of the removed `config.chatModel(...)`

## Test Evidence

New/updated tests, real `mvn test` output:

```
[INFO] Running com.example.otpsentinel.config.AgentConfigTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.261 s -- in com.example.otpsentinel.config.AgentConfigTest
[INFO] Running com.example.otpsentinel.config.InvestigationOrchestratorTest
...
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 15.23 s -- in com.example.otpsentinel.config.InvestigationOrchestratorTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] BUILD SUCCESS
```

Full suite, real `mvn -B spotless:apply verify` output:

```
[INFO] Results:
[INFO]
[INFO] Tests run: 141, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] --- spotless:2.43.0:check (spotless-check) @ otp-sentinel ---
[INFO] Spotless.Java is keeping 166 files clean - 0 needs changes to be clean, 0 were already clean, 166 were skipped because caching determined they were already clean
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

(141 = the pre-existing 140 plus the 1 new `secondInvestigationOnTheSameOrchestratorInstanceStillSucceeds` test.)

## Live End-to-End Proof

Brought up `docker compose up --build -d`, waited for the app to report healthy, then ran the exact investigation-creation curl from the README's API walkthrough **twice** against the same running container:

**Run 1:**
```json
{"investigationId":"acaded1d-8634-4f60-bd52-c708a6454991","status":"ANOMALY_CONFIRMED","severity":"HIGH","summary":"ANOMALY_CONFIRMED", ...
"validation":{"status":"PASSED","warnings":[]}}
```

**Run 2 (same container, second investigation):**
```json
{"investigationId":"86c3d9cf-c989-4daa-846d-da6315f8e43f","status":"ANOMALY_CONFIRMED","severity":"HIGH","summary":"ANOMALY_CONFIRMED", ...
"validation":{"status":"PASSED","warnings":[]}}
```

Both requests returned `ANOMALY_CONFIRMED` with `validation.status: PASSED` — the previous run's `FAILED`/"structured output invalid" symptom on the second call is gone. `docker compose down -v` afterward to clean up.

## Impact

- **Backward compatibility:** No breaking changes to the REST API. `AgentConfig.chatModel(...)` is renamed to `chatModelFactory(...)` — an internal Spring bean, not a public contract — and `InvestigationOrchestrator`'s constructor signature changes accordingly; both call sites (`AgentConfig`'s bean wiring and the test) were updated.
- **Demo reliability:** A running `docker compose` container can now serve any number of investigations across the app's lifetime without the stub's scripted conversation running out — restoring ADR-011's reproducibility guarantee and unblocking re-runnable demos (rehearsal or repeat requests during the interview).
