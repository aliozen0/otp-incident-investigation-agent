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
| US-014 Normal operational chat | FR-020, FR-022, SEC-007 | AC-041, AC-042, AC-051 |
| US-015 Clarification | FR-020, FR-023 | AC-044, AC-050 |
| US-016 Intent-routed investigation | FR-021, FR-024 | AC-043, AC-045 |
| US-017 Session context | FR-025, NFR-011 | AC-046 |
| US-018 Safe visualization | FR-026–FR-028, SEC-008 | AC-047–AC-049 |

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
| R-15 | Her mesajın investigation'a dönüşmesi | Orta | Yüksek | Tool-free LLM route + typed Java gate |
| R-16 | Route prompt injection / invalid output | Orta | Yüksek | Explicit mode policy, schema validation, repair once, fail closed |
| R-17 | Chat tool sızıntısı | Düşük | Kritik | Ayrı tool-free adapter + request-level tool specification tests |
| R-18 | Grafik halüsinasyonu | Orta | Yüksek | Canonical evidence/value/unit binding + bounded schema |
| R-19 | Session context sızıntısı | Düşük | Yüksek | Separate bounded LRU contexts + isolation tests |

## Backlog item Definition of Done

- Derlenir ve standartlara uyar.
- Unit ve gerekiyorsa integration testi vardır.
- İlgili acceptance criterion otomatik doğrulanır.
- Failure path testlidir.
- Canlı LLM zorunlu değildir.
- Spec/API/tool dokümanı günceldir.
- Input validation, log redaction ve write etkisi değerlendirilmiştir.
- Correlation log/metric vardır.
- Response branch'inde izin verilmeyen tool/persistence olmadığını ve visualization provenance'ını
  kullanıcı görünür integration/ATDD testi doğrular.

## MVP release checklist

- [x] `mvn verify`
- [x] Unit/integration/ATDD pass
- [x] Testcontainers pgvector pass
- [x] `docker compose up --build`
- [x] Health UP
- [x] Ana fixture doğru
- [x] INC-2026-041 citation
- [x] Onaysız incident yok
- [x] Idempotency pass
- [x] Prompt injection pass
- [x] Tool budget pass
- [x] Stub API key olmadan çalışır
- [x] README quickstart
- [ ] 5–7 dakika demo (ayrı, insan-anlatımlı olarak bu oturumda zamanlanmadı — bkz. M8 status)
- [x] Secret scan temiz
- [x] Mock olduğu açık

### M7 status

M7 (REST/approval) satisfied US-011/012/013 rows above: preview, human-approval, and idempotency
Gherkin scenarios (`docs/12`) all pass, closing the `Idempotency pass` checkbox. `GlobalExceptionHandler`
now maps `dev.langchain4j.exception.HttpException` → `502 MODEL_PROVIDER_ERROR` and
`dev.langchain4j.exception.TimeoutException` → `504 INVESTIGATION_TIMEOUT`; both are live-model-only
transport failure paths (AI_MODE=live) and are intentionally not exercised by the offline stub test
suite. `docker compose up --build`, `README quickstart`, demo, and secret scan remain M8 per `docs/14`.

### M8 status

- `mvn -B spotless:apply verify`: `BUILD SUCCESS`, `Tests run: 141, Failures: 0, Errors: 0, Skipped: 0` (141 = 137 tests on `main` + 4 new tests on this branch: 3 exception-mislabeling regression tests + 1 stub-script-exhaustion regression test), HEAD `85ac762`.
- `docker compose up --build` verified clean (`docker compose down -v && up --build -d`, WSL2, host port 5432 free, no `POSTGRES_PORT` override needed): `db` healthy, `app` healthy/Up, `GET /actuator/health` -> `{"status":"UP"}`.
- `scripts/demo.sh` run end to end (`time` real 0.446s, well under a minute): investigation created (`ANOMALY_CONFIRMED`/`HIGH`), preview generated (`requiresExplicitApproval=true`), approved (`incidentDraftId=0de1f2c8-a807-42b0-b7c2-d87f37780d18`, `externalIncidentId=DEMO-INC-4A8472AF`), replayed with the same Idempotency-Key -> `idempotentReplay=true`, same `incidentDraftId`.
- README quickstart + API walkthrough re-run verbatim against the same live environment (separately from `demo.sh`): `docker compose up --build`, health check, Swagger UI (`/swagger-ui/index.html` -> HTTP 200), and the 5-step curl walkthrough all worked with no undocumented flags; second idempotency replay again returned `idempotentReplay=true` with the same `incidentDraftId`.
- Secret scan: `git log -p milestone/M8-demo-readiness ^main | grep -iE "nvapi-|api[_-]?key.*=|password.*="` -> clean (no match); `grep -rn "nvapi-" . --include=*.md --include=*.yml --include=*.java --include=*.env*` -> only doc/plan files mentioning the literal string `nvapi-` as documentation text (this checklist and the M8 plan doc itself), no real key; `git ls-files .env` -> empty (untracked, confirmed).
- Mock/PoC disclaimer present in README ("Bu bir mock/PoC'tur" section, with explicit "Mimari" and "MVP dışı" sections listing no real OTP/customer/provider integration).
- 5-7 minute demo: scripted flow (`scripts/demo.sh`) runs in well under a minute as required; the 5-7 minute figure is presenter narration time per `docs/18-demo-interview-guide.md` and was not separately re-timed with a human narrator in this session — left unticked above for that reason (the script's own runtime is verified and evidenced here).
