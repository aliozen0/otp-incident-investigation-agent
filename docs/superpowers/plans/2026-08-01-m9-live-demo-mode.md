# M9 — Live Demo Mode Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prove the system runs end-to-end in `AI_MODE=live` (real NVIDIA NIM chat + real pgvector RAG + real agentic tool-calling) instead of only isolated M4/M5 spikes, and make live demo mode self-contained (auto knowledge ingest, documented `.env`/README, dev-only CORS) so M10's frontend has a proven, runnable live backend to sit on.

**Architecture:** Add one idempotent `ApplicationRunner` bean (`KnowledgeAutoIngestRunner`) that ingests the 4 MVP knowledge fixtures on startup only when `AI_MODE=live`, guarded by an existence check per `(documentId, version)` so repeated restarts never duplicate rows. Add a `@Profile("dev")`-gated CORS `WebMvcConfigurer` bean for future Vite dev-server frontend work (no effect in the default/demo profile). Everything else in this milestone is documentation (README, `.env.example`) plus one manual, non-automated live verification run against `POST /api/v1/investigations` with a real `NVIDIA_API_KEY`, whose real output gets pasted into the session report — not a new automated test (NFR-004: main suite stays green without a key).

**Tech Stack:** Java 21, Spring Boot (`ApplicationRunner`, `WebMvcConfigurer`), LangChain4j (`OpenAiChatModel`/`OpenAiEmbeddingModel`, already wired), PostgreSQL + pgvector, Maven, JUnit 5, Testcontainers.

## Global Constraints

- Main suite (`mvn verify`) must stay green **without** `NVIDIA_API_KEY` (NFR-004). Any new live-only verification is manual/`local-live`-tagged, never part of the default Surefire run.
- The LLM still has no write access — `createIncidentDraft` stays out of the agent's tool set (do not add it).
- No new infrastructure/containers. CORS is Spring config only, no gateway.
- Run all `mvn` and `docker` commands from the repository root.
- Don't write frontend code (M10) or new domain/business rules — this milestone is live-mode plumbing + proof only.
- Follow existing code conventions: config beans build NVIDIA clients inline per `AI_MODE` branch (see `AgentConfig.chatModelFactory`/`knowledgeSearchPort` — do not introduce a new abstraction layer for this).

---

### Task 1: Idempotent knowledge auto-ingest on live startup

**Files:**
- Modify: `src/main/java/com/example/otpsentinel/rag/KnowledgeRepository.java`
- Modify: `src/main/java/com/example/otpsentinel/rag/JdbcKnowledgeRepository.java`
- Create: `src/main/java/com/example/otpsentinel/rag/KnowledgeAutoIngestRunner.java`
- Modify: `src/main/java/com/example/otpsentinel/config/AgentConfig.java`
- Create: `src/test/java/com/example/otpsentinel/rag/KnowledgeAutoIngestRunnerTest.java`

**Interfaces:**
- Consumes: `KnowledgeIngestionService.ingest(KnowledgeDocument)` (existing, `src/main/java/com/example/otpsentinel/rag/KnowledgeIngestionService.java`), `KnowledgeFixtureCatalog.mvpDocuments()` returning `List<KnowledgeDocument>` (existing, `src/main/java/com/example/otpsentinel/rag/fixtures/KnowledgeFixtureCatalog.java`), `KnowledgeDocument.documentId()`/`.version()` (existing record accessors).
- Produces: `KnowledgeRepository.existsDocument(String documentId, String version)` — later tasks/tests may reuse it. `KnowledgeAutoIngestRunner` — a Spring `ApplicationRunner` bean, constructed via `new KnowledgeAutoIngestRunner(KnowledgeIngestionService ingestionService, KnowledgeRepository repository, boolean enabled)`, `run(ApplicationArguments args)`.

- [ ] **Step 1: Write the failing test for `existsDocument`**

Add to a new file `src/test/java/com/example/otpsentinel/rag/KnowledgeAutoIngestRunnerTest.java`:

```java
package com.example.otpsentinel.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.otpsentinel.adapters.persistence.AbstractPostgresIntegrationTest;
import com.example.otpsentinel.rag.fixtures.KnowledgeFixtureCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

/**
 * Auto-ingest runs the MVP knowledge fixture set into a real pgvector-enabled Postgres
 * (Testcontainers), using the deterministic hash embedding test double (no NVIDIA_API_KEY needed
 * — same pattern as {@link JdbcKnowledgeRetrievalIntegrationTest}). Verifies the idempotency
 * contract required by prompts/handoff/M9-prompt.md: running twice never duplicates rows.
 */
class KnowledgeAutoIngestRunnerTest extends AbstractPostgresIntegrationTest {

  private static final int DIMENSION = 1024;

  private KnowledgeAutoIngestRunner newEnabledRunner() {
    JdbcKnowledgeRepository repository = new JdbcKnowledgeRepository(jdbcTemplate);
    KnowledgeIngestionService ingestionService =
        new KnowledgeIngestionService(
            new ContentSanitizer(),
            new Chunker(),
            new DeterministicHashEmbeddingService(DIMENSION),
            repository);
    return new KnowledgeAutoIngestRunner(ingestionService, repository, true);
  }

  @Test
  void firstRunIngestsAllFourMvpDocuments() throws Exception {
    newEnabledRunner().run(new DefaultApplicationArguments());

    Integer count =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM knowledge_document", Integer.class);
    assertThat(count).isEqualTo(KnowledgeFixtureCatalog.mvpDocuments().size());
  }

  @Test
  void secondRunDoesNotDuplicateRows() throws Exception {
    KnowledgeAutoIngestRunner runner = newEnabledRunner();
    runner.run(new DefaultApplicationArguments());
    runner.run(new DefaultApplicationArguments());

    Integer count =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM knowledge_document", Integer.class);
    assertThat(count).isEqualTo(KnowledgeFixtureCatalog.mvpDocuments().size());
  }

  @Test
  void disabledRunnerDoesNothing() throws Exception {
    JdbcKnowledgeRepository repository = new JdbcKnowledgeRepository(jdbcTemplate);
    KnowledgeIngestionService ingestionService =
        new KnowledgeIngestionService(
            new ContentSanitizer(), new Chunker(), new DeterministicHashEmbeddingService(DIMENSION), repository);
    KnowledgeAutoIngestRunner disabled = new KnowledgeAutoIngestRunner(ingestionService, repository, false);

    disabled.run(new DefaultApplicationArguments());

    Integer count =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM knowledge_document", Integer.class);
    assertThat(count).isZero();
  }
}
```

- [ ] **Step 2: Run test to verify it fails (compile error — types don't exist yet)**

Run from the repository root (`.env` is not needed for this Testcontainers test):
`mvn -B -q -Dtest=KnowledgeAutoIngestRunnerTest test`

Expected: compilation failure — `KnowledgeAutoIngestRunner` and `KnowledgeRepository.existsDocument` do not exist yet.

- [ ] **Step 3: Add `existsDocument` to the port**

Edit `src/main/java/com/example/otpsentinel/rag/KnowledgeRepository.java` to:

```java
package com.example.otpsentinel.rag;

import java.util.List;

/** Port: persists an ingested {@link KnowledgeDocument} and its embedded chunks. */
public interface KnowledgeRepository {

  void save(KnowledgeDocument document, List<EmbeddedChunk> chunks);

  /** Used by {@link KnowledgeAutoIngestRunner} to make startup ingestion idempotent. */
  boolean existsDocument(String documentId, String version);
}
```

- [ ] **Step 4: Implement `existsDocument` in the JDBC adapter**

Edit `src/main/java/com/example/otpsentinel/rag/JdbcKnowledgeRepository.java`, add after `save(...)`:

```java
  @Override
  public boolean existsDocument(String documentId, String version) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM knowledge_document WHERE document_id = ? AND version = ?",
            Integer.class,
            documentId,
            version);
    return count != null && count > 0;
  }
```

- [ ] **Step 5: Write `KnowledgeAutoIngestRunner`**

Create `src/main/java/com/example/otpsentinel/rag/KnowledgeAutoIngestRunner.java`:

```java
package com.example.otpsentinel.rag;

import com.example.otpsentinel.rag.fixtures.KnowledgeFixtureCatalog;
import java.util.Objects;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * Ingests the MVP knowledge fixture set (docs/15-demo-fixtures.md) on application startup so
 * {@code AI_MODE=live}'s pgvector RAG has real content without a manual step
 * (prompts/handoff/M9-prompt.md item 1). Idempotent: skips any document/version already present,
 * so repeated {@code docker compose up} restarts never duplicate rows. A no-op ({@code enabled =
 * false}) in stub mode, where {@link KnowledgeSearchPort} is the fixture-backed
 * {@code FixtureKnowledgeSearchPort} and pgvector content is irrelevant.
 */
public final class KnowledgeAutoIngestRunner implements ApplicationRunner {

  private final KnowledgeIngestionService ingestionService;
  private final KnowledgeRepository repository;
  private final boolean enabled;

  public KnowledgeAutoIngestRunner(
      KnowledgeIngestionService ingestionService, KnowledgeRepository repository, boolean enabled) {
    this.ingestionService = Objects.requireNonNull(ingestionService, "ingestionService must not be null");
    this.repository = Objects.requireNonNull(repository, "repository must not be null");
    this.enabled = enabled;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!enabled) {
      return;
    }
    for (KnowledgeDocument document : KnowledgeFixtureCatalog.mvpDocuments()) {
      if (!repository.existsDocument(document.documentId(), document.version())) {
        ingestionService.ingest(document);
      }
    }
  }
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `mvn -B -Dtest=KnowledgeAutoIngestRunnerTest test`
Expected: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`.

- [ ] **Step 7: Wire the runner as a Spring bean in `AgentConfig`**

Edit `src/main/java/com/example/otpsentinel/config/AgentConfig.java`. Add imports:

```java
import com.example.otpsentinel.rag.ContentSanitizer;
import com.example.otpsentinel.rag.Chunker;
import com.example.otpsentinel.rag.EmbeddingService;
import com.example.otpsentinel.rag.JdbcKnowledgeRepository;
import com.example.otpsentinel.rag.KnowledgeAutoIngestRunner;
import com.example.otpsentinel.rag.KnowledgeIngestionService;
```

Add a new `@Bean` method (place after `knowledgeSearchPort`):

```java
  @Bean
  public KnowledgeAutoIngestRunner knowledgeAutoIngestRunner(
      @Value("${AI_MODE:stub}") String aiMode,
      JdbcTemplate jdbcTemplate,
      @Value("${NVIDIA_BASE_URL:https://integrate.api.nvidia.com/v1}") String baseUrl,
      @Value("${NVIDIA_API_KEY:}") String apiKey,
      @Value("${NVIDIA_EMBEDDING_MODEL:}") String embeddingModel) {
    boolean live = "live".equalsIgnoreCase(aiMode);
    JdbcKnowledgeRepository repository = new JdbcKnowledgeRepository(jdbcTemplate);
    EmbeddingService embeddingService =
        live
            ? new NvidiaNimEmbeddingService(baseUrl, apiKey, embeddingModel, 1024)
            : new DisabledEmbeddingService();
    KnowledgeIngestionService ingestionService =
        new KnowledgeIngestionService(new ContentSanitizer(), new Chunker(), embeddingService, repository);
    return new KnowledgeAutoIngestRunner(ingestionService, repository, live);
  }
```

`DisabledEmbeddingService` avoids constructing a real `NvidiaNimEmbeddingService` (which needs a base URL/key) in stub mode purely to satisfy the constructor — it is never invoked because `enabled=false` short-circuits `run()` before any embedding call. Add this small private static nested class at the bottom of `AgentConfig`, inside the `AgentConfig` class body:

```java
  /** Never invoked — {@link KnowledgeAutoIngestRunner#run} short-circuits when disabled (stub mode). */
  private static final class DisabledEmbeddingService implements EmbeddingService {
    @Override
    public java.util.List<Double> embed(String text, com.example.otpsentinel.rag.EmbeddingInputType inputType) {
      throw new UnsupportedOperationException("stub mode never ingests knowledge documents");
    }

    @Override
    public String modelId() {
      return "disabled";
    }
  }
```

First check `EmbeddingService`'s exact method signatures in `src/main/java/com/example/otpsentinel/rag/EmbeddingService.java` before writing this — match them exactly (the two methods above are from memory of `KnowledgeIngestionService`'s usage: `embeddingService.embed(c.content(), EmbeddingInputType.PASSAGE)` and `embeddingService.modelId()`).

- [ ] **Step 8: Run the full test suite**

Run: `mvn -B spotless:apply && mvn -B verify -Dsurefire.excludedGroups=local-live`
Expected: `BUILD SUCCESS`, same test count as before + 3 new tests, no `NVIDIA_API_KEY` needed (stub-mode bean construction never touches the network — verify `AgentConfigTest` in `src/test/java/com/example/otpsentinel/config/AgentConfigTest.java` still passes; if it asserts the exact bean set/shape, extend it to also assert `knowledgeAutoIngestRunner` bean exists and is disabled when `AI_MODE=stub`/unset).

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/example/otpsentinel/rag/KnowledgeRepository.java \
  src/main/java/com/example/otpsentinel/rag/JdbcKnowledgeRepository.java \
  src/main/java/com/example/otpsentinel/rag/KnowledgeAutoIngestRunner.java \
  src/main/java/com/example/otpsentinel/config/AgentConfig.java \
  src/test/java/com/example/otpsentinel/rag/KnowledgeAutoIngestRunnerTest.java
git commit -m "feat(rag): idempotent knowledge auto-ingest on live startup"
```

---

### Task 2: Dev-only CORS for future frontend dev server

**Files:**
- Create: `src/main/java/com/example/otpsentinel/config/DevCorsConfig.java`
- Create: `src/test/java/com/example/otpsentinel/config/DevCorsConfigTest.java`

**Interfaces:**
- Consumes: nothing new — standard Spring `WebMvcConfigurer`.
- Produces: nothing consumed by later tasks; this is a leaf.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/example/otpsentinel/config/DevCorsConfigTest.java`:

```java
package com.example.otpsentinel.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * ADR-016: CORS must be off by default (same-origin embedded frontend in the demo/production
 * image) and only active under the {@code dev} Spring profile (Vite dev server on a different
 * port during frontend development, M10).
 */
class DevCorsConfigTest {

  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
  @TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:corsdefaulttest")
  static class DefaultProfile {
    @org.springframework.beans.factory.annotation.Autowired ApplicationContext context;

    @Test
    void corsConfigBeanIsAbsentByDefault() {
      assertThat(context.getBeanNamesForType(DevCorsConfig.class)).isEmpty();
    }
  }

  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
  @ActiveProfiles("dev")
  @TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:corsdevtest")
  static class DevProfile {
    @org.springframework.beans.factory.annotation.Autowired ApplicationContext context;

    @Test
    void corsConfigBeanIsPresentUnderDevProfile() {
      assertThat(context.getBeanNamesForType(DevCorsConfig.class)).hasSize(1);
    }
  }
}
```

Before finalizing this test, check how other `@SpringBootTest` classes in this repo start a datasource without Testcontainers/Postgres (grep `TestPropertySource` and `spring.datasource.url` usage in `src/test/java/com/example/otpsentinel/api/*Test.java` and `src/test/java/com/example/otpsentinel/OtpSentinelApplicationSmokeTest.java`) — if this repo has no H2 dependency, adjust to extend `AbstractPostgresIntegrationTest` instead (it already gives a running Postgres via Testcontainers + `@DynamicPropertySource`) rather than introducing a new H2 dependency, e.g.:

```java
class DevCorsConfigTest {
  @ActiveProfiles("dev")
  static class DevProfile extends AbstractPostgresIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired ApplicationContext context;
    @Test
    void corsConfigBeanIsPresentUnderDevProfile() {
      assertThat(context.getBeanNamesForType(DevCorsConfig.class)).hasSize(1);
    }
  }
  static class DefaultProfile extends AbstractPostgresIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired ApplicationContext context;
    @Test
    void corsConfigBeanIsAbsentByDefault() {
      assertThat(context.getBeanNamesForType(DevCorsConfig.class)).isEmpty();
    }
  }
}
```
Use whichever matches the existing `@SpringBootTest` setup pattern in this codebase — do not add a new test dependency (no H2) if Postgres-via-Testcontainers is the established pattern.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B -Dtest=DevCorsConfigTest test`
Expected: compilation failure (`DevCorsConfig` doesn't exist) or the `DevProfile` case failing (bean absent).

- [ ] **Step 3: Implement `DevCorsConfig`**

Create `src/main/java/com/example/otpsentinel/config/DevCorsConfig.java`:

```java
package com.example.otpsentinel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS only for local frontend development against a separate-port Vite dev server (ADR-016). The
 * demo/production image serves the frontend from the same Spring Boot origin (M10), so this bean
 * is intentionally absent unless {@code SPRING_PROFILES_ACTIVE=dev} — never active in
 * default/demo/production.
 */
@Configuration
@Profile("dev")
public class DevCorsConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins("http://localhost:5173")
        .allowedMethods("GET", "POST")
        .allowedHeaders("*");
  }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -B -Dtest=DevCorsConfigTest test`
Expected: `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/otpsentinel/config/DevCorsConfig.java \
  src/test/java/com/example/otpsentinel/config/DevCorsConfigTest.java
git commit -m "feat(api): dev-profile-only CORS for future Vite frontend dev server"
```

---

### Task 3: README + `.env.example` live-demo documentation

**Files:**
- Modify: `.env.example`
- Modify: `README.md`

**Interfaces:** None — documentation only, no code interfaces.

- [ ] **Step 1: Verify `.env.example` completeness**

Open `.env.example`. Confirm every variable `AgentConfig` reads for live mode is present where required: `AI_MODE`, `NVIDIA_API_KEY` (must stay empty; live credential is injected at runtime), `NVIDIA_BASE_URL`, `NVIDIA_CHAT_MODEL`, `NVIDIA_EMBEDDING_MODEL`. If any non-secret variable is missing, add it with the same default as `application.yml`'s `${VAR:default}` fallback.

- [ ] **Step 2: Add a "Canlı demo nasıl çalıştırılır" section to `README.md`**

Insert a new section after "## API walkthrough" (before "## Bilinen sınırlamalar"):

```markdown
## Canlı demo nasıl çalıştırılır

Varsayılan mod (`AI_MODE=stub`) deterministik, sabit bir script kullanır — ağ
bağlantısı veya API key gerektirmez, CI'de ve `docker compose up`'ta hep
yeşildir. Canlı mod (`AI_MODE=live`) yerine gerçek NVIDIA NIM chat modeli
(`NVIDIA_CHAT_MODEL`) ile gerçek tool-calling yapar ve gerçek pgvector RAG'dan
(NVIDIA embedding modeliyle, `NVIDIA_EMBEDDING_MODEL`) sonuç döndürür — model
her seferinde biraz farklı ifade/sıra üretebilir (stub'un birebir sabit
script'inin aksine), ama aynı kanıt/hipotez/citation kurallarına uyar (bkz.
`docs/16-adr.md` ADR-015, `docs/07-agent-tool-spec.md`).

```bash
cp .env.example .env
# .env içinde:
#   AI_MODE=live
#   NVIDIA_API_KEY=<gerçek NVIDIA API key>
docker compose up --build
```

Uygulama açılışında (`AI_MODE=live` iken) `docs/15-demo-fixtures.md`'deki 4
knowledge fixture'ı (`INC-2026-041`, `RB-OTP-001`, `ERR-OTP-001`,
`POL-CHANGE-001`) otomatik olarak pgvector'a ingest edilir — elle bir adım
gerekmez. Bu ingest idempotenttir: konteyner her yeniden başladığında zaten var
olan belge/versiyon tekrar yazılmaz.

`NVIDIA_API_KEY` olmadan `AI_MODE=live` ile başlatmak, ilk NVIDIA çağrısında
hata verir. Ana test suite ve varsayılan demo akışı hep `AI_MODE=stub` ile
çalışır ve gerçek bir key gerektirmez.
```

- [ ] **Step 3: Note dev-only CORS in README's "Bilinen sınırlamalar" or a new one-liner**

In the "## Bilinen sınırlamalar" section, add one bullet:

```markdown
- CORS varsayılan olarak kapalıdır (frontend aynı origin'den servis edilir,
  bkz. `docs/16-adr.md` ADR-016). Yalnızca `SPRING_PROFILES_ACTIVE=dev` ile
  (M10 frontend geliştirme sırasında, ayrı port Vite dev server için) dar bir
  CORS izni açılır.
```

- [ ] **Step 4: Commit**

```bash
git add .env.example README.md
git commit -m "docs: live demo mode run instructions and dev-only CORS note"
```

---

### Task 4: Real end-to-end live investigation run (manual verification, not automated)

This task is **not** written as failing-test/implementation steps — it is a one-time, manual, `local-live`-style verification run (same category as M4/M5's `NvidiaNimEmbeddingServiceLiveTest`/`NvidiaNimChatServiceLiveTest`, but exercising the whole stack through the REST API instead of one isolated client call). Do this after Tasks 1-3 are committed.

**Files:** None created. Possible modify: `src/main/java/com/example/otpsentinel/agent/IncidentAnalysisAiService.java` (`@SystemMessage` text) only if step 3 below finds a real problem.

- [ ] **Step 1: Boot the stack in live mode**

```bash
set -a && source .env && set +a && AI_MODE=live docker compose up --build -d
```

Wait for `docker compose ps` to show `db` as `healthy` and the app as `Up`. Check `curl -s http://localhost:8080/actuator/health` returns `{"status":"UP"}`.

- [ ] **Step 2: Confirm knowledge auto-ingest actually ran**

```bash
docker compose exec -T db psql -U otpsentinel -d otpsentinel -c "SELECT document_id, version FROM knowledge_document ORDER BY document_id;"
```

Expected: 4 rows (`ERR-OTP-001`, `INC-2026-041`, `POL-CHANGE-001`, `RB-OTP-001`). Restart the app container (`docker compose restart app` or equivalent service name) and re-run the same query — row count must stay 4 (idempotency proof for the report).

- [ ] **Step 3: Run the real investigation and capture output**

```bash
curl -s -X POST http://localhost:8080/api/v1/investigations \
  -H "Content-Type: application/json" \
  -d "{\"question\": \"Son 15 dakikada OTP teslimat oranı neden düştü?\", \"timeWindow\": {\"startAt\": \"2026-07-30T11:15:00Z\", \"endAt\": \"2026-07-30T11:30:00Z\"}, \"locale\": \"tr-TR\"}" | tee /tmp/m9-live-investigation.json | jq .
```

Capture the full JSON response and the app container logs for this request (`docker compose logs app --since=2m`) — the logs must show real HTTP calls going out to `integrate.api.nvidia.com` (tool-calling round-trips), not stub script log lines.

- [ ] **Step 4: Evaluate against the prompt's item-3 criteria**

Check, from the captured response + logs:
1. Total tool calls ≤ 8 (`AI_MAX_TOOL_CALLS`).
2. A connection-pool/provider theme surfaces as a hypothesis (need not be word-for-word the stub's hypothesis text — real model, real phrasing).
3. All `evidence[].id` values match IDs the application's `EvidenceCollector` generates (i.e., present in the tool-call evidence, not invented strings) — cross-check against `docker compose logs app` tool-call entries.
4. If the question/response implies any forbidden action (auto rollback/restart), confirm `recommendedActions[].actionType` never claims auto-execution and `requiresApproval` is `true` for any risky action — this is enforced by `IncidentInvestigationService`'s validation pipeline (M6), so a failure here would be a real regression, not just an LLM quirk.

If any of these fail because the model didn't call tools correctly or returned unusable output: read `IncidentAnalysisAiService`'s `@SystemMessage` (`src/main/java/com/example/otpsentinel/agent/IncidentAnalysisAiService.java`), tighten the wording (e.g. be more explicit that tool calls are mandatory before answering, or clarify evidence-id sourcing), re-run Step 3, and note the diff + reasoning in the session report. Do not touch `docs/07-agent-tool-spec.md` tool contracts themselves — only the system prompt wording is in scope.

- [ ] **Step 5: Tear down**

```bash
docker compose down
```

- [ ] **Step 6: No commit for this task unless Step 4 required a system-prompt change**

If `IncidentAnalysisAiService` was edited, run the full offline suite again (`mvn -B spotless:apply && mvn -B verify -Dsurefire.excludedGroups=local-live` — this must still pass, since stub mode doesn't call the system prompt against a real model but any Java changes must still compile/pass existing stub-mode assertions), then:

```bash
git add src/main/java/com/example/otpsentinel/agent/IncidentAnalysisAiService.java
git commit -m "fix(agent): tighten live system prompt after M9 live e2e verification"
```

---

### Task 5: Final whole-branch verification

**Files:** None (verification only).

- [ ] **Step 1: Full offline build**

```bash
mvn -B spotless:apply && mvn -B verify -Dsurefire.excludedGroups=local-live
```

Expected: `BUILD SUCCESS`, all tests green, Spotless clean, no `NVIDIA_API_KEY` used.

- [ ] **Step 2: Whole-branch review**

Diff `milestone/M9-live-demo-mode` against `main` (`git diff main...HEAD`) and sanity-check: no `createIncidentDraft` added to the agent's tool set, no secrets committed (`git log -p main..HEAD | grep -i 'NVIDIA_API_KEY\s*=\s*[A-Za-z0-9]'` should be empty besides the blank `.env.example` line), CORS bean stays profile-gated, auto-ingest stays idempotent.

- [ ] **Step 3: Write the session report and log entry**

Per `prompts/08-session-report.md`, write `prompts/handoff/M9-report.md` (status **DONE**, not VERIFIED — an independent session verifies) with the real commands/output from Task 4's live run, and append the M9 line to `SESSION_LOG.md`.

## Self-Review Notes

- **Spec coverage:** Prompt item 1 → Task 1. Item 2/3 → Task 4. Item 4 → Task 3. Item 5 (CORS) → Task 2. "Bitti sayılması" checklist → Tasks 1 (idempotent + tested), 4 (live run + report), 5 (`mvn verify` green), 3 (README).
- **No placeholders:** all steps carry real code/commands; Task 4 is deliberately manual/non-TDD per the prompt's own instruction ("Bu otomatik test değil").
- **Type consistency:** `KnowledgeRepository.existsDocument(String, String)` used identically in `JdbcKnowledgeRepository`, `KnowledgeAutoIngestRunner`, and the test. `KnowledgeAutoIngestRunner` constructor signature `(KnowledgeIngestionService, KnowledgeRepository, boolean)` used identically in `AgentConfig` and the test.
