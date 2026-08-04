## Görev: M2 — Fixture tools

Önce: `git checkout main && git pull && git checkout -b milestone/M2-fixture-tools` (bkz. `docs/20-git-workflow.md`).

Kaynak: `docs/14-implementation-plan.md` → **M2 — Fixture tools**.

İlgili spec dosyaları: `docs/07-agent-tool-spec.md` (tool catalog, `ToolResult<T>` envelope), `docs/15-demo-fixtures.md` (`OTP-DROP-001` ve negatif fixture verileri), `docs/03-system-requirements.md` (FR-005 allowlist, FR-006 budget, FR-007 read-only, NFR-008 timeout/retry), `docs/17-traceability-risk-dod.md` (tool traceability tablosu).

### Kapsam

Yalnızca şunu yap:

1. **Fixture loader**: `OTP-DROP-001`, `OTP-NORMAL-001`, `OTP-PARTIAL-001`, `OTP-RAG-NONE-001`, `OTP-INJECTION-001` fixture verilerini (`docs/15-demo-fixtures.md`'deki tam sayılarla) immutable Java yapılarına yükleyen bir mekanizma (kod içi sabit veya kaynak dosyadan okuma — sen karar ver, ama deterministik ve testli olsun).
2. Beş tool'un **fixture adapter** implementasyonu (henüz LangChain4j `@Tool` binding yok, sadece port+adapter, saf Java):
   - `getOtpMetrics` (T-001)
   - `getErrorDistribution` (T-002)
   - `getQueueHealth` (T-003)
   - `getProviderHealth` (T-004)
   - `getRecentChanges` (T-005)
3. Ortak `ToolResult<T>` envelope (`executionId, toolName, status, observedAt, data, error`).
4. Timeout simülasyonu: `OTP-PARTIAL-001` fixture'ında `getProviderHealth` timeout senaryosu tetiklenebilmeli (test edilebilir bir mekanizma, ör. fixture-driven `ToolStatus.TIMEOUT`).

Henüz **yazma**: LangChain4j agent/tool binding, RAG, REST, persistence, gerçek network/HTTP çağrısı — bunlar M3+/M4/M5.

Bu milestone'un "Kabul" kriteri: her tool component testli ve fixture toplamları (`docs/15-demo-fixtures.md`'deki current/previous/provider/error/queue/provider-health/recent-changes sayılarıyla) tutarlı (`docs/14` M2 kabul cümlesi).

### Sırayla

1. İlgili requirement/acceptance criterion'ı belirle (tool traceability tablosu: getOtpMetrics→AC-001/AC-002, getErrorDistribution→AC-003, getQueueHealth→AC-004, getProviderHealth→AC-003/AC-005, getRecentChanges→AC-007).
2. Değişecek dosyaları listele (`tools` paketi altında port+fixture adapter + `src/test/.../tools/**` + fixture verisi nerede tutulacaksa onun dosyası).
3. Önce failing test yaz: her tool için en az "OTP-DROP-001 için beklenen sayılar" testi + `getProviderHealth` timeout testi.
4. Minimum implementasyonla geçir.
5. Refactor.
6. `docs/17-traceability-risk-dod.md` tool traceability tablosunu gerekiyorsa güncelle.

### Kısıtlar

- Tool adapter'ları salt-okunur; hiçbir write/side-effect yok (FR-007).
- `docs/15-demo-fixtures.md`'deki sayıları uydurma/yuvarlama — birebir kopyala (ör. current success 72.10%, Operatör B success 51.73%, PROVIDER_TIMEOUT 2228 vb.).
- `M1`'de domain'e eklenen `Evidence`/`TimeWindow` gibi tipleri tekrar yazma, mevcut domain tiplerini kullan.
- M1 raporunda not düşülen "current/previous evidence ayrımı" belirsizliğini burada netleştir: `getOtpMetrics` çıktısı (`OtpMetricsResult.previousPeriod`) gerçek current/previous ayrımını taşımalı; domain `Evidence` VO'suna nasıl map edileceğini bu oturumda karara bağla ve raporda açıkla (gerekirse M1 domain modeline küçük bir alan eklemek gerekiyorsa bunu spec çelişkisi olarak ayrıca raporla, sessizce büyük bir domain değişikliği yapma).
- Bu milestone dışına taşma: agent/LangChain4j/RAG/REST kodu yazma.
- Onaylanmamış yeni bağımlılık ekleme.

### Bitti sayılması için

- Her 5 tool için component test geçiyor (gerçek çalıştırma, gerçek çıktı).
- `OTP-DROP-001` fixture'ından üretilen sayılar `docs/15-demo-fixtures.md` ile birebir eşleşiyor (testte assert edilmiş).
- Timeout simülasyonu (`OTP-PARTIAL-001`) testli.
- `docs/17-traceability-risk-dod.md` DoD listesine uyuyor.

### Oturum sonu

`prompts/08-session-report.md` kuralına göre `prompts/handoff/M2-report.md` dosyasını yaz ve `SESSION_LOG.md`'ye satır ekle. Branch adı `milestone/M2-fixture-tools`, commit convention `docs/20-git-workflow.md`. Tüm `mvn`/`docker` komutları repository kökünden (`docs/19-technology-baseline.md` → "Yerel çalıştırma ortamı").
