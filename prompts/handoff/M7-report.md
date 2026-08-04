# M7 — REST/approval

## Durum
DONE

## Kapsam
`docs/06-api-contracts.md`'deki tam REST sözleşmesini ve `docs/09` human-in-the-loop onay
zincirini uçtan uca uyguladım. `superpowers:writing-plans` ile 10 task'lık plana böldüm
(`docs/superpowers/plans/2026-07-31-m7-rest-approval.md`), `superpowers:subagent-driven-development`
ile her task'ı taze implementer + ayrı reviewer subagent'a yaptırdım (2 task'ta fix-round gerekti),
sonunda tüm branch'e bağımsız bir final review yaptırdım (1 Critical + 4 Important bulgu, hepsi
düzeltildi ve ayrı bir re-review'la doğrulandı).

**Audit (FR-017):** `EvidenceCollector`'a `TOOL_CALLED/COMPLETED/FAILED` ve `RAG_COMPLETED` audit
emisyonu, `IncidentInvestigationService`'e `LLM_COMPLETED`/`VALIDATION_PASSED/FAILED` audit'i eklendi
(hepsi additive overload — eski 5-arg/2-arg/1-arg constructor'lar M5/M6 testlerini bozmadan aynen
duruyor). `InvestigationOrchestrator` (yeni, `config` paketi) `REQUEST_ACCEPTED`/`TIME_WINDOW_RESOLVED`/
`PREVIEW_GENERATED`/`APPROVAL_DECIDED`/`INCIDENT_CREATED`'i kendisi audit'liyor. FR-017'nin tam listesi
artık gerçek `audit_event` tablosuna karşı test ediliyor (önceden yalnızca in-memory fake'lerle
kanıtlanıyordu — final review'da bulunan bir eksiklikti, düzeltildi).

**Wiring:** M3'ün üç JDBC repository'si Spring bean'i oldu (`PersistenceConfig`, yeni). `AgentConfig`
artık gerçek OTP-DROP-001 stub script'ini (M5 testinden çıkarılıp paylaşılan
`agent/stub/OtpDropOneOhOneScript`) ve stub-mode'da deterministic `FixtureKnowledgeSearchPort`'u
(yeni, `rag/fixtures`) kullanıyor; `AI_MODE=live` yolu değişmedi (`JdbcKnowledgeSearchAdapter` +
`NvidiaNimEmbeddingService`). `InvestigationOrchestrator` her istek için guard/collector/tools/aiService
kurup `IncidentInvestigationService`'i çağırıyor, sonucu persist ediyor.

**REST:** `POST /api/v1/investigations` (validation: soru 10-1000 karakter, pencere 1dk-24sa, gelecek
zaman red, locale allowlist — `TimeWindow`'un kendi constructor'ına delege, tekrar yazılmadı),
`GET /api/v1/investigations/{id}`, `POST .../incident-draft/preview` (kalıcı kayıt yok — hem yapısal
hem DB sorgusuyla kanıtlı), `POST .../incident-draft/decisions` (`Idempotency-Key` zorunlu; approve
`IncidentDraft.approve()+create()`'i çağırıp persist ediyor, reject `reject()`'i çağırıyor).
**Idempotency tek kaynaktan:** controller/orchestrator hiçbir ön-kontrol yapmıyor; her çağrı yeni bir
`IncidentDraft` inşa edip `save()` deniyor, aynı `idempotencyKey` tekrarında DB'nin
`uq_incident_draft_idempotency_key` unique constraint'i `DataIntegrityViolationException` fırlatıyor,
bu yakalanıp `findByIdempotencyKey` ile orijinal sonuç `idempotentReplay=true` ile dönüyor — M7
prompt'unun "tek kaynak DB constraint kalsın" kısıtına birebir uyuyor, ayrıca bir replay testiyle
(aynı key ikinci kez, `incident_draft` satır sayısı hâlâ 1) kanıtlandı.

**Final review'da bulunup düzeltilen gerçek buglar (merge öncesi):**
- **Critical:** `decide(...)`'daki `decision` alanı doğrulanmıyordu — geçersiz/null değer sessizce
  REJECT'e düşüp idempotency key'i harcıyor, sonra `AuditEvent`'in blank-result reddiyle 500'e
  düşüyordu. Artık `decision` tam olarak `APPROVE`/`REJECT` değilse 400 dönüyor (state değişmeden önce).
- **Important:** preview endpoint'i `COMPLETED` olmayan (örn. `FAILED`) investigation'da `severity`
  null olduğu için NPE/500 veriyordu — `decide`'daki hazırlık kontrolü preview'a da taşındı, artık 409.
- **Important:** hatalı path UUID'i (`GET .../not-a-uuid`) 500 veriyordu — `IllegalArgumentException`
  handler'ı eklendi (400).
- **Important:** blanket `NoSuchElementException`/`IllegalStateException` handler'ları gerçek internal
  bug'ları da 404/409 gibi gösterip yanlış etiketleyebilirdi — dar, orchestrator'ın kasıtlı fırlattığı
  `InvestigationNotFoundException`/`InvestigationNotActionableException` tipleriyle değiştirildi.
- **Minor (parked, merge engellemedi):** yukarıdaki #3'ün fix'i olarak eklenen geniş
  `IllegalArgumentException→400` handler'ı, `IncidentInvestigationService.requireMatchingRequest`
  (satır 204) ve `ToolBudgetGuard`'ın config-guard'ları gibi iki internal-only path'i de teorik olarak
  400'e düşürüyor — client'tan tetiklenemez (yalnızca wiring bug/bozuk config ile), M8'e not düşüldü.

## Değişen dosyalar
- `src/main/java/com/example/otpsentinel/agent/EvidenceCollector.java` — 3-arg constructor,
  TOOL_CALLED/COMPLETED/FAILED + RAG_COMPLETED audit.
- `src/main/java/com/example/otpsentinel/application/IncidentInvestigationService.java` — 7-arg
  `investigate(...)` overload, LLM_COMPLETED/VALIDATION_PASSED/FAILED audit.
- `src/main/java/com/example/otpsentinel/rag/fixtures/FixtureKnowledgeSearchPort.java` — yeni.
- `src/main/java/com/example/otpsentinel/agent/stub/OtpDropOneOhOneScript.java` — yeni (M5 testinden
  çıkarıldı, paylaşılan).
- `src/main/java/com/example/otpsentinel/config/PersistenceConfig.java` — yeni.
- `src/main/java/com/example/otpsentinel/config/AgentConfig.java` — chatModel/knowledgeSearchPort/fixture
  tool bean'leri.
- `src/main/java/com/example/otpsentinel/config/InvestigationOrchestrator.java` — yeni, composition root.
- `src/main/java/com/example/otpsentinel/api/dto/*.java` — yeni, DTO'lar (domain type sızdırmıyor).
- `src/main/java/com/example/otpsentinel/api/InvestigationRequestValidator.java`,
  `ApiException.java`, `GlobalExceptionHandler.java`,
  `InvestigationNotFoundException.java`, `InvestigationNotActionableException.java` — yeni.
- `src/main/java/com/example/otpsentinel/config/CorrelationIdFilter.java` — yeni.
- `src/main/java/com/example/otpsentinel/api/InvestigationController.java`,
  `IncidentDraftController.java` — yeni.
- `docs/17-traceability-risk-dod.md` — Idempotency pass işaretlendi, M7 durum notu eklendi.
- Test dosyaları: `EvidenceCollectorTest`, `IncidentInvestigationServiceTest`,
  `FixtureKnowledgeSearchPortTest`, `OtpDropOneOhOneScriptTest`, `InvestigationOrchestratorTest`,
  `InvestigationRequestValidatorTest`, `InvestigationControllerTest`,
  `IncidentDraftPreviewControllerTest`, `IncidentDraftDecisionControllerTest` — yeni.
- `docs/superpowers/plans/2026-07-31-m7-rest-approval.md` — yeni, uygulama planı.

## Testler (gerçek komut + gerçek çıktı, iddia değil)
Komut:
`mvn -B spotless:apply && mvn -B verify -Dsurefire.excludedGroups=local-live`

Çıktı özeti:
```text
[INFO] Tests run: 137, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- spotless:2.43.0:check (spotless-check) @ otp-sentinel ---
[INFO] Spotless.Java is keeping 166 files clean - 0 needs changes to be clean
[INFO] BUILD SUCCESS
[INFO] Total time:  38.621 s
```

Test loglarında secret/OTP/telefon taraması (AC-028):
```text
grep -ciE "sk-abcdef|482913|555-123-4567" verify-log => 0
```

`docker compose up --build`: bu ortamda host'ta 5432 portu ilgisiz başka bir projenin container'ı
(`group-1-postgres-1`) tarafından tutulduğu için full-stack başlatılamadı — app image'ı container
içinde `mvn` ile başarıyla build oldu (`BUILD SUCCESS`), ancak compose stack'i port çakışmasından
çalışmadı. Kod defekti değil, ortam kısıtı; `docker compose down` ile temiz kapatıldı, M8'de/port
boşken tekrar denenmeli.

## Karşılanan requirement/AC
- FR-013 (Incident preview) — `IncidentDraftPreviewControllerTest`: preview sonrası `incident_draft`
  tablosunda 0 satır (DB sorgusuyla).
- FR-014 (Onay), SEC-002 — `IncidentDraftDecisionControllerTest.approvalCreatesExactlyOneIncident`:
  yalnızca `IncidentDraft.approve()`+`create()` üzerinden, controller'da bypass yok.
- FR-015 (Idempotency) — `IncidentDraftDecisionControllerTest.replayedApprovalReturnsOriginalIdAndNoSecondIncident`:
  gerçek DB constraint violation'ı üzerinden, aynı key ikinci kez → `idempotentReplay=true`, satır
  sayısı hâlâ 1.
- FR-016 (Re-fetch) — `InvestigationControllerTest.investigatesTheOtpDropOneOhOneFixtureAndAllowsRefetch`.
- FR-017 (Audit) — tüm event tipleri artık gerçek `audit_event` tablosuna karşı assert ediliyor
  (`InvestigationOrchestratorTest`, `IncidentDraftDecisionControllerTest`).
- NFR-006/007/008 — `/api/v1` base path, problem-details format, tool timeout/retry/budget (M5'ten
  değişmeden, sadece config'e bağlandı).
- docs/12 "Human approval" (4 senaryo: preview-no-incident, approve-creates-one, idempotent-replay,
  reject) ve "API validation" (2 senaryo: future interval, >24h interval) — tamamı testli ve geçiyor.

## Karşılanmayan / ertelenen
- Gerçek authentication/authorization — M7 prompt'unda kapsam dışı bırakılmıştı (DEMO MODE sinyali:
  `X-Demo-Mode: true` header, `CorrelationIdFilter` tarafından set ediliyor).
- `422 QUESTION_NOT_ACTIONABLE`/`429 INVESTIGATION_RATE_LIMITED` — stub-only MVP path'te gerçekçi bir
  tetikleyicisi yok (`422` için ikinci bir scripted senaryo icat etmek YAGNI ihlali olurdu); `502`/`504`
  handler'ları yazıldı ama yalnızca canlı model transport hatalarında tetikleniyor, offline test suite'te
  gerçekçi bir test yok — bilinçli, raporlanmış boşluk.
- `docker compose up --build` — yukarıda açıklanan ortam kısıtı, kod değil.
- Final review'ın parked minor bulgusu (geniş `IllegalArgumentException→400` handler'ının iki
  internal-only path'i de kapsaması) — client'tan tetiklenemediği için merge engellemedi, M8'e not.

## Spec çelişkisi/belirsizlik (varsa)
1. `docs/06`'nın decision endpoint'i request body'sinde draft-id taşımıyor — bu yüzden her çağrı
   deterministik payload'ı yeniden inşa edip yeni bir `IncidentDraft` deniyor; idempotency tamamen
   DB unique constraint'ine dayanıyor (M7-prompt'un kısıtına birebir uyuyor, ama literal API
   sözleşmesinde açıkça yazmıyordu — tasarım kararı olarak plan dosyasında gerekçelendirildi).
2. `docs/06`'nın "locale allowlist" ifadesi liste vermiyor; `tr-TR`/`en-US` varsayıldı (dokümante
   edilmiş varsayım).
3. Preview/decisions endpoint'lerinin `investigation not ready` durumu için docs/06'da açık bir error
   code yoktu; `409 INVESTIGATION_NOT_ACTIONABLE` eklendi (literal spec'in eksik bıraktığı bir alan,
   icat değil — davranış gerekliydi).

## Sonraki oturum için not
M7 branch'i bağımsız verify'a hazır (PR #1).
M8'de: Quickstart/Swagger örnekleri/seed/curl örnekleri/mimari diyagram/demo script/temiz log/failure
demo/opsiyonel minimal UI; ayrıca `docker compose up --build`'ı port çakışması olmayan bir ortamda
tekrar doğrula, ve final review'ın parked minor'ını (dar exception tipi ile IllegalArgumentException
handler'ını böl) ele al.
