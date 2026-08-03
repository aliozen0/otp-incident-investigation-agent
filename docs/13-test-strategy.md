# 13 — Test Strategy

## Hedef

LLM sisteminin yalnız cevap üretmesini değil; güvenli, kaynaklı, tekrar üretilebilir ve entegrasyona uygun davranmasını doğrulamak.

## Test katmanları

### Unit

- TimeWindowResolver
- anomaly calculation
- domain invariant
- claim validator
- action policy
- idempotency
- DTO mapping
- PII redaction
- intent decision and suggestion validation
- visualization evidence/value/unit/limit validation
- bounded semantic context LRU/session isolation

### Component

- her mock tool
- RAG ingestion/retrieval
- incident adapter
- model stub
- purpose-specific router and tool-free chat stubs
- stable general-knowledge chat prompt boundary (zero tool, zero freshness claim)
- typed visualization renderer for every allowlisted type

### Integration

Testcontainers ile:

- PostgreSQL + pgvector
- Flyway
- repository
- vector similarity
- REST + DB
- idempotency constraint
- `/chat/messages` route branches with zero-tool/zero-save assertions
- visualization POST/GET canonical round trip and old-row empty-list migration

### Local-live compatibility

- NVIDIA model admission requires a real sequential two-tool round trip plus typed structured output.
- Single-tool-call provider compatibility is covered with the Llama 3.1 8B candidate in the same gate.
- `local-live` tests require an explicit API key and are excluded from the offline main suite.

### Contract

- API schema
- tool input/output
- error format
- Java/PHP plain JSON uyumu

### ATDD

`12-atdd-gherkin.md` içindeki senaryolar.

### Live model evaluation

CI zorunlu değil:

- ana fixture doğruluğu
- unsupported claim rate
- citation completeness
- tool selection
- latency/token cost
- semantic intent routing and selected-model identity
- absence of tool specifications on chat/clarification calls
- evidence-bound visualization selection

## LLM test yaklaşımı

### CI: deterministic stub

Stub model:

- beklenen tool çağrılarını ister,
- fixture'a uygun result üretir,
- invalid JSON ve failure senaryolarını taklit eder.
- greeting, identity, clarification, investigation ve invalid-router-repair senaryoları için request
  başına purpose-specific deterministik davranır; global tükenen script paylaşmaz.

### Live profile

Exact text değil şunlar test edilir:

- status/severity
- hypothesis order
- evidence refs
- forbidden action absence
- confidence range
- knowledge source
- semantik keyword grubu

## Fixture standardı

Her fixture:

- immutable ID
- timestamps
- inputs
- expected domain result
- expected calls
- forbidden outcomes

Ana fixture: `OTP-DROP-001`.

## Güvenlik testleri

- log secret scan
- no OTP/phone
- approval scope
- prompt injection
- unknown provider
- arbitrary URL rejection
- oversized question
- investigation sırasında write tool blocked
- CHAT/CLARIFICATION gerçek model request'inde tool specification yok
- route/suggestion/chart PII, HTML ve prompt-injection validation
- unknown evidence/fabricated value/incompatible unit/oversize visualization rejection

## Performance

- 20 concurrent stub investigation
- p95 latency
- DB connection usage
- no duplicate incident
- tool budget enforcement

## CI kalite kapıları

- build
- unit/integration/ATDD
- migration test
- format/static analysis
- vulnerability scan
- secret scan
- Docker image build
- smoke test

Domain/application için %80 coverage önerilir; kritik kurallar scenario ile kapsanmalıdır.
