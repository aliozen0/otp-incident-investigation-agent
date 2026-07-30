## Görev: M3 — Persistence/audit

Önce: `git checkout main && git pull && git checkout -b milestone/M3-persistence-audit` (bkz. `docs/20-git-workflow.md`).

Kaynak: `docs/14-implementation-plan.md` → **M3 — Persistence/audit**.

İlgili spec dosyaları: `docs/05-domain-and-architecture.md` (aggregate/port tanımları — M1'de yazıldı), `docs/03-system-requirements.md` (FR-016 re-fetch, FR-017 audit event listesi, DATA-005 append-only audit, SEC-006 idempotent+audit write), `docs/09-security-governance.md` (audit alanları), `docs/13-test-strategy.md` (Testcontainers entegrasyon katmanı).

### Kapsam

Yalnızca şunu yap:

1. Flyway migration'ları: `Investigation`, `IncidentDraft` ve audit event tablolarının gerçek şeması (M0'daki `V1__baseline.sql` sadece pgvector extension kuruyordu; buraya `V2__...` ekle).
2. `InvestigationRepository` ve `IncidentDraftRepository` portlarının (M1'de tanımlandı) Spring Data JDBC/JPA (`docs/19` dependency kategorisine göre) implementasyonu — `adapters` paketinde, `domain` paketine dokunmadan.
3. Append-only audit event kaydı: `docs/03-system-requirements.md` FR-017'deki olay listesi (request accepted, time window resolved, tool called/completed/failed, RAG completed, LLM completed, validation passed/failed, preview generated, approval/rejection, incident created) için bir `AuditEvent` yapısı + repository. Bu milestone'da henüz gerçek tetikleyiciler (tool/RAG/LLM) yok — sadece mekanizmayı ve şemayı kur, birkaç temsili event tipiyle test et.
4. Idempotency: aynı `idempotencyKey` ile ikinci `IncidentDraft` persist denemesi DB seviyesinde (unique constraint) engellensin, tek kayıt dönsün.
5. Testcontainers PostgreSQL entegrasyon testleri: repository CRUD, restart sonrası GET (persist edilen Investigation tekrar okunabiliyor), duplicate idempotency key'in tek kayıt ürettiği.

Henüz **yazma**: RAG/pgvector similarity search (M4), LangChain4j/agent (M5), REST endpoint (M7). Repository'ler bu milestone'da application/test kodundan çağrılacak, controller'dan değil.

Bu milestone'un "Kabul" kriteri: restart sonrası GET aynı sonucu döner, duplicate approval tek kayıt üretir (`docs/14` M3 kabul cümlesi).

### Sırayla

1. İlgili requirement/acceptance criterion'ı belirle (AC-014 idempotency, AC-030 GET canonical snapshot, FR-015, FR-016, FR-017, SEC-006).
2. Değişecek dosyaları listele (`src/main/resources/db/migration/V2__*.sql`, `adapters` paketi altında repository implementasyonları, audit event sınıfı/tablosu, `src/test/.../adapters/**` Testcontainers testleri).
3. Önce failing test yaz: Testcontainers ile "persist et → restart simülasyonu (yeni repository instance) → GET aynı veriyi döndürür", "aynı idempotency key ikinci kez CREATE dener → tek kayıt, ikinci çağrı eski ID'yi döner".
4. Minimum implementasyonla geçir.
5. Refactor.
6. Gerekirse `docs/05-domain-and-architecture.md`'deki port imzalarını (implementasyon değil, varsa eksik metod) küçük ekleme ile tamamla; büyük domain değişikliği gerekiyorsa spec çelişkisi olarak raporla.

### Kısıtlar

- `domain` paketine Spring/JPA import etme (M1'in NFR-002 sınırı burada da geçerli) — repository implementasyonu `adapters` paketinde, port'u `domain`'de arayüz olarak kalır.
- Idempotency DB constraint ile garanti edilmeli, yalnızca application-kodu kontrolüne güvenme (yarış durumu riski).
- Audit event'lerde secret/OTP/telefon/PII olamaz (`docs/09-security-governance.md`).
- Bu milestone dışına taşma: RAG, agent, REST kodu yazma.
- Tüm `mvn`/`docker` komutları WSL2 üzerinden (`docs/19-technology-baseline.md` → "Yerel çalıştırma ortamı").
- Commit etmeden önce `mvn spotless:apply` çalıştır, `mvn verify`'ın tamamını (yalnız yeni testleri değil) yeşil gördüğünü doğrula — M2'de "preexisting sorun" diye BUILD FAILURE'ı görmezden gelme hatası tekrarlanmasın.

### Bitti sayılması için

- Testcontainers entegrasyon testleri geçiyor (gerçek çalıştırma, gerçek çıktı).
- Restart-sonrası-GET ve duplicate-idempotency-key senaryoları ayrı testlerle kanıtlı.
- `mvn verify` (tüm proje, yalnızca yeni paket değil) BUILD SUCCESS.
- `docs/17-traceability-risk-dod.md` DoD listesine uyuyor.

### Oturum sonu

`prompts/08-session-report.md` kuralına göre `prompts/handoff/M3-report.md` dosyasını yaz ve `SESSION_LOG.md`'ye satır ekle. Branch adı `milestone/M3-persistence-audit`, commit convention `docs/20-git-workflow.md`.
