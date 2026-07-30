# 16 — Architecture Decision Records

## ADR-001 — Java 21 + Spring Boot

- **Status:** Accepted
- **Decision:** Java 21 ve Spring Boot.
- **Reason:** Kurumsal Java uyumu, REST/data/testing desteği.

## ADR-002 — LangChain4j

- **Status:** Accepted
- **Decision:** Tool calling, RAG ve structured output için LangChain4j.
- **Consequence:** Framework agent adapter sınırında tutulur.

## ADR-003 — Core modülleri manuel config

- **Status:** Accepted
- **Decision:** Beta starter zorunlu değil; core/provider bean'leri manuel yapılandırılabilir.
- **Reason:** Sürüm uyumu ve kontrol.

## ADR-004 — Modüler monolith

- **Status:** Accepted
- **Reason:** Demo güvenilirliği ve tek komut çalışma.
- **Consequence:** Modül sınırları korunur; sonra ayrıştırılabilir.

## ADR-005 — PostgreSQL + pgvector

- **Status:** Accepted
- **Reason:** Transactional data ve vector search tek bileşende.
- **Consequence:** Büyük ölçekte ayrı store yeniden değerlendirilir.

## ADR-006 — Hybrid agentic workflow

- **Status:** Accepted
- **Decision:** Tool seçimi/hipotez agentic; policy/write deterministik.

## ADR-007 — Structured output zorunlu

- **Status:** Accepted
- **Reason:** API, test ve validation.
- **Consequence:** Repair/failure akışı gerekir.

## ADR-008 — Evidence ID uygulama üretir

- **Status:** Accepted
- **Reason:** Modelin kaynak uydurmasını engellemek.

## ADR-009 — Otomatik remediation yok

- **Status:** Accepted
- **Reason:** Güvenlik ve gerçek sistem erişimi olmaması.

## ADR-010 — Incident ayrı approval akışı

- **Status:** Accepted
- **Reason:** Human-in-the-loop ve yetki.

## ADR-011 — Deterministic stub

- **Status:** Accepted
- **Reason:** CI, offline demo ve tekrar üretilebilirlik.

## ADR-012 — Kalıcı chat memory yok

- **Status:** Accepted
- **Reason:** Context sızıntısı ve flaky test riskini azaltmak.

## ADR-013 — Synchronous API

- **Status:** Accepted for MVP
- **Decision:** Request maksimum 30 saniye bekler.
- **Future:** Production'da async job değerlendirilebilir.

## ADR-014 — Mock adapters first

- **Status:** Accepted
- **Reason:** İç sistem erişimi yok, fixture deterministik.

## ADR-015 — NVIDIA NIM canlı model/embedding sağlayıcısı

- **Status:** Accepted
- **Decision:** `local-live`/`demo` profillerinde LLM ve embedding sağlayıcısı olarak NVIDIA NIM (`https://integrate.api.nvidia.com/v1`, OpenAI-compatible API) kullanılır. LangChain4j entegrasyonu `langchain4j-open-ai` modülünün `OpenAiChatModel`/`OpenAiEmbeddingModel` builder'ı ile `baseUrl` NVIDIA endpoint'ine, `apiKey` `NVIDIA_API_KEY` env değişkenine set edilerek yapılır — ayrı bir NVIDIA-özel LangChain4j modülü eklenmez.
- **Reason:** Kullanıcının mevcut NVIDIA API key'i var; NIM OpenAI-uyumlu şema sunduğu için LangChain4j'nin OpenAI adaptörü değişiklik gerektirmeden çalışır (ADR-002'deki provider soyutlamasına uygun).
- **Consequence:** Chat ve embedding model ID'leri (`NVIDIA_CHAT_MODEL`, `NVIDIA_EMBEDDING_MODEL`) M4 (RAG/embedding) ve M5 (agent) oturumlarında NVIDIA build katalogundan seçilip pinlenecek; küçük bir compatibility spike (`docs/19-technology-baseline.md`) bu seçimden önce çalıştırılmalı. `test`/`local-stub` profillerinde bu sağlayıcıya ihtiyaç yoktur (deterministic stub kullanılır).
- **Not:** `NVIDIA_API_KEY` asla repoya/loglara girmez (`docs/09-security-governance.md`).
