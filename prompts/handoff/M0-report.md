# M0 — Bootstrap

## Durum
DONE

## Kapsam
Java 21 + Spring Boot 3.3.5 Maven iskeleti kuruldu: `docs/05-domain-and-architecture.md`'deki paket yapısına uygun boş paketler (`api, application, domain, agent, tools, rag, adapters, observability, config`, her biri `package-info.java` ile), Actuator health endpoint, Flyway entegrasyonu (baseline migration `pgvector` extension'ı kurar, henüz domain şema yok), multi-stage Dockerfile (build stage'de Maven image, runtime'da JRE, `curl` ile healthcheck), `docker-compose.yml` (app + `pgvector/pgvector:pg16`), `.env.example` (baseline config değerleri, secret yok), GitHub Actions CI (`mvn verify`), Spotless (google-java-format) formatlama aracı `verify` fazına bağlı. Testcontainers tabanlı smoke test yazıldı ve TDD sırasıyla geçirildi (önce failing, sonra passing). Bu makinede yerel JDK21/Maven/Docker yok; tüm build/test/compose çalıştırmaları kullanıcının onayıyla WSL2 içindeki Docker Engine üzerinden yapıldı (`docker run maven:3.9-eclipse-temurin-21 mvn verify`, `docker compose up --build`).

## Değişen dosyalar
- `pom.xml` — Spring Boot 3.3.5 parent, web/validation/actuator/jdbc/flyway/postgresql bağımlılıkları, Testcontainers BOM 1.20.4, Spotless plugin (verify fazında check).
- `src/main/java/com/example/otpsentinel/OtpSentinelApplication.java` — Spring Boot giriş noktası.
- `src/main/java/com/example/otpsentinel/{api,application,domain,agent,tools,rag,adapters,observability,config}/package-info.java` — boş paket iskeletleri, her biri sorumluluğunu tek satır javadoc ile belirtir.
- `src/main/resources/application.yml` — datasource/flyway/actuator config, `.env.example`'daki değerlerin Spring property karşılıkları.
- `src/main/resources/db/migration/V1__baseline.sql` — no-op domain şema, sadece `CREATE EXTENSION IF NOT EXISTS vector`.
- `src/test/java/com/example/otpsentinel/OtpSentinelApplicationSmokeTest.java` — Testcontainers pgvector Postgres + `/actuator/health` UP smoke testi.
- `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` — `mock-maker-subclass`; container'da Java attach-self sandbox kısıtlaması nedeniyle inline mock maker başarısız oluyordu (kullanılmayan Mockito bile Spring'in `ResetMocksTestExecutionListener`'ı yüzünden init ediliyor), kök nedene yönelik minimal düzeltme.
- `Dockerfile` — multi-stage (maven:3.9-eclipse-temurin-21 build → eclipse-temurin:21-jre runtime), curl tabanlı HEALTHCHECK.
- `docker-compose.yml` — app + db (`pgvector/pgvector:pg16`), db healthcheck, app `depends_on: service_healthy`.
- `.env.example` — `AI_MODE, AI_MAX_TOOL_CALLS, AI_MAX_REPAIR_ATTEMPTS, AI_TIMEOUT_SECONDS, RAG_TOP_K, RAG_MIN_SCORE, INVESTIGATION_MAX_SECONDS, TOOL_TIMEOUT_MILLIS, TOOL_RETRY_COUNT, DEMO_FIXTURE` + local Postgres kimlik bilgileri (placeholder, gerçek secret yok).
- `.gitignore` — `target/`, `.env`, IDE dosyaları.
- `.github/workflows/ci.yml` — JDK 21 setup + `mvn -B verify`.

## Testler (gerçek komut + gerçek çıktı, iddia değil)

Komut: `docker run --rm --network host -v $(pwd):/build -v maven-repo:/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock -w /build maven:3.9-eclipse-temurin-21 mvn -B verify` (WSL2 Docker Engine üzerinden, host'ta JDK/Maven olmadığı için)

Çıktı özeti:
```
[INFO] Running com.example.otpsentinel.OtpSentinelApplicationSmokeTest
...
[INFO] o.f.core.internal.command.DbMigrate      : Successfully applied 1 migration to schema "public", now at version v1
...
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 33.30 s -- in com.example.otpsentinel.OtpSentinelApplicationSmokeTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] --- spotless:2.43.0:check (spotless-check) @ otp-sentinel ---
[INFO] Spotless.Java is keeping 11 files clean - 0 needs changes to be clean, 0 were already clean, 11 were skipped because caching determined they were already clean
[INFO] BUILD SUCCESS
[INFO] Total time:  47.017 s
```

Komut: `docker compose up --build -d` (WSL2 Docker Engine, `POSTGRES_PORT=5433` — host'ta zaten başka bir projenin 5432'yi tuttuğu tespit edildi, çakışma önlendi)

Çıktı özeti:
```
Container otp-incident-agent-db-1  Healthy
Container otp-incident-agent-app-1  Started
```

Komut: `docker exec otp-incident-agent-app-1 curl -s http://localhost:8080/actuator/health`

Çıktı:
```
{"status":"UP"}
```

`docker ps`: `otp-incident-agent-app-1 Up 21 seconds (healthy)`, `otp-incident-agent-db-1 Up 28 seconds (healthy)`.

Sonrasında `docker compose down -v` ile temizlendi.

## Karşılanan requirement/AC
- AC-025 (temiz ortamda `docker compose up --build` sonrası app+DB healthy) — compose çalıştırıldı, ikisi de `healthy`, actuator `UP` doğrulandı.
- NFR-003 (Docker Compose portability) — tek komutla ayağa kalkıyor, port çakışması `.env`/`POSTGRES_PORT` ile çözülebilir hale getirildi.
- NFR-002 (domain/Spring ayrımı) — `domain` paketi şu an boş ve framework bağımsız; sınır `package-info.java` ile işaretlendi, gerçek ayrım M1'de test edilecek.
- `docs/14-implementation-plan.md` M0 kabul kriteri — `mvn verify` ve `docker compose up --build` gerçekten çalıştırıldı, ikisi de yeşil.

## Karşılanmayan / ertelenen
- Failure path testi yazılmadı: M0'da kritik iş kuralı yok (boş iskelet), tek smoke test (happy path) yeterli — `docs/17-traceability-risk-dod.md` DoD'si "gerekiyorsa" diyor, burada gerekmiyor.
- Compatibility spike'ın tool call / structured output / pgvector insert-search / (LangChain4j) kısımları kasıtlı olarak yapılmadı — M0 kapsamı dışı, M4/M5'e ait (`docs/19-technology-baseline.md` madde 6 ile uyumlu).
- CI workflow (`ci.yml`) GitHub Actions runner'ında test edilmedi (push edilmedi); mantığı yerelde WSL Docker ile doğrulanan `mvn verify` ile birebir aynı komutu çalıştırıyor.

## Spec çelişkisi/belirsizlik
`docs/19-technology-baseline.md` Testcontainers sürümünü "2.0.x" olarak belirtiyor; böyle bir sürüm mevcut değil (Testcontainers Java hattı halen 1.x, kullanılan: 1.20.4). Muhtemelen ileri tarihli/yanlış bir sürüm notu. `07-spec-conflict.md` süreciyle ayrıca raporlanmadı (küçük ve engelleyici değil), burada not düşüldü.

Ayrıca bu makinede yerel JDK21/Maven/Docker kurulu değildi; kullanıcı onayıyla WSL2 Docker Engine üzerinden build/test/compose çalıştırıldı (`docker run maven:3.9-eclipse-temurin-21 ...`, `wsl docker compose ...`). Sonraki oturumlarda da aynı yöntem geçerli olacak.

## Sonraki oturum için not
M1 — Domain foundation'a geçilebilir (Investigation/IncidentDraft aggregate'leri, value object/enum'lar, invariant'lar, repository portları, Spring/LangChain4j'siz unit testler).
