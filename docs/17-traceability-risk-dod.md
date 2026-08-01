# 17 — Traceability, Risk Register and Definition of Done

## Traceability matrix

| Story | Requirements | Acceptance |
|---|---|---|
| US-001 Natural query | FR-001, FR-002 | AC-024, AC-029 |
| US-002 Compare period | FR-003 | AC-001, AC-002 |
| US-003 Provider | FR-005 | AC-003 |
| US-004 Queue | FR-005 | AC-004 |
| US-005 Change correlation | AI-002 | AC-007 |
| US-006 RAG | FR-008, DATA-003 | AC-008, AC-020 |
| US-007 Sources | FR-010, NFR-009 | AC-009, AC-023 |
| US-009 Missing data | AI-007 | AC-018–020 |
| US-010 Structured | FR-009 | AC-022, AC-029 |
| US-011 Preview | FR-013 | AC-013 |
| US-012 Approval | FR-014, SEC-002 | AC-013, AC-015 |
| US-013 Idempotency | FR-015 | AC-014 |

## Tool traceability

| Tool | Ana acceptance |
|---|---|
| getOtpMetrics | AC-001, AC-002 |
| getErrorDistribution | AC-003 |
| getQueueHealth | AC-004 |
| getProviderHealth | AC-003, AC-005 |
| getRecentChanges | AC-007 |
| searchIncidentKnowledge | AC-008 |
| createIncidentDraft | AC-013, AC-014 |

## Risk register

| ID | Risk | Olasılık | Etki | Önlem |
|---|---|---|---|---|
| R-01 | Scope büyümesi | Yüksek | Yüksek | P0 bitmeden teknoloji yok |
| R-02 | LLM flaky | Yüksek | Orta | Stub + semantic assertions |
| R-03 | Kanıtsız sayı | Orta | Yüksek | Numeric claim validator |
| R-04 | Sonsuz tool loop | Düşük | Yüksek | Max 8 + duplicate block |
| R-05 | İlgisiz RAG | Orta | Orta | Filter/threshold/eval |
| R-06 | Prompt injection | Orta | Yüksek | Untrusted context + policy |
| R-07 | Duplicate incident | Orta | Yüksek | DB unique/idempotency |
| R-08 | Onaysız write | Düşük | Kritik | Ayrı endpoint/scope |
| R-09 | Secret loglama | Orta | Yüksek | Redaction/secret scan |
| R-10 | Dependency uyumu | Orta | Orta | BOM/pin/smoke spike |
| R-11 | Demo internet sorunu | Yüksek | Yüksek | Stub fallback |
| R-12 | Fixture tutarsızlığı | Orta | Yüksek | Arithmetic tests |
| R-13 | İç mimari varmış algısı | Orta | Orta | Açık mock disclaimer |
| R-14 | Windows Docker sorunu | Orta | Orta | Portable paths/smoke |

## Backlog item Definition of Done

- Derlenir ve standartlara uyar.
- Unit ve gerekiyorsa integration testi vardır.
- İlgili acceptance criterion otomatik doğrulanır.
- Failure path testlidir.
- Canlı LLM zorunlu değildir.
- Spec/API/tool dokümanı günceldir.
- Input validation, log redaction ve write etkisi değerlendirilmiştir.
- Correlation log/metric vardır.

## MVP release checklist

- [x] `mvn verify`
- [x] Unit/integration/ATDD pass
- [x] Testcontainers pgvector pass
- [ ] `docker compose up --build`
- [ ] Health UP
- [x] Ana fixture doğru
- [x] INC-2026-041 citation
- [x] Onaysız incident yok
- [x] Idempotency pass
- [x] Prompt injection pass
- [x] Tool budget pass
- [x] Stub API key olmadan çalışır
- [ ] README quickstart
- [ ] 5–7 dakika demo
- [ ] Secret scan temiz
- [ ] Mock olduğu açık

### M7 status

M7 (REST/approval) satisfied US-011/012/013 rows above: preview, human-approval, and idempotency
Gherkin scenarios (`docs/12`) all pass, closing the `Idempotency pass` checkbox. `GlobalExceptionHandler`
now maps `dev.langchain4j.exception.HttpException` → `502 MODEL_PROVIDER_ERROR` and
`dev.langchain4j.exception.TimeoutException` → `504 INVESTIGATION_TIMEOUT`; both are live-model-only
transport failure paths (AI_MODE=live) and are intentionally not exercised by the offline stub test
suite. `docker compose up --build`, `README quickstart`, demo, and secret scan remain M8 per `docs/14`.
