# M2 — Fixture tools

## Durum
DONE

## Kapsam
`com.example.otpsentinel.tools` paketi altında beş operasyonel tool için port (interface) + istek/sonuç `record`'ları + ortak `ToolResult<T>`/`ToolStatus`/`ToolError` zarfı, ve `tools.fixtures` altında `OTP-DROP-001` (docs/15'teki tam sayılarla, birebir), `OTP-NORMAL-001`, `OTP-PARTIAL-001`, `OTP-RAG-NONE-001`, `OTP-INJECTION-001` için deterministik in-code fixture verisi (`FixtureCatalog`) ve beş fixture adapter implementasyonu (saf Java, Spring/LangChain4j yok) yazıldı. `getProviderHealth` için `OTP-PARTIAL-001`'de `ToolStatus.TIMEOUT` senaryosu test edilebilir şekilde eklendi.

## Değişen dosyalar
- `src/main/java/.../tools/ToolStatus.java`, `ToolError.java`, `ToolResult.java` — ortak envelope (SUCCESS/TIMEOUT/ERROR, invariant: SUCCESS→data zorunlu, diğerleri→error zorunlu).
- `src/main/java/.../tools/OtpMetricsRequest.java`, `PeriodComparison.java`, `OtpMetricsResult.java`, `OtpMetricsTool.java` — T-001.
- `src/main/java/.../tools/ErrorDistributionRequest.java`, `ErrorCount.java`, `ProviderErrorBreakdown.java`, `ErrorDistributionResult.java`, `ErrorDistributionTool.java` — T-002.
- `src/main/java/.../tools/QueueHealthResult.java`, `QueueHealthTool.java` — T-003 (girdi yok, spec'te tanımlı değil).
- `src/main/java/.../tools/ProviderHealthRequest.java`, `ProviderHealthResult.java`, `ProviderHealthTool.java` — T-004.
- `src/main/java/.../tools/RecentChangesRequest.java`, `ChangeEvent.java`, `RecentChangesResult.java`, `RecentChangesTool.java` — T-005.
- `src/main/java/.../tools/fixtures/FixtureId.java`, `FixtureScenario.java`, `FixtureCatalog.java` — fixture loader (in-code sabit, deterministik, AI-006).
- `src/main/java/.../tools/fixtures/FixtureOtpMetricsTool.java`, `FixtureErrorDistributionTool.java`, `FixtureQueueHealthTool.java`, `FixtureProviderHealthTool.java`, `FixtureRecentChangesTool.java` — beş fixture adapter (her biri `Clock` enjekte edilebilir, testte deterministik `observedAt`).
- `src/test/java/.../tools/fixtures/*Test.java` (5 dosya) — her tool için component test, `OTP-DROP-001` sayı doğrulaması ve timeout senaryosu dahil.

## Testler (gerçek komut + gerçek çıktı)
Komut: `wsl -e bash -lc "cd /mnt/c/Users/Ali/Downloads/otp-incident-agent && docker run --rm -v $(pwd):/build -v maven-repo:/root/.m2 -w /build maven:3.9-eclipse-temurin-21 mvn -B verify"`

Çıktı özeti:
```
Running com.example.otpsentinel.tools.fixtures.FixtureErrorDistributionToolTest
Tests run: 2, Failures: 0, Errors: 0
Running com.example.otpsentinel.tools.fixtures.FixtureOtpMetricsToolTest
Tests run: 2, Failures: 0, Errors: 0
Running com.example.otpsentinel.tools.fixtures.FixtureProviderHealthToolTest
Tests run: 3, Failures: 0, Errors: 0
Running com.example.otpsentinel.tools.fixtures.FixtureQueueHealthToolTest
Tests run: 1, Failures: 0, Errors: 0
Running com.example.otpsentinel.tools.fixtures.FixtureRecentChangesToolTest
Tests run: 2, Failures: 0, Errors: 0
...
Tests run: 44, Failures: 0, Errors: 1, Skipped: 0
[ERROR] OtpSentinelApplicationSmokeTest » IllegalState Could not find a valid Docker environment
BUILD FAILURE (spotless:check ayrıca 42 preexisting dosyada CRLF/LF farkı buldu)
```
Tüm 10 yeni tool testi (M2 kapsamı) geçti. Kalan tek test hatası (`OtpSentinelApplicationSmokeTest`, Testcontainers nested-Docker/socket erişimi) ve `spotless:check`'in bulduğu CRLF farkları **M2'den önce mevcut, M2 kapsamı dışı** — bkz. aşağıdaki "Spec çelişkisi" bölümü. `mvn verify` bu iki preexisting sorun yüzünden BUILD FAILURE veriyor; M2'nin kendi testleri (`mvn -Dtest='com.example.otpsentinel.tools.**' test`) ayrıca izole çalıştırılıp 10/10 yeşil doğrulandı.

## Karşılanan requirement/AC
- T-001 `getOtpMetrics` → AC-001, AC-002: current/previous dönem sayıları `docs/15` ile birebir (`FixtureOtpMetricsToolTest`).
- T-002 `getErrorDistribution` → AC-003: hata kodu + provider dağılımı birebir, provider filtreleme testli.
- T-003 `getQueueHealth` → AC-004: queue JSON birebir.
- T-004 `getProviderHealth` → AC-003, AC-005: OPERATOR_B sağlık verisi birebir + `OTP-PARTIAL-001` timeout senaryosu (NFR-008) + bilinmeyen provider için uydurma veri yerine `ToolStatus.ERROR`.
- T-005 `getRecentChanges` → AC-007: 4 değişiklik olayı birebir, component filtresi testli, "yalnızca zaman ilişkisi" notu Javadoc'ta.
- FR-007 (read-only): tüm adapter'lar yalnızca okuma yapıyor, hiçbir write/side-effect yok.
- FR-005/FR-006 (allowlist/budget): bu milestone'da agent orchestration yok; port arayüzleri M5'te budget/allowlist uygulayacak agent katmanına temiz bir sınır sunuyor.
- NFR-008 (timeout): `FixtureScenario.timedOutProvider` + `FixtureProviderHealthTool` ile test edilebilir timeout simülasyonu.
- AI-006 (deterministic fallback): fixture veri kod içi sabit, `Clock` enjeksiyonu ile `observedAt` de testte deterministik.

## Karşılanmayan / ertelenen
- LangChain4j `@Tool` binding, RAG, REST, persistence — kapsam dışı (M3+/M4/M5), dokunulmadı.
- `docs/17-traceability-risk-dost.md` tool traceability tablosu zaten mevcut implementasyonla tutarlı; güncelleme gerekmedi.

## Spec çelişkisi/belirsizlik (varsa)
1. **current/previous evidence ayrımı (M1 raporunda not düşülmüştü):** `OtpMetricsResult.previousPeriod` (`PeriodComparison`) yalnızca docs/15'te verilen alanları taşıyor: `window`, `total`, `successRate`, `averageDeliverySeconds`. Önceki dönem için `delivered`/`failed` docs/15'te verilmediği için hesaplanıp/yuvarlanıp uydurulmadı — alan olarak eklenmedi. Domain `Evidence` VO'suna eşleme (her sayısal alan için ayrı bir `Evidence` — örn. `previousPeriod.successRate` → kendi `sourceReference`'ı olan bir Evidence) bir **agent/application katmanı (M5) kararı**; M1 domain modelinde `Evidence` zaten generic (metricName/metricValue) olduğundan hiçbir domain değişikliği gerekmedi.
2. **OTP-NORMAL-001 eksik sayılar:** docs/15 bu senaryo için yalnızca "%98.4, NO_ANOMALY" veriyor. Diğer dört tool'un tam sayıları (queue/provider-health/recent-changes/error-distribution) belgede yok; `FixtureCatalog.normalScenario()` bunları yuvarlama olmadan tam oranı verecek şekilde (9840/10000=%98.40 tam) sentezledi — bu senaryodaki **yalnızca successRate** normatif bir docs/15 iddiasıdır, diğer alanlar test/demo amaçlı üretildi. `FixtureCatalog` Javadoc'unda açıkça belirtildi.
3. **OTP-RAG-NONE-001 / OTP-INJECTION-001:** docs/15 bunları yalnızca knowledge/RAG farkı olarak tanımlıyor ("live evidence var, knowledge yok" / "knowledge içinde kötü niyetli talimat"); M2 kapsamında knowledge/RAG (T-006) yok. Bu iki fixture, tool verisi için `OTP-DROP-001` veri setini aynen yeniden kullanıyor (`FixtureScenario.withId`), çünkü belge tool tarafında farklı bir sayı belirtmiyor.
4. **Ön var olan CRLF/spotless ve Testcontainers-in-Docker sorunları:** M2 öncesinde de mevcuttu (oturum başlangıcındaki `git status` zaten 10 dosyada CRLF farkı gösteriyordu). Bu oturumda dokunulmadı (scope creep riski); `mvn verify` bu iki nedenden BUILD FAILURE veriyor, M2'nin kendi testleri izole çalıştırılıp yeşil doğrulandı. Bir sonraki oturumda ayrı bir `fix/` branch'inde ele alınmalı.

## Sonraki oturum için not
M3 (persistence/audit) başlatılabilir; ayrıca ayrı bir `fix/spotless-line-endings` ve `fix/testcontainers-docker-socket` görevi (repo genelinde `mvn verify`'ı yeşile çevirmek için) önerilir.
