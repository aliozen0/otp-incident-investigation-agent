## Görev: M1 — Domain foundation

Önce: `git checkout main && git pull && git checkout -b milestone/M1-domain-foundation` (bkz. `docs/20-git-workflow.md`).

Kaynak: `docs/14-implementation-plan.md` → **M1 — Domain foundation**.

İlgili spec dosyaları: `docs/05-domain-and-architecture.md` (aggregate/value object/invariant tanımları), `docs/03-system-requirements.md` (FR-004, FR-011, FR-012, FR-015, DATA-001), `docs/11-acceptance-criteria.md` (AC-009, AC-010, AC-011, AC-014).

### Kapsam

Yalnızca şunu yap: `domain` paketinde Spring/LangChain4j'den tamamen bağımsız, saf Java ile:

- `Investigation` aggregate (alanlar: InvestigationId, Question, ResolvedTimeWindow, Status, Severity, Evidence, Hypotheses, RecommendedActions, KnowledgeReferences, Confidence, ValidationReport, ToolExecutions, PromptVersion/SchemaVersion) — yaşam döngüsü `RECEIVED -> COLLECTING_EVIDENCE -> GENERATING_ANALYSIS -> VALIDATING -> COMPLETED` (veya `PARTIAL`/`FAILED`).
- `IncidentDraft` aggregate (`PREVIEWED -> APPROVED -> CREATED` / `-> REJECTED`; alanlar: investigationId, payload, approval, idempotencyKey, externalIncidentId).
- Value object'ler: `TimeWindow` (startAt<endAt, min 1dk, max 24saat), `Evidence`, `Hypothesis`, `RecommendedAction`.
- Enum'lar: `InvestigationStatus` (`NO_ANOMALY, ANOMALY_CONFIRMED, INSUFFICIENT_DATA, PARTIAL_ANALYSIS, FAILED`), `Severity`, `ActionType`, `ExecutionMode` (`MANUAL_CHECK, DRAFT_ONLY`).
- Repository portları (interface, implementasyon yok): `InvestigationRepository`, `IncidentDraftRepository`.
- Domain invariant'ları (`docs/05-domain-and-architecture.md`'deki 9 madde) constructor/factory method içinde zorlanmalı, dışarıdan bozulamaz olmalı.

Henüz **yazma**: Spring bean/JPA, LangChain4j, REST, gerçek tool'lar, RAG, persistence implementasyonu. Bunlar M2+.

Bu milestone'un "Kabul" kriteri: domain katmanı Spring/LangChain4j import'u olmadan, sade JUnit 5 ile test edilebilir (`docs/14-implementation-plan.md` M1 kabul cümlesi).

### Sırayla

1. İlgili requirement/acceptance criterion'ı belirle (AC-009 hypothesis→evidence id zorunlu, AC-010 max 3 hypothesis, AC-011 confidence 0-1, AC-014 idempotency key→tek incident, FR-004 status enum, FR-015 idempotency, DATA-001 UTC/ISO-8601).
2. Değişecek dosyaları listele (`domain` paketi altında aggregate/value object/enum/port sınıfları + `src/test/.../domain/**`).
3. Önce failing test yaz: her invariant için ayrı unit test (ör. "4. hypothesis eklenemez", "confidence 1.5 reddedilir", "aynı idempotency key ikinci CREATED üretmez", "TimeWindow 25 saat reddedilir").
4. Minimum implementasyonla geçir.
5. Refactor.
6. Gerekirse `docs/05-domain-and-architecture.md` ile kod arasında sapma varsa raporla (kod yazma önce spec'i sessizce değiştirme).

### Kısıtlar

- Domain paketine `org.springframework.*`, `dev.langchain4j.*`, JPA annotation'ı import etme — bu paket framework'ten tamamen izole olmalı (NFR-002).
- Bu milestone dışına taşma: repository implementasyonu, REST, tool, RAG, agent kodu yazma.
- 9 invariant'ın hepsi en az bir testle kanıtlanmalı; eksik bırakma.
- Onaylanmamış yeni bağımlılık ekleme (saf Java + JUnit 5 yeter).

### Bitti sayılması için

- Tüm domain unit testleri geçiyor (çalıştırıldı, gerçek çıktı raporlanacak).
- 9 invariant'ın her biri en az bir pozitif + bir negatif test ile kapsanmış.
- `domain` paketinde Spring/LangChain4j import'u yok (`grep` ile doğrula, rapora ekle).
- `docs/17-traceability-risk-dod.md` DoD listesine uyuyor.

### Oturum sonu

`prompts/08-session-report.md` kuralına göre `prompts/handoff/M1-report.md` dosyasını yaz ve `SESSION_LOG.md`'ye satır ekle. Branch adı `milestone/M1-domain-foundation`, commit convention `docs/20-git-workflow.md`.
