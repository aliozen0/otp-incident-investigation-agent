## Görev: M7 — REST/approval

Önce: `git checkout main && git pull && git checkout -b milestone/M7-rest-approval` (bkz. `docs/20-git-workflow.md`).

**Süreç:** M5/M6'daki gibi çalış — `superpowers:writing-plans` ile küçük sıralı task'lara böl, `superpowers:subagent-driven-development` ile her task'ı taze implementer + ayrı reviewer subagent'a yaptır. Context limiti dolarsa bir `M7-session-status.md` yazıp durabilirsin.

Kaynak: `docs/14-implementation-plan.md` → **M7 — REST/approval**.

İlgili spec dosyaları: `docs/06-api-contracts.md` (tam REST sözleşmesi, request/response örnekleri, error kodları), `docs/03-system-requirements.md` (NFR-006/007/008, FR-013/014/015/016), `docs/09-security-governance.md` (human-in-the-loop onay zinciri), `docs/12-atdd-gherkin.md` ("Human approval" ve "API validation" feature'ları).

### Önce oku — zaten var olanı tekrar yazma

- M1: `Investigation`/`IncidentDraft` aggregate'leri, repository portları.
- M3: `JdbcInvestigationRepository`/`JdbcIncidentDraftRepository`/`JdbcAuditEventRepository` implementasyonları — hazır, DI ile bağlanmayı bekliyor.
- M5: `IncidentInvestigationService.investigate(...)` — agent orchestration, `Investigation` üretiyor ama henüz repository'ye persist etmiyor/audit yazmıyor (M6 raporunda "gerçek DI ile bağlanma M7'nin işi" notu var).
- M6: `ClaimValidator`, `PiiScanner` — validation zaten `IncidentInvestigationService` içinde çağrılıyor.

### Kapsam

1. **Wiring:** `IncidentInvestigationService`'i gerçek `InvestigationRepository`/`AuditEventRepository` bean'lerine bağla (M3'ün Jdbc implementasyonları) — investigation tamamlanınca persist edilsin, her adımda (`request accepted, tool called/completed/failed, RAG completed, LLM completed, validation passed/failed`) audit event yazılsın (FR-017'nin tam listesi, M6'da yalnızca prompt-injection sinyali vardı).
2. **POST `/api/v1/investigations`** — `docs/06`'daki tam sözleşme: request validation (question 10-1000 karakter, timeWindow 1dk-24saat, gelecek zaman red, locale allowlist), `IncidentInvestigationService`'i çağırıp `IncidentAnalysisResult`'ı domain `Investigation`'a ve oradan API response DTO'suna map et.
3. **GET `/api/v1/investigations/{id}`** — persist edilmiş canonical snapshot'ı (M3 repository) döndür (AC-030).
4. **POST `/api/v1/investigations/{id}/incident-draft/preview`** — kalıcı kayıt oluşturmadan `IncidentDraft` payload'ı göster (AC-013, FR-013).
5. **POST `/api/v1/investigations/{id}/incident-draft/decisions`** — `Idempotency-Key` header zorunlu; `APPROVE`/`REJECT`. Approve: yetki+snapshot doğrula, `IncidentDraft.approve(...)` (M1 domain metodu) çağır, persist et, audit'e yaz. Aynı key tekrarında `200` + eski ID + `idempotentReplay=true` (AC-014). Reject: incident oluşturma, reddi audit'e yaz (AC-015).
6. **Error handling:** problem-details formatı (`docs/06` örneği), `400 INVALID_TIME_WINDOW`, `400 INVALID_REQUEST`, `422 QUESTION_NOT_ACTIONABLE`, `429 INVESTIGATION_RATE_LIMITED`, `502 MODEL_PROVIDER_ERROR`, `504 INVESTIGATION_TIMEOUT`.
7. `X-Correlation-Id` header desteği (varsa kullan, yoksa üret; audit/log'a yansıt).

Henüz **yazma**: gerçek authentication/authorization (docs/09 "Local profilde auth kapalı olabilir, açık DEMO MODE sinyali üretir" — bu MVP'de yeterli, tam auth production vizyonu M8/sonrası değil bu projenin kapsamı dışı), UI, Swagger'ın ötesinde bir şey.

Bu milestone'un "Kabul" kriteri: ana Gherkin akışları (docs/12 "Human approval" + "API validation" feature'ları) uçtan uca geçer (`docs/14` M7 kabul cümlesi).

### Sırayla

1. İlgili requirement/acceptance criterion'ı belirle (AC-013, AC-014, AC-015, AC-024, AC-027, AC-029, AC-030, FR-013/014/015/016/017, NFR-006/007/008).
2. Değişecek dosyaları listele (`api` paketi: controller'lar, DTO'lar, exception handler; `application` paketinde investigation/approval orchestration servisleri; `config`'te DI wiring).
3. Önce failing test yaz: docs/12 "Human approval" (4 senaryo: preview-no-incident, approve-creates-one, idempotent-replay, reject) + "API validation" (2 senaryo: future interval, >24h interval) — MockMvc/Testcontainers ile uçtan uca.
4. Minimum implementasyonla geçir.
5. Refactor.
6. `docs/17-traceability-risk-dod.md`'yi güncelle.

### Kısıtlar

- LLM'e hiçbir write yetkisi yok; `createIncidentDraft` hâlâ agent tool setinde değil (M5/M6'da doğrulandı, bozma).
- Approval ayrı, açık `APPROVE` kararı olmadan `IncidentDraft` kalıcı olamaz (M1 domain invariant'ı zaten bunu zorluyor — controller bunu bypass etmemeli).
- Idempotency DB seviyesinde garanti (M3) — controller'da ayrıca application-level kontrol ekleme, tek kaynak DB constraint kalsın.
- API DTO'ları domain tiplerini `api` paketine sızdırmadan map etsin (hexagonal sınır, `AGENTS.md`).
- Tüm `mvn`/`docker` komutları repository kökünden.
- Commit'ten önce `mvn spotless:apply` + tam `mvn verify` (tüm proje) yeşil olduğunu doğrula.

### Bitti sayılması için

- docs/12 "Human approval" (4 senaryo) ve "API validation" (2 senaryo) testli ve geçiyor.
- `mvn verify` (tüm proje) BUILD SUCCESS.
- `docs/17-traceability-risk-dod.md` DoD listesine uyuyor.

### Oturum sonu

`prompts/08-session-report.md` kuralına göre `prompts/handoff/M7-report.md` dosyasını yaz ve `SESSION_LOG.md`'ye satır ekle. Branch adı `milestone/M7-rest-approval`, commit convention `docs/20-git-workflow.md`. **Kendi işini kendin "VERIFIED" yazma** — DONE yaz, ayrı/bağımsız bir oturum doğrulayacak.
