## Görev: M11 — Agent console backend (session memory, model seçimi, hızlı/detaylı mod, belge yükleme)

Önce: `git checkout main && git pull && git checkout -b milestone/M11-agent-console-backend` (bkz. `docs/20-git-workflow.md`).

**Süreç:** M5-M10'daki gibi çalış — `superpowers:writing-plans` ile küçük sıralı task'lara böl, `superpowers:subagent-driven-development` ile her task'ı taze implementer + ayrı reviewer subagent'a yaptır, final whole-branch review yap.

### Bağlam

Bu proje temelde bir LLM agent + RAG projesi ve arayüz (M12'de yapılacak) bunu bir ChatGPT/Claude tarzı sohbet uygulaması olarak sunacak: sol sidebar'da geçmiş sohbetler (thread), ortada mesajlaşma, ayarlarda model seçimi/hızlı-detaylı mod/belge yükleme. M12'nin buna bağlanabilmesi için önce backend'in bunu desteklemesi gerekiyor — bu milestone yalnızca backend.

### Önce oku

- `docs/16-adr.md` ADR-012 ("kalıcı chat memory yok") ve ADR-015 (NVIDIA NIM sağlayıcısı).
- `src/main/java/com/example/otpsentinel/config/AgentConfig.java` — `chatModelFactory` bean'i (şu an `Supplier<ChatModel>`, `live` modeli başlangıçta bir kere inşa ediyor).
- `src/main/java/com/example/otpsentinel/rag/KnowledgeIngestionService.java` — genel amaçlı, framework-free, zaten reusable (`ingest(documentId, version, title, documentTypeRaw, provider, effectiveFrom, effectiveTo, language, tags, rawContent)`), şu an yalnızca `AgentConfig` içinde ad-hoc inşa ediliyor, kendi `@Bean`'i yok.
- `src/main/java/com/example/otpsentinel/agent/IncidentAnalysisAiService.java`, `agent/ToolBudgetGuard.java`.
- `docs/19-technology-baseline.md` satır ~135-137 — model seçim geçmişi (`meta/llama-3.1-8b-instruct` doğrulanmış/pinlenmiş, `meta/llama-3.3-70b-instruct` M5'te `503 ResourceExhausted` ile başarısız olmuştu — kapasite sorunu, tool-calling sorunu değil).

### Kapsam

1. **ADR-017 yaz** (`docs/16-adr.md`'ye ekle, ADR-012'yi silme/değiştirme — üzerine yeni bir madde): session-scoped `ChatMemory` kabul edilir. Gerekçe: aynı sohbet thread'i içinde takip sorularına ("peki ya X?") izin vermek gerekiyor; cross-session memory yasak kalarak ADR-012'nin asıl endişesi (context sızıntısı/flaky test) sınırlı tutuluyor — memory yalnızca bir `sessionId` ile scope'lanıyor, session'lar arası hiç paylaşılmıyor.

2. **Session/thread kavramı**: `Investigation`'a (`domain/Investigation.java`) opsiyonel `sessionId` alanı ekle (client-üretimli UUID, invariant gerektirmez — nullable). M3 Flyway zincirine `V4__session_id.sql` (`investigation.session_id` kolonu + index). `InvestigationRepository`'ye `findBySessionId(sessionId)` (kronolojik sıralı) ekle. Yeni `GET /api/v1/sessions/{sessionId}/investigations` endpoint'i — bir thread'in tüm geçmiş investigation'larını döner (sidebar'ın veri kaynağı olacak).

3. **Session-scoped chat memory**: `IncidentAnalysisAiService`'e LangChain4j `AiServices.builder()...chatMemoryProvider(sessionId -> MessageWindowChatMemory.withMaxMessages(N))` ekle (N makul bir pencere, ör. 10 — token/maliyet kontrolü, gerekçelendir). In-memory `Map<String, ChatMemory>` (uygulama restart'ında kaybolması kabul edilebilir — investigation sonuçları zaten M3'te ayrıca persist ediliyor, memory yalnızca konuşma bağlamı için). **Kritik:** her tur kendi kanıtını (evidence) taze toplar — M6'nın validation pipeline'ı, M5'in tool-budget/evidence-id kuralları değişmeden çalışır; chat memory yalnızca modelin "önceki ne konuşulduğunu hatırlaması" için, geçmiş evidence'ların tekrar kanıt olarak kullanılmasına izin vermez.

4. **Model seçimi**: `chatModelFactory` bean'ini `Supplier<ChatModel>`'den `Function<String, ChatModel>`'e (model id → `ChatModel`) dönüştür; `ConcurrentHashMap` ile lazy-cache'le (her model id ilk istekte `OpenAiChatModel.builder()...modelName(id).build()` ile inşa edilir, sonra cache'den döner — restart gerekmez). `POST /api/v1/investigations` request body'sine opsiyonel `modelId` alanı ekle (boşsa `NVIDIA_CHAT_MODEL` varsayılanı). Küçük bir compatibility spike (`@Tag("local-live")`, M5/M9 emsali) ile en az 1 ek modeli doğrula (`meta/llama-3.3-70b-instruct`'ı tekrar dene — kapasite sorunu geçici olabilir — veya NVIDIA kataloğundan başka bir tool-calling destekli Instruct model). Yalnızca gerçekten doğrulanan modeller yeni `GET /api/v1/models` endpoint'inde (statik, küçük, hardcoded liste) görünür.

5. **Hızlı/Detaylı mod**: `POST /api/v1/investigations` request body'sine `mode: "quick" | "thorough"` ekle (varsayılan `thorough`, mevcut davranış). `quick` modda deterministik olarak `ToolBudgetGuard`'a düşük bir tool bütçesi (ör. max 3) geçirilir ve/veya `searchIncidentKnowledge` çağrısı atlanır — bunu uygulama katmanında zorla, yalnızca sistem promptuna "kısa tut" ekleyerek LLM'in inisiyatifine bırakma.

6. **Belge yükleme endpoint'i**: `KnowledgeIngestionService`'i kendi `@Bean`'i olarak `AgentConfig`'e ekle (mevcut `ContentSanitizer`/`Chunker`/embedding servisini reuse et). Yeni `POST /api/v1/knowledge/documents` (JSON body: `title, documentType, provider, tags, effectiveFrom, effectiveTo, language, content` — metin/markdown; PDF parsing kapsam dışı). Yeni `GET /api/v1/knowledge/documents` (mevcut belgeleri listeler — id/version/title/documentType/effectiveFrom, ayarlar sayfası için).

### Kısıtlar

- `domain` paketi framework-free kalır (chat memory/model seçimi `agent`/`config`/`api` katmanında kalır, `domain`'e Spring/LangChain4j import etme).
- Ana test suite `NVIDIA_API_KEY` olmadan yeşil kalmalı — model spike ve gerçek NVIDIA çağrısı gerektiren her şey `@Tag("local-live")` ile ayrılır.
- Mevcut idempotency (M3), audit (M3/M6), validation (M6) mekanizmaları bozulmaz — yalnızca ek alanlar/endpoint'ler.
- Bu milestone dışına taşma: frontend kodu yazma (M12'nin işi).
- Tüm `mvn`/`docker` komutları repository kökünden. Commit'ten önce `mvn spotless:apply` + tam `mvn verify` yeşil olduğunu doğrula.

### Bitti sayılması için

- Session-scoped memory testi: aynı `sessionId` ile 2. çağrı 1.'yi hatırladığını (ör. takip sorusuna doğru bağlamla cevap verdiğini), farklı `sessionId`'nin hatırlamadığını kanıtlayan test.
- Model spike (`@Tag("local-live")`, gerçek key ile) en az 1 ek modeli doğrular; `GET /models` yalnızca doğrulananları döner.
- Quick-mode'un gerçekten daha az tool çağrısı yaptığı (veya RAG atladığı) testle kanıtlanmış.
- Belge yükleme sonrası `searchIncidentKnowledge`'ın yeni belgeyi bulduğunu gösteren entegrasyon testi.
- `mvn verify` (tüm proje, offline) BUILD SUCCESS.
- ADR-017 yazılmış, `docs/16-adr.md`'de ADR-012 ile çelişki açıkça not düşülmüş (silinmemiş, üzerine yazılmış).

### Oturum sonu

`prompts/08-session-report.md` kuralına göre `prompts/handoff/M11-report.md` yaz, `SESSION_LOG.md`'ye satır ekle. Branch `milestone/M11-agent-console-backend`. **Kendi işini VERIFIED yazma** — DONE, bağımsız oturum doğrulayacak. Bitince M12 (frontend sohbet arayüzü) promptu verilecek.
