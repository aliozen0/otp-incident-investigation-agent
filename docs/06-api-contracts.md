# 06 — REST API Contracts

## Genel standartlar

- Base path: `/api/v1`
- Content type: `application/json`
- Timestamp: ISO-8601 UTC
- Correlation header: `X-Correlation-Id`
- Write header: `Idempotency-Key`
- Hata biçimi: problem details

## POST `/api/v1/investigations`

### Request

```json
{
  "question": "Son 15 dakikada OTP teslimat oranı neden düştü?",
  "timeWindow": {
    "startAt": "2026-07-30T11:15:00Z",
    "endAt": "2026-07-30T11:30:00Z"
  },
  "locale": "tr-TR"
}
```

`timeWindow` verilmezse soru içinden çözümlenir.

### Validation

- question: 10–1000 karakter
- aralık: 1 dakika–24 saat
- gelecek zaman kabul edilmez
- locale allowlist

### Success

```json
{
  "investigationId": "2c321a4e-178f-4f68-b705-18f188d73e75",
  "status": "ANOMALY_CONFIRMED",
  "severity": "HIGH",
  "summary": "OTP başarısı %98,1'den %72,1'e düştü ve başarısızlıklar Operatör B üzerinde yoğunlaştı.",
  "timeWindow": {
    "startAt": "2026-07-30T11:15:00Z",
    "endAt": "2026-07-30T11:30:00Z",
    "timezone": "UTC"
  },
  "evidence": [
    {
      "id": "ev-provider-rate",
      "sourceType": "PROVIDER_HEALTH",
      "sourceReference": "tool:getProviderHealth:exec-5",
      "observation": "Operatör B timeout oranı %31 ve circuit breaker HALF_OPEN.",
      "observedAt": "2026-07-30T11:30:00Z"
    }
  ],
  "hypotheses": [
    {
      "rank": 1,
      "possibleCause": "Gateway bağlantı havuzunda kapasite veya connection release problemi",
      "probability": "HIGH",
      "supportingEvidenceIds": ["ev-provider-rate", "ev-connections", "ev-prior-incident"],
      "verificationSteps": [
        "Gateway v2.4 connection pool metriklerini incele",
        "Önceki sürümle connection lifecycle değişikliklerini karşılaştır"
      ]
    }
  ],
  "recommendedActions": [
    {
      "actionType": "MANUAL_CHECK",
      "description": "Operatör B bağlantı havuzunu incele",
      "risk": "LOW",
      "requiresApproval": false
    },
    {
      "actionType": "CHANGE_PROPOSAL",
      "description": "Rollback seçeneğini change-management sürecine sun",
      "risk": "HIGH",
      "requiresApproval": true
    }
  ],
  "knowledgeReferences": [
    {
      "documentId": "INC-2026-041",
      "version": "1",
      "chunkId": "chunk-2",
      "title": "Operatör timeout ve connection pool olayı",
      "similarityScore": 0.86
    }
  ],
  "confidence": 0.87,
  "approvalRequired": true,
  "validation": {
    "status": "PASSED",
    "warnings": ["Deploy ile hata başlangıcı arasında korelasyon vardır; nedensellik doğrulanmamıştır."]
  }
}
```

### Errors

- `400 INVALID_TIME_WINDOW`
- `400 INVALID_REQUEST`
- `422 QUESTION_NOT_ACTIONABLE`
- `429 INVESTIGATION_RATE_LIMITED`
- `502 MODEL_PROVIDER_ERROR`
- `504 INVESTIGATION_TIMEOUT`

## GET `/api/v1/investigations/{id}`

Canonical persisted result snapshot'ını döndürür.

`summary` modelin ürettiği ve deterministik validator'dan geçen doğal dil özetidir. Persist edilen
snapshot tekrar okunduğunda değişmez. `knowledgeReferences` yalnızca model çıktısından kopyalanmaz;
uygulamanın retrieval sırasında topladığı canonical `documentId`, `version`, `title`, `chunkId` ve
`similarityScore` alanlarıyla zenginleştirilir.

## GET `/api/v1/models`

Geriye uyumluluk için `models` alanında doğrulanmış model ID listesi korunur. `options` alanı
composer'ın kullanacağı kullanıcı dostu metadata'yı, `defaultModelId` varsayılan seçimi taşır.
Yalnızca canlı NVIDIA NIM tool-calling + structured-output compatibility testi geçen modeller
listelenir.

## GET `/api/v1/knowledge/documents`

Belge envanterini tam metadata ve chunk sayısıyla döndürür. Ham/unsanitized içerik dönmez.

## GET `/api/v1/knowledge/documents/{documentId}/versions/{version}`

Belgenin metadata'sını, sanitize edilmiş canonical içeriğini ve chunk ayrıntılarını döndürür.

## POST `/api/v1/knowledge/search-preview`

Salt-okunur RAG doğrulama endpoint'idir. `query` zorunlu, `provider` opsiyonel ve `topK` 1–5
aralığındadır. Sonuçlar `documentId`, `version`, `title`, `chunkId`, `sectionTitle`, sanitize edilmiş
content excerpt ve `similarityScore` taşır. Tool budget veya incident approval akışını değiştirmez.

## POST `/api/v1/investigations/{id}/incident-draft/preview`

Kalıcı kayıt oluşturmadan taslak gösterir.

```json
{
  "title": "[HIGH] OTP delivery degradation on Operator B",
  "severity": "HIGH",
  "summary": "Son 15 dakikada OTP başarısı %72,1'e düştü.",
  "evidenceCount": 6,
  "recommendedChecks": ["Connection pool metriklerini incele", "Provider durumunu doğrula"],
  "requiresExplicitApproval": true
}
```

## POST `/api/v1/investigations/{id}/incident-draft/decisions`

Header:

```text
Idempotency-Key: 9094e929-4efa-4bd1-ae92-37ff9e587a9a
```

Approve:

```json
{
  "decision": "APPROVE",
  "reason": "Teknik ekip incelemesi için incident gerekli."
}
```

İlk cevap `201`:

```json
{
  "incidentDraftId": "5b14fbad-bc66-49e5-89c4-96f8e56d3813",
  "externalIncidentId": "DEMO-INC-0001",
  "status": "CREATED",
  "idempotentReplay": false
}
```

Aynı key tekrarı `200`, aynı ID ve `idempotentReplay=true`.

Reject:

```json
{
  "decision": "REJECT",
  "reason": "Provider bakım duyurusu doğrulandı."
}
```

## Error response

```json
{
  "type": "https://errors.example.local/investigation-timeout",
  "title": "Investigation timed out",
  "status": 504,
  "detail": "The investigation exceeded the configured deadline.",
  "instance": "/api/v1/investigations",
  "correlationId": "corr-ec3c",
  "errorCode": "INVESTIGATION_TIMEOUT"
}
```

## Uyumluluk ilkeleri

- Plain JSON
- Enum'lar açık
- Unknown response alanları istemci tarafından görmezden gelinebilir
- Breaking değişiklik yeni major API versiyonunda
- Framework'e özel client zorunlu değil
