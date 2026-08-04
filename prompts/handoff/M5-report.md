# M5 — Agent orchestration

## Durum
DONE

## Kapsam
LangChain4j tabanlı OTP investigation agent'ı tamamlandı: NVIDIA NIM chat compatibility spike ile
`meta/llama-3.1-8b-instruct` pinlendi; çevrimdışı/CI için deterministik scripted `StubChatModel`,
uygulama üretimli evidence/citation mapping, altı read-only investigation tool'u, düz Java
budget/dedup/timeout/retry politikası, structured `AiService`, stub/live `ChatModel` seçimi ve
`IncidentInvestigationService` lifecycle orchestration eklendi. Invalid structured output bir kez
onarılıyor, ikinci hata `FAILED`; bilinmeyen evidence reddediliyor, dönmeyen knowledge referansı
filtreleniyor. OTP-DROP-001 uçtan uca testi altı tool'un sırasını ve sekiz çağrı sınırını,
`ANOMALY_CONFIRMED/HIGH`, 72.1/98.1 metriklerini, OPERATOR_B connection-pool birinci hipotezini,
sağlıklı queue'nun ilk neden olmamasını, `INC-2026-041` citation'ını ve v2.4 deploy'un yalnız
korelasyon olarak ifade edilmesini doğruluyor. `createIncidentDraft` agent tool setine eklenmedi.

## Değişen dosyalar
- `.env.example`, `docs/19-technology-baseline.md` — NVIDIA chat modeli
  `meta/llama-3.1-8b-instruct` olarak pinlendi.
- `pom.xml` — LangChain4j `AiServices` bağımlılığı ve `local-live` Surefire ayrımı.
- `src/main/java/com/example/otpsentinel/agent/` — structured DTO'lar, `ToolBudgetGuard`,
  `EvidenceCollector`, altı `@Tool`, `IncidentAnalysisAiService` ve deterministic stub model.
- `src/main/java/com/example/otpsentinel/application/IncidentInvestigationService.java`,
  `InvestigationRequest.java` — lifecycle, repair-once, policy-limit ve citation doğrulama.
- `src/main/java/com/example/otpsentinel/config/AgentConfig.java` — `AI_MODE=stub|live` model seçimi.
- `src/test/java/com/example/otpsentinel/agent/` — DTO/evidence/budget/tool/stub/AiService unit ve
  NVIDIA live compatibility testleri.
- `src/test/java/com/example/otpsentinel/application/` — orchestration failure-path testleri ve
  OTP-DROP-001 uçtan uca ATDD testi.
- `src/test/java/com/example/otpsentinel/config/AgentConfigTest.java` — stub/live bean seçimi.
- `docs/superpowers/plans/2026-07-30-m5-agent-orchestration.md`,
  `prompts/handoff/M5-prompt.md`, `prompts/handoff/M5-session-status.md` — plan ve ara handoff.
- `docs/17-traceability-risk-dod.md` — gerçekten doğrulanan M5 release-checklist maddeleri.

## Testler (gerçek komut + gerçek çıktı, iddia değil)
Komut:
`mvn -B spotless:apply && mvn -B verify -Dsurefire.excludedGroups=local-live`

Çıktı özeti:
```text
[INFO] Tests run: 101, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- spotless:2.43.0:check (spotless-check) @ otp-sentinel ---
[INFO] Spotless.Java is keeping 135 files clean - 0 needs changes to be clean
[INFO] BUILD SUCCESS
[INFO] Total time:  01:13 min
```

Ana kabul testi, ayrıca izole:
`mvn -B test -Dtest=OtpDropOneOhOneEndToEndTest`

```text
[INFO] Running com.example.otpsentinel.application.OtpDropOneOhOneEndToEndTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

NVIDIA NIM live compatibility spike (credential environment üzerinden sağlandı; secret loglanmadı):
`mvn -B test -Dsurefire.excludedGroups= -Dtest=NvidiaNimChatServiceLiveTest`

```text
[INFO] Running com.example.otpsentinel.agent.NvidiaNimChatServiceLiveTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.486 s
[INFO] BUILD SUCCESS
```

Live model: `meta/llama-3.1-8b-instruct`.

Secret-pattern taraması:
```text
SECRET_FILE_MATCHES=0
```

## Karşılanan requirement/AC
- FR-005/FR-007, AC-012/AC-013 — yalnız altı onaylı read-only tool bağlı;
  `createIncidentDraft` agent'a verilmedi ve otomatik remediation yok.
- FR-006, AC-016/AC-017 — en fazla sekiz çağrı, başarılı duplicate reddi, timeout ve tek retry
  `ToolBudgetGuard` testleriyle; framework tool exception'ını yutsa bile policy-limit state'iyle
  `PARTIAL_ANALYSIS` garanti edildi.
- FR-009/AI-007, AC-022/AC-029 — typed structured output; ilk invalid JSON sonrası tek repair,
  ikinci invalid output sonrası `FAILED`.
- AI-001, AC-009 — evidence ID'leri uygulama üretir; modelin bilinmeyen evidence ID'si hard failure,
  dönmeyen knowledge citation'ı filtrelenir.
- FR-011/FR-012, AC-010/AC-011 — en fazla üç hipotez, supporting evidence ve 0–1 confidence;
  OTP-DROP-001 confidence `0.85` ile belgelenen `0.80–0.92` aralığında.
- AI-002/AI-006, AC-001/AC-002/AC-003/AC-004/AC-005/AC-007/AC-008/AC-026 — deterministic stub
  üzerinden ana fixture sonucu, dönem karşılaştırması, OPERATOR_B yoğunlaşması, healthy queue,
  connection-pool sıralaması, deploy korelasyonu ve `INC-2026-041` citation doğrulandı.
- NFR-004 — 101 testlik ana suite `NVIDIA_API_KEY` olmadan geçti; live test ayrı
  `local-live` grubunda.

## Karşılanmayan / ertelenen
- M6: numeric-claim source validator (AC-023), forbidden automatic-action validator,
  correlation-wording validator ve tam validation-report pipeline.
- M7: REST endpoint'leri, persistence/audit orchestration, canonical GET ve onaylı
  `createIncidentDraft` akışı.
- Task 10'da `docker compose up --build`/health tekrar çalıştırılmadı; MVP checklist'te bu maddeler
  işaretlenmedi. Full verify içindeki Spring Boot smoke ve Testcontainers pgvector testleri geçti.
- Provider-timeout, initial-metrics-failure ve RAG-none senaryolarının component davranışları önceki
  milestone'larda mevcut; bu M5 planında yeni uçtan uca ATDD kapsamı yalnız OTP-DROP-001 ve
  repair/budget/citation failure yollarıydı.

## Spec çelişkisi/belirsizlik (varsa)
1. Task 9 plan örneği confidence için `0.75` kullanıyordu; source-of-truth AC-011 ana fixture için
   `0.80–0.92` istiyor. Test `0.85` ve açık aralık assertion'ı ile düzeltildi.
2. LangChain4j 1.18.1 `DefaultToolExecutor`, model JSON stringini doğrudan `Instant` parametresine
   güvenilir biçimde çeviremiyor. Agent adapter sınırı ISO-8601 string kabul edip deterministik
   `Instant.parse` ile tool request'e dönüştürüyor; domain/tool portları `Instant` kalıyor.
3. `IncidentAnalysisResult`, planın kilitli tasarım kararı uyarınca docs/07 literal şemasındaki
   türetilebilir `timeWindow` ve `approvalRequired` alanlarını modelden istemiyor; bunlar application
   katmanının sorumluluğunda.
4. Bilinmeyen knowledge referansını plan kararına uygun filtrelemek için service çağrısı
   investigation-scoped `EvidenceCollector` alıyor; plan kod örneğindeki guard-only imza known
   knowledge allowlist'ine erişemiyordu.

## Sonraki oturum için not
M5 branch'i bağımsız verify'a ve ardından M6 validation pipeline çalışmasına hazırdır.
