## Görev: Yeni tool — `{TOOL_NAME}`

Kaynak: `docs/07-agent-tool-spec.md`.

### Tanım

- Amaç: {TOOL_AMAC}
- Input record alanları: {INPUT_ALANLARI}
- Output record alanları: {OUTPUT_ALANLARI}
- İlgili acceptance criterion: {AC_ID}

### Kısıtlar (07-agent-tool-spec.md ile aynı hizada olmalı)

- Salt-okunur; investigation sırasında write yok.
- `ToolResult<T>` envelope'una uy (executionId, toolName, status, observedAt, data, error).
- Allowlist'e ekle; agent yalnızca tanımlı tool'ları çağırabilir.
- Timeout {TOOL_TIMEOUT_MILLIS_ONERI, varsayılan 2000ms}, 1 transient retry.
- Aynı tool+aynı parametre başarılı çağrı tekrarlanmaz (mevcut budget mekanizmasına tak).

### Sırayla

1. Fixture adapter'da mock veri ekle/genişlet (`docs/15-demo-fixtures.md` ile tutarlı).
2. Port arayüzünü domain/application katmanında tanımla.
3. Adapter implementasyonu (Spring/LangChain4j detayı yalnızca adapter'da).
4. `@Tool` binding.
5. Component test: mock adapter + timeout simülasyonu.
6. Tool traceability tablosunu güncelle (`docs/17-traceability-risk-dod.md`).

### Bitti sayılması için

- Component test geçiyor.
- Timeout/retry davranışı testli.
- Evidence mapping'e (`ev-*` id üretimi uygulamada, modelde değil) uyuyor.

### Oturum sonu

`prompts/08-session-report.md` kuralına göre rapor yaz ve `SESSION_LOG.md`'ye satır ekle.
