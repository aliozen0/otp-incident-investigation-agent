# 10 — Observability and SLO

## Amaç

Sistem yalnızca sonuç üretmemeli; sonucun nasıl üretildiği izlenebilmelidir.

## Correlation

Her request şu kimlikleri ilişkilendirir:

- correlationId
- investigationId
- toolExecutionId
- approvalId

## Structured log örneği

```json
{
  "event": "tool_execution_completed",
  "correlationId": "corr-123",
  "investigationId": "inv-456",
  "toolName": "getProviderHealth",
  "status": "SUCCESS",
  "durationMs": 74,
  "resultSizeBytes": 812
}
```

Loglanmaz:

- API key
- Authorization header
- OTP
- telefon numarası
- raw prompt
- büyük raw payload

## Metrics

### API

- http requests/status/latency

### Investigation

- investigation total/duration/status
- partial count
- validation failure count

### Tools

- tool calls/duration/errors
- budget exhausted

### RAG

- query duration
- result count
- no result
- top score

### LLM

- calls/duration
- input/output token
- schema repair
- errors

### Approval

- preview
- approve/reject
- idempotent replay

## Trace örneği

```text
investigation
├── resolve_time_window
├── collect_initial_metrics
├── agent_tool_loop
│   ├── error_distribution
│   ├── queue_health
│   ├── provider_health
│   └── recent_changes
├── rag_retrieval
├── llm_generate
├── validate_analysis
└── persist_result
```

LLM span raw prompt yerine modelName, promptVersion, tokenCount, finishReason ve duration taşır.

## Demo SLO'ları

| SLI | Hedef |
|---|---|
| Health p95 | < 250 ms |
| Mock tool p95 | < 300 ms |
| Stub investigation p95 | < 3 s |
| Live investigation | < 30 s |
| Ana ATDD | %100 |
| Kaynaksız evidence | 0 |
| Onaysız incident | 0 |

## Health

### Liveness

Process çalışıyor mu?

### Readiness

- database
- pgvector extension
- migration
- fixture
- model config

Stub profilde dış model aranmaz.

## Production vizyonu alert'leri

- validation failure > %5
- model timeout artışı
- tool failure > %10
- RAG no-result artışı
- audit failure
- idempotency conflict
- token maliyetinde ani artış
