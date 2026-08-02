# 19 — Technology Baseline and References

## Doğrulama tarihi

`2026-07-30`

Sürümler implementation başlangıcında yeniden doğrulanmalı ve Maven/Compose içinde exact pin yapılmalıdır.

## Baseline

| Teknoloji | Karar |
|---|---|
| Java | 21 |
| Spring Boot | Güncel stabil, LangChain4j ile smoke test edilmiş sürüm |
| LangChain4j | Stabil 1.18.x hattı değerlendirilir |
| Spring starter | Beta suffix varsa zorunlu değil; manuel bean config |
| PostgreSQL | pgvector destekli stabil sürüm |
| Testcontainers | 2.0.x |
| JUnit | Jupiter |
| Build | Maven |
| Container | Docker Compose |

## Sürüm politikası

1. Java 21 sabit.
2. Spring Boot + LangChain4j küçük compatibility spike ile doğrulanır.
3. LangChain4j modülleri aynı BOM/sürüm hattında tutulur.
4. Beta starter yerine core/provider manuel config seçilebilir.
5. Testcontainers BOM kullanılır.
6. Docker tag ve mümkünse digest pinlenir.
7. Dependency update test kapılarından geçer.

## Resmî kaynaklar

### LangChain4j

- `https://docs.langchain4j.dev/`
- `https://docs.langchain4j.dev/tutorials/tools/`
- `https://docs.langchain4j.dev/tutorials/rag/`
- `https://docs.langchain4j.dev/tutorials/structured-outputs/`
- `https://docs.langchain4j.dev/tutorials/observability/`
- `https://github.com/langchain4j/langchain4j/releases`

### Spring Boot

- `https://docs.spring.io/spring-boot/system-requirements.html`

### Testcontainers

- `https://java.testcontainers.org/`
- `https://java.testcontainers.org/test_framework_integration/junit_5/`
- `https://java.testcontainers.org/modules/databases/postgres/`
- `https://github.com/testcontainers/testcontainers-java/releases`

### pgvector

- `https://github.com/pgvector/pgvector`
- `https://github.com/pgvector/pgvector-java`

## Dependency kategorileri

- spring-boot-starter-web
- spring-boot-starter-validation
- spring-boot-starter-actuator
- JDBC veya JPA
- flyway-core
- PostgreSQL driver
- LangChain4j core + model provider
- embedding integration
- pgvector integration
- Micrometer
- Testcontainers JUnit/PostgreSQL
- Cucumber JVM (ATDD otomatikleştirilecekse)

## Compatibility spike

Ana koddan önce şu küçük spike çalışmalıdır:

- Spring app boot
- Bir typed tool call
- Bir structured output
- Bir pgvector insert/search
- Bir Testcontainers PostgreSQL testi
- Docker health

Spike başarısızsa sürüm kombinasyonu ana geliştirme başlamadan değiştirilir.

## Yerel çalıştırma ortamı

Bu makinede Windows tarafında yerel JDK21/Maven/Docker kurulu değil. Tüm `mvn`, `docker`, `docker compose` komutları **WSL2 içindeki Docker Engine** üzerinden çalıştırılır, Windows Docker Desktop üzerinden değil.

```bash
wsl -e bash -lc "cd /mnt/c/Users/Ali/Downloads/otp-incident-agent && docker run --rm -v \$(pwd):/build -v maven-repo:/root/.m2 -w /build maven:3.9-eclipse-temurin-21 mvn -B verify"
wsl -e bash -lc "cd /mnt/c/Users/Ali/Downloads/otp-incident-agent && docker compose up --build -d"
```

Kurallar:

- Maven build/test: `maven:3.9-eclipse-temurin-21` image'ı ile `docker run` içinden.
- Compose/health doğrulama: `wsl -e bash -lc "cd /mnt/c/... && docker compose ..."`.
- Windows tarafında doğrudan `mvn`/`docker` komutu çalıştırmaya çalışma — kurulu değil, başarısız olur.
- Port çakışması olursa (`POSTGRES_PORT` gibi) `.env`/ortam değişkeniyle çöz, sabit port'a güvenme.

## Profil önerisi

| Profil | Model | Tools | DB |
|---|---|---|---|
| test | stub | mock | Testcontainers |
| local-stub | stub | mock | Docker Postgres |
| local-live | live | mock | Docker Postgres |
| demo | stub fallback/live | mock | Docker Postgres |
| prod-future | live | real adapters | managed Postgres |

## Temel config

```text
AI_MODE=stub
AI_MAX_TOOL_CALLS=8
AI_MAX_REPAIR_ATTEMPTS=1
AI_TIMEOUT_SECONDS=20
RAG_TOP_K=5
RAG_MIN_SCORE=0.70
INVESTIGATION_MAX_SECONDS=30
TOOL_TIMEOUT_MILLIS=2000
TOOL_RETRY_COUNT=1
DEMO_FIXTURE=OTP-DROP-001

# local-live / demo profilinde (bkz. ADR-015)
NVIDIA_API_KEY=
NVIDIA_BASE_URL=https://integrate.api.nvidia.com/v1
NVIDIA_CHAT_MODEL=meta/llama-3.1-8b-instruct
NVIDIA_EMBEDDING_MODEL=nvidia/nv-embedqa-e5-v5
```

`NVIDIA_CHAT_MODEL` M5 spike'ıyla doğrulandı: `meta/llama-3.1-8b-instruct`, tool/function calling destekli (NvidiaNimChatServiceLiveTest ile gerçek endpoint'e karşı bir tool-call round trip doğrulandı). NVIDIA build katalogundaki Llama 3.x Instruct ailesinden seçildi (ADR-015); `meta/llama-3.3-70b-instruct` ilk denemede endpoint kapasite sınırına (`503 ResourceExhausted`) takıldı, `meta/llama-3.1-8b-instruct` tool-call'ı güvenilir şekilde tamamladı.

M11 re-verified a second chat model for the console's model picker: `meta/llama-3.3-70b-instruct`, confirmed via
`NvidiaNimAlternateModelLiveTest` with a real tool-call round trip against the NVIDIA NIM endpoint.
Both models are listed in `ModelCatalog`; `GET /api/v1/models` only ever returns ids that have a
passing `@Tag("local-live")` spike backing them — no unverified model id is exposed.

`NVIDIA_EMBEDDING_MODEL` M4'te compatibility spike ile doğrulandı: `nvidia/nv-embedqa-e5-v5`, dimension 1024, OpenAI-uyumlu `/v1/embeddings` şemasına ek `input_type` (`query`/`passage`) parametresi kabul ediyor. LangChain4j 1.18+'in `OpenAiEmbeddingRequestParameters.CUSTOM_PARAMETERS` passthrough'u bu alanı ayrı bir HTTP interceptor yazmadan taşıyor (bkz. `NvidiaNimEmbeddingService`).
