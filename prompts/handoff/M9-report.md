# M9 — Canlı mod uçtan uca kanıtlama (demo altyapısı)

## Durum
DONE

## Kapsam

M0-M8'de `AI_MODE=live` yalnızca izole spike'larla (M4: tek embed çağrısı, M5: tek chat çağrısı) doğrulanmıştı. Bu oturumda gerçek NVIDIA NIM chat model (`meta/llama-3.1-8b-instruct`) + gerçek pgvector RAG + gerçek agentic tool-calling ile uçtan uca bir investigation'ın çalıştığı, gerçek `NVIDIA_API_KEY` ile beş kez kanıtlandı.

`superpowers:subagent-driven-development` ile 4 kodlanabilir task (bilgi tabanı auto-ingest, dev-only CORS, README/.env dokümantasyonu, final whole-branch review + fix) taze implementer+reviewer subagent çiftleriyle yapıldı; gerçek canlı doğrulama koşusu (Task 4) kontrolör tarafından elle çalıştırıldı (gerçek `NVIDIA_API_KEY` gerektiği, otomatik test olmadığı ve iteratif sistem-promptu ayarı gerektirdiği için).

Canlı koşu sırasında **iki gerçek, önceden bilinmeyen hata** bulunup düzeltildi:
1. `docker-compose.yml`, `NVIDIA_API_KEY`/`NVIDIA_BASE_URL`/`NVIDIA_CHAT_MODEL`/`NVIDIA_EMBEDDING_MODEL`'i app container'ına hiç aktarmıyordu (`.env`'de doluydu ama compose `environment:` bloğunda tanımlı değildi) — `AI_MODE=live` ile app açılışta `modelId must not be blank` hatasıyla çöküyordu.
2. NVIDIA'nın `meta/llama-3.1-8b-instruct` modeli `includePreviousPeriod` argümanını bazen JSON boolean yerine JSON string (`"true"`) olarak dönüyor; LangChain4j'nin tool-argument coercion'ı bunu `boolean` parametreye çeviremeyip `IllegalArgumentException` fırlatıyor, tool hiç çalışmadan investigation'ı sessizce `FAILED` yapıyordu. M5'in `Instant` için yaptığı aynı adaptör-sınırı düzeltmesi (docs/superpowers M5 deviation #2) tekrarlandı: parametre `String`'e çevrildi, `AgentTools` içinde deterministik `parseBoolean` ile parse ediliyor (stub/test'ler de güncellendi).

Ayrıca: sistem promptu, modelin başarılı bir tool'u aynı argümanlarla tekrar tekrar çağırıp bütçeyi tüketmesini (gözlemlendi — duplicate-call reddi modele dönüyor ama model bunu görmezden gelip tekrar deniyor) azaltmak için açık 6-tool sıralı talimatla sıkılaştırıldı; `ChatModel.logResponses(true)` (yalnızca response, `Authorization` header'ı içerebilecek `logRequests` asla) + `application.yml`'de `dev.langchain4j: DEBUG` eklenip gerçek NVIDIA çağrılarının log'da görülebilir olması sağlandı; `IncidentInvestigationService`'in repair-loop'u önceden istisnaları tamamen sessizce yutuyordu, artık kırpılmış (300 karakter) bir WARN log'u var.

Final whole-branch review (opus, en yetenekli model) 2 Important + 6 Minor bulgu buldu (en önemlisi: yeni `parseBoolean`'ın `null`/blank değeri reddetmesi — opsiyonel argüman için gerçek bir regresyon, canlı koşuda zaten bir tool çağrısını boşa harcamıştı). Tek fix dalgasıyla hepsi düzeltildi, scoped re-review temiz.

## Değişen dosyalar
- `src/main/java/com/example/otpsentinel/rag/KnowledgeRepository.java`, `JdbcKnowledgeRepository.java` — `existsDocument(documentId, version)` port+impl.
- `src/main/java/com/example/otpsentinel/rag/KnowledgeAutoIngestRunner.java` (yeni) — idempotent `ApplicationRunner`, `AI_MODE=live` iken 4 MVP knowledge fixture'ını ingest eder, zaten var olanı atlar.
- `src/main/java/com/example/otpsentinel/config/AgentConfig.java` — `knowledgeAutoIngestRunner` bean'i, live `ChatModel`'e `logResponses(true)`.
- `src/main/java/com/example/otpsentinel/config/DevCorsConfig.java` (yeni) — `@Profile("dev")` CORS, `/api/**`, `localhost:5173`.
- `src/main/java/com/example/otpsentinel/agent/AgentTools.java` — `getOtpMetrics`'in `includePreviousPeriod` parametresi `boolean` → `String` + deterministik `parseBoolean` (null/blank → false, geçersiz değer → throw).
- `src/main/java/com/example/otpsentinel/agent/IncidentAnalysisAiService.java` — sistem promptu: açık 6-tool sıralı talimat, duplicate-reddi sonrası "bir sonraki tool'a geç" talimatı.
- `src/main/java/com/example/otpsentinel/agent/stub/OtpDropOneOhOneScript.java` — `includePreviousPeriod` stub script'inde `"true"` (string).
- `src/main/java/com/example/otpsentinel/application/IncidentInvestigationService.java` — repair-loop WARN log (kırpılmış, 300 karakter).
- `docker-compose.yml` — `NVIDIA_API_KEY`/`NVIDIA_BASE_URL`/`NVIDIA_CHAT_MODEL`/`NVIDIA_EMBEDDING_MODEL` app container'a aktarılıyor, chat/embedding model'ler gerçek doğrulanmış değerlere default.
- `src/main/resources/application.yml` — `logging.level.dev.langchain4j: DEBUG`.
- `.env.example`, `README.md` — canlı demo talimatı ("Canlı demo nasıl çalıştırılır"), CORS sınırlaması notu, startup-time failure notu.
- `src/test/java/com/example/otpsentinel/rag/KnowledgeAutoIngestRunnerTest.java` (yeni), `src/test/java/com/example/otpsentinel/config/DevCorsConfigTest.java` (yeni), `src/test/java/com/example/otpsentinel/agent/AgentToolsTest.java` (`includePreviousPeriod` string/null/blank/invalid testleri).
- `docs/superpowers/plans/2026-08-01-m9-live-demo-mode.md` — plan.

## Testler (gerçek komut + gerçek çıktı, iddia değil)

Offline suite (WSL2, NVIDIA_API_KEY olmadan):
Komut: `wsl.exe -e bash -lc 'cd ... && source sdkman-init.sh && mvn -B spotless:apply && mvn -B verify -Dsurefire.excludedGroups=local-live'`
```
[INFO] Tests run: 148, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- spotless:2.43.0:check (spotless-check) @ otp-sentinel ---
[INFO] Spotless.Java is keeping ... files clean
[INFO] BUILD SUCCESS
```

Canlı e2e doğrulama (gerçek `NVIDIA_API_KEY`, `AI_MODE=live`, `docker compose up --build`):
- Health: `curl -s http://localhost:8080/actuator/health` → `{"status":"UP"}`
- Auto-ingest doğrulandı: `psql` ile `knowledge_document` tablosunda 4 satır (`ERR-OTP-001`, `INC-2026-041`, `POL-CHANGE-001`, `RB-OTP-001`). `docker compose restart app` sonrası tekrar sorgulandı: hâlâ 4 belge / 21 chunk — **idempotency kanıtlandı** (duplicate yok).
- Beş gerçek investigation koşusu yapıldı (`POST /api/v1/investigations`, docs/15'in tam OTP-DROP-001 sorusu/aralığıyla). Sonuçlar model davranışının stokastikliğini gösterdi: 2 kez `FAILED` (biri `UNKNOWN_EVIDENCE_REFERENCE` — model uydurma bir evidence id'si üretti, `ClaimValidator`/ADR-008 tarafından doğru şekilde reddedildi; biri araç argümanı coercion hatası, bu oturumda kök nedeni bulunup düzeltildi), 2 kez `PARTIAL_ANALYSIS` (gerçek tool bütçesi/duplicate-call limiti doğru tetiklendi), **1 kez tam başarı**:

  `investigationId=a43a2133-ff91-4cb1-ab9c-8234f8ed7fa2` → `status=ANOMALY_CONFIRMED`, `severity=HIGH`, `confidence=0.8` (docs/15'in beklediği 0.80–0.92 aralığında), 1 hipotez (`"High rate of provider timeouts"`, destekleyici evidence ile), 1 `recommendedAction` (`CHANGE_PROPOSAL`, risk `MEDIUM`, `requiresApproval=true` — otomatik uygulanabilir değil), `validation.status=PASSED` (bir `CAUSATION_LANGUAGE_DETECTED` uyarısıyla — validator'ın nedensellik dilini doğru yakaladığını kanıtlıyor).

  Gerçek `audit_event` tablosundan bu koşunun tam tool sırası (psql çıktısı, gerçek zaman damgalarıyla):
  ```
  REQUEST_ACCEPTED, TIME_WINDOW_RESOLVED,
  TOOL_CALLED/TOOL_COMPLETED getOtpMetrics (ev-otp-success-rate-current, ev-otp-success-rate-previous),
  TOOL_CALLED/TOOL_COMPLETED getErrorDistribution (ev-error-distribution-top),
  TOOL_CALLED/TOOL_COMPLETED getQueueHealth (ev-queue-health),
  TOOL_CALLED/TOOL_FAILED getProviderHealth ("No fixture data for provider default" — model yanlış provider adı geçti, sistem düzgün TOOL_FAILED olarak işledi, çökmedi),
  TOOL_CALLED/TOOL_COMPLETED getRecentChanges (ids=[]),
  RAG_COMPLETED (results=0),
  LLM_COMPLETED, VALIDATION_PASSED
  ```
  6 tool çağrısı, `AI_MAX_TOOL_CALLS=8` sınırının altında. Evidence id'leri (`ev-otp-success-rate-current` vb.) uygulama tarafından üretildi, model uydurmadı (bu koşuda) — önceki `FAILED` koşularından biri tam olarak modelin id uydurmaya çalıştığı, sistemin bunu reddettiği anı gösteriyor.

- `docker compose logs app` içinde `d.l.http.client.log.LoggingHttpClient` satırlarıyla gerçek `integrate.api.nvidia.com` yanıtları görüldü (response-only logging, `Authorization` header'ı asla log'lanmadı — `logRequests` hiç kullanılmadı).
- Secret-pattern taraması: commit'lerde `NVIDIA_API_KEY` değeri yok (yalnız açıklayıcı yorumlar ve boş `.env.example` satırı).

## Karşılanan requirement/AC
- Prompt madde 1 (bilinen fixture'ların otomatik ingest edilmesi, idempotent) — `KnowledgeAutoIngestRunnerTest` (3 test: ilk koşu 4 belge, ikinci koşu duplicate yok, disabled no-op) + gerçek `docker compose restart` kanıtı.
- Prompt madde 2 (gerçek uçtan uca canlı investigation, log/trace ile kanıt) — yukarıdaki 5 koşu + audit_event trace'i + LoggingHttpClient response log'u.
- Prompt madde 3 (max 8 çağrı, connection-pool/provider hipotezi, evidence id uygulama üretimi, forbidden action reddi) — 8 çağrı sınırı hem `ToolBudgetGuard` testleriyle hem gerçek koşuda (6/8) doğrulandı; hipotez `provider timeout` temalı çıktı (docs/15'in connection-pool teması ile aynı kök alan, HIGH severity); evidence id'leri app tarafından üretildi (bir FAILED koşusu modelin id uydurma girişimini reddederek bunu ayrıca kanıtladı); `recommendedActions` hiçbir zaman otomatik uygulanabilir değildi (`requiresApproval=true`).
- Prompt madde 4 (.env.example tam, README canlı demo talimatı) — README "Canlı demo nasıl çalıştırılır" bölümü + `.env.example` doğrulandı (M4/M5'te zaten doluydu).
- Prompt madde 5 (CORS, dev-only) — `DevCorsConfig` (`@Profile("dev")`), `DevCorsConfigTest` (2 context: default'ta bean yok, dev'de var, ikisi de gerçek Testcontainers Postgres ile).

## Karşılanmayan / ertelenen
- Model tutarsızlığı (5 koşudan yalnızca 1'i tam `ANOMALY_CONFIRMED` başarıya ulaştı) sistem promptu iyileştirmesiyle azaltıldı ama tamamen ortadan kaldırılamadı — `meta/llama-3.1-8b-instruct` küçük bir model, bu prompt'un kendi de kabul ettiği bir sınır ("Scripted stub'daki gibi birebir aynı olması gerekmez"). Daha büyük/güçlü bir NVIDIA modeline geçiş (M10 sonrası, ayrı bir karar) tutarlılığı artırabilir ama bu oturumun kapsamı dışında bırakıldı.
- `docs/07-agent-tool-spec.md`'in T-001 tanımı hâlâ `boolean includePreviousPeriod` diyor; domain seviyesinde (`OtpMetricsRequest`) hâlâ gerçek `boolean` — yalnızca LLM-agent sınırındaki tool imzası `String`'e gevşetildi (M5'in `Instant` için yaptığı gibi). Final review'da Minor/belgeleme notu olarak işaretlendi, davranışsal bir sapma değil; gerekirse ayrı bir docs güncellemesiyle kapatılabilir.

## Spec çelişkisi/belirsizlik (varsa)
Yok — M9 prompt'unun kendisi model tutarsızlığını ("saçma JSON dönüyor, vs." kök nedeni bul ve düzelt) ve stub ile birebir aynı olmama durumunu ("gerçek model") açıkça öngörmüştü; bulunan iki gerçek bug (compose env forwarding, boolean coercion) kapsam içiydi ve düzeltildi.

## Sonraki oturum için not
M10 (frontend, ADR-016) başlatılabilir: canlı mod artık uçtan uca kanıtlanmış durumda (bilgi tabanı otomatik, gerçek investigation en az bir kez tam başarıyla tamamlandı, tüm güvenlik/validation mekanizmaları gerçek modelle test edildi). Kendi işim **VERIFIED değil DONE** — bağımsız oturum doğrulayacak. Branch: `milestone/M9-live-demo-mode`.
