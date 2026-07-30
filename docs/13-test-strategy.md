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

### Component

- her mock tool
- RAG ingestion/retrieval
- incident adapter
- model stub

### Integration

Testcontainers ile:

- PostgreSQL + pgvector
- Flyway
- repository
- vector similarity
- REST + DB
- idempotency constraint

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

## LLM test yaklaşımı

### CI: deterministic stub

Stub model:

- beklenen tool çağrılarını ister,
- fixture'a uygun result üretir,
- invalid JSON ve failure senaryolarını taklit eder.

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
