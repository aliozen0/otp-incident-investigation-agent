# M3 — Persistence/audit

## Durum
DONE

## Kapsam
`Investigation`, `IncidentDraft` ve yeni `AuditEvent` için gerçek Flyway şeması (`V2__domain_schema.sql`) yazıldı; M1'de tanımlanan `InvestigationRepository`/`IncidentDraftRepository` portları `adapters.persistence` paketinde plain `JdbcTemplate` ile implemente edildi (Spring Data JDBC/JPA yerine — bkz. "Spec çelişkisi"). Aggregate'lerin iç listeleri (evidence/hypotheses/recommendedActions/knowledgeReferences/toolExecutions/validationReport/approval) JSONB kolonlarda Jackson ile saklanıyor; domain paketine hiçbir Jackson/Spring importu eklenmedi. Repository'lerin persisted state'ten aggregate kurabilmesi için `Investigation.reconstitute(...)` ve `IncidentDraft.reconstitute(...)` adında, invariant kontrollerini atlayan (yalnızca ilk persist anında zaten doğrulanmış state için) küçük factory metodları domain'e eklendi. FR-017 audit event listesi için `AuditEventType` enum'u, `AuditEvent` record'u (docs/09 "Audit alanları" ile birebir alan seti) ve `AuditEventRepository` portu + `JdbcAuditEventRepository` implementasyonu yazıldı; append-only kısıtı hem port seviyesinde (update/delete metodu yok) hem de DB seviyesinde (audit_event tablosunda UPDATE/DELETE'i reddeden trigger) uygulandı. Idempotency (AC-014/FR-015/SEC-006), `incident_draft.idempotency_key` üzerindeki UNIQUE constraint ile DB seviyesinde garanti edildi — application kodu bir race'i kendi başına engellemiyor, ikinci INSERT `DataIntegrityViolationException` ile başarısız oluyor. Testcontainers PostgreSQL (pgvector image, M0'daki ile aynı) ile 3 entegrasyon test sınıfı yazıldı: repository CRUD/restart-sonrası-GET, duplicate idempotency key, audit append/immutability.

## Değişen dosyalar
- `src/main/resources/db/migration/V2__domain_schema.sql` — `investigation`, `incident_draft` (idempotency_key UNIQUE), `audit_event` (+ UPDATE/DELETE reddeden trigger) tabloları.
- `src/main/java/.../domain/Investigation.java` — `reconstitute(...)` factory eklendi (repository rehydration, invariant'ları atlar; javadoc'ta neden açıklandı).
- `src/main/java/.../domain/IncidentDraft.java` — aynı amaçla `reconstitute(...)` eklendi.
- `src/main/java/.../domain/AuditEventType.java`, `AuditEvent.java`, `AuditEventRepository.java` — FR-017/DATA-005 audit mekanizması (yeni domain tipleri, Spring/Jackson yok).
- `src/main/java/.../adapters/persistence/JsonColumnMapper.java` — JSONB (de)serileştirme için paylaşılan Jackson yardımcı sınıfı (`FAIL_ON_UNKNOWN_PROPERTIES` kapalı; record'ların türetilmiş getter'ları — örn. `Evidence.isMetric()` — deserialization'da yok sayılıyor).
- `src/main/java/.../adapters/persistence/JdbcInvestigationRepository.java`, `JdbcIncidentDraftRepository.java`, `JdbcAuditEventRepository.java` — portların JdbcTemplate implementasyonları (`ON CONFLICT (id) DO UPDATE` upsert deseni).
- `src/main/java/.../adapters/persistence/package-info.java`.
- `pom.xml` — `postgresql` driver scope `runtime` → varsayılan (compile); `PGobject`'i adapter kodunda kullanabilmek için gerekliydi.
- `src/test/java/.../adapters/persistence/AbstractPostgresIntegrationTest.java` — paylaşılan Testcontainers fixture'ı (singleton-container deseni; bkz. "Spec çelişkisi").
- `src/test/java/.../adapters/persistence/JdbcInvestigationRepositoryTest.java`, `JdbcIncidentDraftRepositoryTest.java`, `JdbcAuditEventRepositoryTest.java` — entegrasyon testleri.

## Testler (gerçek komut + gerçek çıktı)
Komut: `mvn -B verify`

Çıktı özeti:
```
[INFO] Running com.example.otpsentinel.adapters.persistence.JdbcAuditEventRepositoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in ...JdbcAuditEventRepositoryTest
[INFO] Running com.example.otpsentinel.adapters.persistence.JdbcIncidentDraftRepositoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in ...JdbcIncidentDraftRepositoryTest
[INFO] Running com.example.otpsentinel.adapters.persistence.JdbcInvestigationRepositoryTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in ...JdbcInvestigationRepositoryTest
...
[INFO] Running com.example.otpsentinel.OtpSentinelApplicationSmokeTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in ...OtpSentinelApplicationSmokeTest
...
[INFO] Results:
[INFO] Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- spotless:2.43.0:check (spotless-check) @ otp-sentinel ---
[INFO] Spotless.Java is keeping 83 files clean - 0 needs changes to be clean
[INFO] BUILD SUCCESS
[INFO] Total time:  01:00 min
```
Tüm proje: 52/52 test yeşil, `mvn verify` (Flyway + spotless:check dahil) BUILD SUCCESS. M3'ün kendi 3 entegrasyon test sınıfı (8 test) bu 52'nin içinde.

## Karşılanan requirement/AC
- FR-015/AC-014 (idempotency) — `JdbcIncidentDraftRepositoryTest.duplicateIdempotencyKeyProducesExactlyOneRow`: aynı `idempotency_key` ile ikinci `preview()` sonrası `save()` çağrısı `DataIntegrityViolationException` fırlatıyor (DB unique constraint), `findByIdempotencyKey` tek satır/ilk id'yi dönüyor.
- FR-016/AC-030 (re-fetch/canonical GET) — `JdbcInvestigationRepositoryTest.survivesRestartAtEveryLifecyclePhase` ve `JdbcIncidentDraftRepositoryTest.survivesRestartThroughApprovalAndCreation`: her lifecycle adımında `save()` sonrası **yeni bir repository instance'ı** (aynı DB'ye bağlı, "restart" simülasyonu) ile `findById` aynı state'i dönüyor.
- FR-017 (audit) — `JdbcAuditEventRepositoryTest.appendsAndListsEventsForAnInvestigationInOccurredOrder`: listedeki temsili event tipleri (REQUEST_ACCEPTED, TOOL_CALLED, APPROVAL_DECIDED, INCIDENT_CREATED) append edilip `occurred_at` sırasıyla okunuyor.
- DATA-005 (append-only) — `JdbcAuditEventRepositoryTest.auditEventIsAppendOnlyAtTheDatabaseLevel`: `audit_event` tablosuna doğrudan `UPDATE`/`DELETE` DB trigger'ı tarafından reddediliyor; port da zaten update/delete metodu sunmuyor.
- SEC-006 (idempotent + audited write) — idempotency DB constraint'i yukarıdaki testle kanıtlı; audit mekanizması bu milestone'da yalnızca mekanizma+temsili event tipleriyle kuruldu (gerçek tetikleyiciler M4/M5/M7'de).
- NFR-002 (domain framework-free) — `domain` paketine bu oturumda eklenen tek şeyler saf Java (`reconstitute`, `AuditEvent*`); grep ile Spring/Jackson import'u yok doğrulandı.

## Karşılanmayan / ertelenen
- Gerçek audit tetikleyicileri (tool/RAG/LLM/approval akışına audit çağrısı gömme) — M4/M5/M7 kapsamında, bu milestone yalnızca mekanizmayı kurdu.
- REST endpoint / controller entegrasyonu — M7.

## Spec çelişkisi/belirsizlik (varsa)
1. **Spring Data JDBC/JPA yerine plain JdbcTemplate:** Prompt "Spring Data JDBC/JPA" öneriyordu (docs/19 dependency kategorisi). `Investigation`/`IncidentDraft` zengin, invariant-korumalı, mutable-ama-encapsulated aggregate'ler (private constructor, yaşam döngüsü metodları) olduğundan Spring Data JDBC'nin constructor/property mapping modeli ya domain'i kirletir ya da ayrı bir "persistence model" sınıfı (ekstra mapping katmanı, over-engineering) gerektirirdi. Bunun yerine iç koleksiyonlar JSONB'de saklanıp `JdbcTemplate` + el yazımı `RowMapper` ile aggregate `reconstitute(...)` üzerinden kuruluyor; pom'a yeni bağımlılık eklenmedi (zaten mevcut `spring-boot-starter-jdbc`). Bu, domain saflığını (NFR-002) hiçbir ek soyutlama olmadan koruyor; büyük domain değişikliği gerekmedi.
2. **`Investigation.reconstitute`/`IncidentDraft.reconstitute`:** docs/05'te tanımlı olmayan, ama M3 kapsamı "gerekirse port imzalarını küçük ekleme ile tamamla" talimatına uygun küçük factory eklemeleri — repository dışında kullanılmıyor, invariant'ları bilerek atlıyor (state zaten ilk persist anında doğrulanmıştı). Büyük bir domain değişikliği değil.
3. **AuditEvent domain paketinde:** docs/05 aggregate listesinde yok, ama diğer tüm repository portları (docs kuralı: "adaptörler application ports'a bağlanır") domain'de arayüz olarak durduğundan, aynı konvansiyon `AuditEventRepository`/`AuditEvent` için de uygulandı. FK yok (audit_event → investigation): audit "request accepted" gibi olaylar investigation persist edilmeden önce de üretilebileceğinden append-only tablo kasıtlı olarak bağımsız bırakıldı.
4. **Testcontainers "singleton container" deseni:** `@Container`+`@Testcontainers` ile 3 test sınıfının hepsi aynı statik alanı miras aldığından JUnit5 extension container'ı ilk sınıftan sonra durdurup sonraki sınıfları `Connection refused` ile patlatıyordu (ilk denemede canlı yakalandı). Çözüm: `@Container` kaldırıldı, container `static { postgres.start(); }` ile bir kere başlatılıp Ryuk'a bırakıldı — standart Testcontainers "manual singleton container" idiomu, dokümante edildi (Javadoc).

## Sonraki oturum için not
M4 (RAG) başlatılabilir. Audit mekanizması hazır; M4/M5/M7'de gerçek olay tetikleyicileri (`AuditEventRepository.append(...)` çağrıları) eklenmeli.
