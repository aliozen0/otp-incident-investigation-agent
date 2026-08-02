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

## ADR-016 — Portfolyo demosu için birinci sınıf web UI

- **Status:** Accepted
- **Decision:** `docs/02-prd.md`'nin "UI kapsamı" bölümü ("Swagger yeterlidir, basit UI yalnızca zaman kalırsa") revize edildi: proje artık gerçek, canlı (stub değil) uçtan uca bir demoyu gösterecek bir web arayüzü içerir (M9/M10). Frontend, backend'e ayrı bir servis/container olarak değil, statik build çıktısı Spring Boot'un `static/` kaynaklarına (multi-stage Dockerfile ile) gömülerek sunulur — `docker compose up --build` tek komut kuralı (ADR-004, NFR-003) bozulmaz, yeni container/altyapı eklenmez.
- **Reason:** Kullanıcı bu projeyi iş görüşmelerinde/portföyde göstermek istiyor; yalnızca Swagger/curl ile "gerçekten çalışıyor" hissi verilemiyor. Amaç, sistemin gerçek NVIDIA NIM canlı model + gerçek pgvector RAG + gerçek agentic tool-calling ile uçtan uca çalıştığını (stub değil) görsel olarak kanıtlamak.
- **Tech stack:** React + TypeScript + Vite + Tailwind CSS (SPA, backend'in REST API'sini doğrudan tüketir). Açık/beyaz tema; koyu tema yok. "Yapay zeka ile üretilmiş şablon" görünümünden kaçınılır (bkz. `frontend-design` skill rehberliği) — kasıtlı tipografi/renk/layout kararları olan, sıradan olmayan bir tasarım hedeflenir.
- **Consequence:** M9, canlı modun (gerçek NVIDIA chat + gerçek pgvector RAG + gerçek tool-calling) uçtan uca çalıştığını kanıtlar (önceden yalnızca izole spike'larla doğrulanmıştı). M10 bu kanıtlanmış canlı API'nin üzerine UI'ı kurar. Mock veri (`OTP-DROP-001` fixture) küçük ve gerçekçi kalır; UI bunu "gerçek bir sistem" gibi sunar ama README/ADR'de mock olduğu açık kalır (docs/18 "Gösterilmemesi gerekenler" ihlal edilmez).

## ADR-017 — Session-scoped chat memory (M11)

- **Status:** Accepted
- **Decision:** LangChain4j `MessageWindowChatMemory` scoped by a client-supplied `sessionId`
  (`@MemoryId`) is now permitted within a single chat thread, so a follow-up question ("peki ya
  X?") can refer to earlier turns in the same thread. Memory is held in-process (an LRU-bounded
  synchronized `LinkedHashMap<String, ChatMemory>`), capped to the last 40 messages per session
  (`otp-sentinel.ai.chat-memory-max-messages`) and to 1000 concurrent sessions
  (`otp-sentinel.ai.chat-memory-max-sessions`, least-recently-used evicted first), and is never
  persisted to a database or shared across sessions. The 40-message window is sized from one
  thorough turn's actual traffic — 1 system + 1 user + 6 AI tool-call messages + 6 tool results +
  1 final answer ≈ 15 messages — so ~2.5 turns fit and a turn can never evict its own opening
  question. The session cap is needed because an anonymous investigation (no client `sessionId`)
  uses its own investigation id as memory id and is never revisited.
- **Reason:** M12's chat console needs multi-turn conversations inside one thread. ADR-012's actual
  concern — context leaking across unrelated investigations and flaky tests from shared mutable
  state — is preserved: memory is strictly scoped to one `sessionId`, never shared cross-session,
  and lost on restart (no persistence, no leakage between demo runs or test runs).
- **Consequence:** ADR-012's "no persistent chat memory" still holds *across* sessions and *across*
  restarts; it no longer holds *within* one session's lifetime. Every investigation turn still
  collects its own fresh evidence via the M5/M6 tool-budget and validation pipeline — chat memory
  only carries the model's own prior turns for conversational continuity, never past evidence ids
  as if they were newly collected (docs/16 ADR-008 evidence-id provenance is unaffected).
