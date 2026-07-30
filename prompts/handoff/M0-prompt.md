## Görev: M0 — Bootstrap

Kaynak: `docs/14-implementation-plan.md` → **M0 — Bootstrap**.

İlgili spec dosyaları: `AGENTS.md`, `docs/05-domain-and-architecture.md` (paket yapısı), `docs/19-technology-baseline.md` (sürüm/profil/config), `docs/16-adr.md` (ADR-001, ADR-003, ADR-004, ADR-005, ADR-014).

### Kapsam

Yalnızca şunu yap: çalışan bir Java 21 + Spring Boot Maven iskeleti kur — `docs/05-domain-and-architecture.md`'deki paket yapısına (`api, application, domain, agent, tools, rag, adapters, observability, config`) uygun boş modüller, Dockerfile + docker-compose (app + PostgreSQL/pgvector), Flyway entegrasyonu (henüz migration şart değil, mekanizma çalışsın), Spring Actuator health endpoint, CI pipeline (build+test), kod formatlama aracı.

Henüz **yazma**: domain aggregate'leri, tool implementasyonları, agent/LLM entegrasyonu, RAG, REST endpoint'leri. Bunlar sonraki milestone'lar (M1+).

Bu milestone'un "Kabul" kriteri: `mvn verify` temiz geçer; `docker compose up --build` sonrası app ve DB container'ları ayakta ve Actuator health `UP` döner.

### Sırayla

1. İlgili requirement/acceptance criterion'ı belirle (AC-025: temiz ortamda `docker compose up --build` sonrası app ve DB healthy; NFR-003 Docker Compose portability; NFR-002 domain/Spring ayrımı).
2. Değişecek dosyaları listele (pom.xml, Dockerfile, docker-compose.yml, `.env.example`, CI config, boş paket iskeletleri, Flyway config).
3. Önce failing test yaz: en azından Actuator health endpoint'inin `UP` döndüğünü doğrulayan bir smoke/integration test (Testcontainers PostgreSQL ile).
4. Minimum implementasyonla geçir: yalnızca boot etmeye yetecek Spring config, boş paketler, Flyway baseline migration (boş/no-op olabilir), pgvector extension kurulumu compose'da.
5. Refactor: formatlama aracını uygula, gereksiz starter/bağımlılık ekleme (`docs/19-technology-baseline.md`'deki dependency kategorilerine sadık kal, beta starter zorunlu değil).
6. `docs/19-technology-baseline.md`'deki "Compatibility spike" listesini (boot, tool call, structured output, pgvector insert/search, Testcontainers, Docker health) bu aşamada tam karşılamak zorunda değilsin — sadece Spring boot + Testcontainers + Docker health kısmı bu milestone'da; tool call/structured output/pgvector search sonraki milestone'lara ait, ilgisiz olarak ekleme.

### Kısıtlar

- Bu milestone dışına taşma; domain/tool/RAG/agent kodu yazma.
- `AGENTS.md`'deki mimari sınırları (api→application→domain, adapters→ports) koru; boş paketler bile olsa bu sınırı yansıt.
- Kafka, Redis, Kubernetes, Python servisi, çoklu agent veya başka AI framework ekleme (onaylanmamış).
- `docs/19-technology-baseline.md`'deki temel config değerlerini (`AI_MODE`, `RAG_TOP_K` vb.) `.env.example`'a koy ama gerçek secret koyma.

### Bitti sayılması için

- `mvn verify` çalıştırıldı ve geçti (gerçek çıktı raporlanacak, iddia değil).
- `docker compose up --build` çalıştırıldı, health `UP`.
- Failure path testli değilse (bu milestone'da kritik iş kuralı yok) neden gerekmediği raporda belirtilsin.
- `docs/17-traceability-risk-dod.md` DoD listesine (derlenir/standart, spec güncel, secret yok) uyuyor.

### Oturum sonu

`prompts/08-session-report.md` kuralına göre `prompts/handoff/M0-report.md` dosyasını yaz ve `SESSION_LOG.md`'ye satır ekle.
