## Görev: Endpoint — `{METHOD} {PATH}`

Kaynak: `docs/06-api-contracts.md`.

### Sözleşme

- Request şeması: {REQUEST_ALANLARI}
- Response şeması: {RESPONSE_ALANLARI}
- Hata kodları: {ERROR_CODES, örn: 400 INVALID_TIME_WINDOW, 422 QUESTION_NOT_ACTIONABLE}
- İlgili acceptance criterion: {AC_ID}

### Kısıtlar

- Base path `/api/v1`, problem-details hata formatı, `X-Correlation-Id` desteği.
- Write endpoint ise `Idempotency-Key` header zorunlu.
- Validation: {VALIDATION_KURALLARI, örn: question 10-1000 char, aralık 1dk-24saat, gelecek zaman red}.
- Handler yalnızca application service'e delege eder; iş kuralı controller'da yazılmaz.

### Sırayla

1. Contract test yaz (request/response şekli + hata kodları).
2. DTO + validation.
3. Controller → application service çağrısı.
4. Hata mapping (problem details).
5. Swagger örneği güncelle.

### Bitti sayılması için

- Contract test geçiyor.
- Geçersiz input için doğru error code dönüyor.
- `docs/17` traceability tablosunda ilgili AC işaretli.

### Oturum sonu

`prompts/08-session-report.md` kuralına göre rapor yaz ve `SESSION_LOG.md`'ye satır ekle.
