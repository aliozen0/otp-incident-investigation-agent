# M11 — Agent console backend (session memory, model seçimi, hızlı/detaylı mod, belge yükleme)

## Durum
DONE

## Kapsam
M12'nin (frontend sohbet arayüzü) dayanacağı backend desteği eklendi: `Investigation`'a opsiyonel `sessionId` (chat-thread) alanı ve `GET /api/v1/sessions/{sessionId}/investigations`; LangChain4j `@MemoryId`/`MessageWindowChatMemory` ile session-scoped chat memory (ADR-017, ADR-012'yi silmeden üzerine yeni bir madde); `chatModelFactory`'nin `Supplier<ChatModel>`'den `Function<String, ChatModel>`'e dönüşümü + `GET /api/v1/models` (gerçek NVIDIA spike ile `meta/llama-3.3-70b-instruct` ikinci model olarak doğrulandı); deterministik hızlı/detaylı mod (`mode: quick|thorough`, tool bütçesi mod'a göre); markdown/text bilgi belgesi yükleme (`POST`/`GET /api/v1/knowledge/documents`), NVIDIA key'siz çalışması için `HashEmbeddingService` (test double'dan production'a taşındı). Süreç: `superpowers:writing-plans` ile 7 küçük task'a bölündü, `superpowers:subagent-driven-development` ile her task taze implementer + ayrı reviewer subagent'a yaptırıldı (6/6 task review "Approved", Critical/Important sıfır — yalnızca birkaç Minor ertelendi). Final whole-branch review (opus) 6 Important bulgu buldu: session-memory testi gerçek `@MemoryId` seviyesinde değil sadece map seviyesindeydi; `chat-memory-max-messages:10` bir thorough turn'ün kendi mesajlarını bile tutamayacak kadar küçüktü; quick mode tool bütçesini aşınca `PARTIAL_ANALYSIS`'e düşüp boş sonuç dönüyordu (hızlı-ama-bozuk, hızlı-ama-tam değil); yüklenen belgeler stub/demo modda agent'ın `searchIncidentKnowledge` aracına hiç görünmüyordu (`FixtureKnowledgeSearchPort` hep sabit `INC-2026-041` dönüyordu); hash-embedding ile NVIDIA-embedding aynı tabloda karışabilirdi; `SessionChatMemoryStore` sınırsız büyüyordu. Kullanıcı talimatıyla ("hepsini tam çalışır hale getir, hiçbir şeyi yok sayma") tek bir fix dalgasında hepsi kök nedenden düzeltildi (quick mode artık `searchIncidentKnowledge`'ı guard'a hiç sokmadan atlıyor ve diğer 5 aracı tam çalıştırıyor; stub modda `CompositeKnowledgeSearchPort` fixture + gerçek pgvector/hash sonucunu birleştiriyor; `embedding_model` filtreli sorgu; `SessionChatMemoryStore` LRU sınırlı; gerçek `@MemoryId` round-trip testi eklendi), scoped re-review tüm bulguları "ADDRESSED, no new breakage" olarak doğruladı.

## Değişen dosyalar
- `docs/16-adr.md` — ADR-017 eklendi (ADR-012 silinmedi/değiştirilmedi)
- `docs/19-technology-baseline.md` — ikinci doğrulanmış model notu
- `src/main/resources/db/migration/V4__session_id.sql` — yeni, `investigation.session_id` (TEXT, nullable) + index
- `src/main/java/.../domain/Investigation.java` — opsiyonel `sessionId`, yeni `receive`/`reconstitute` overload'ları
- `src/main/java/.../domain/InvestigationRepository.java`, `adapters/persistence/JdbcInvestigationRepository.java` — `findBySessionId`
- `src/main/java/.../api/SessionController.java`, `api/dto/InvestigationDtoMapper.java` — yeni
- `src/main/java/.../agent/IncidentAnalysisAiService.java` — `@MemoryId` parametresi
- `src/main/java/.../agent/SessionChatMemoryStore.java` — yeni, LRU-sınırlı (`chat-memory-max-sessions:1000`)
- `src/main/java/.../application/IncidentInvestigationService.java` — memoryId taşıyan yeni overload (eski overload'lar kaynak-uyumlu)
- `src/main/java/.../config/AgentConfig.java` — `chatModelFactory` artık `Function<String, ChatModel>` (model-id cache'li); `KnowledgeIngestionService`/`KnowledgeRepository` bean'leri; non-live `knowledgeSearchPort` artık `CompositeKnowledgeSearchPort`
- `src/main/java/.../config/InvestigationOrchestrator.java` — `runInvestigation(question, window, correlationId, sessionId, modelId, mode)`, mod-bağımlı tool bütçesi
- `src/main/java/.../agent/InvestigationMode.java` — yeni enum
- `src/main/java/.../api/ModelCatalog.java`, `api/ModelsController.java` — yeni, `GET /api/v1/models`
- `src/main/java/.../agent/AgentTools.java` — `ragEnabled` bayrağı, quick modda `searchIncidentKnowledge` guard'a girmeden boş dönüyor
- `src/main/java/.../rag/HashEmbeddingService.java` — yeni (test double'dan taşındı), `KnowledgeRepository.listDocuments()`, `KnowledgeDocumentSummary`
- `src/main/java/.../rag/fixtures/CompositeKnowledgeSearchPort.java` — yeni, fixture + gerçek pgvector sonucunu birleştiriyor
- `src/main/java/.../rag/JdbcKnowledgeSearchAdapter.java` — `embedding_model` filtresi eklendi
- `src/main/java/.../api/KnowledgeController.java`, `api/dto/KnowledgeDocumentDto.java` — yeni, `POST`/`GET /api/v1/knowledge/documents`
- `src/main/resources/application.yml` — `chat-memory-max-messages:40`, `chat-memory-max-sessions:1000`, `quick-mode-max-tool-calls:5`
- `src/test/java/...` — her task için yeni/güncellenmiş testler (bkz. commit geçmişi), en kritikleri: `SessionMemoryAiServiceTest` (gerçek `@MemoryId` round-trip), `QuickModeControllerTest` (quick modun tam sonuç döndüğünü kanıtlıyor), `KnowledgeControllerTest` (gerçek autowired `KnowledgeSearchPort` üzerinden upload→search), `SessionChatMemoryStoreTest` (LRU eviction dahil), `NvidiaNimAlternateModelLiveTest` (`@Tag("local-live")`)
- `docs/superpowers/plans/2026-08-02-m11-agent-console-backend.md` — implementasyon planı

## Testler (gerçek komut + gerçek çıktı, iddia değil)
Komut: `mvn -o verify` (`NVIDIA_API_KEY` olmadan)
Çıktı özeti:
```
[INFO] Results:
[INFO]
[INFO] Tests run: 167, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] --- spotless-maven-plugin:2.43.0:check (spotless-check) @ otp-sentinel ---
[INFO] Spotless.Java is keeping 187 files clean - 0 needs changes to be clean, 0 were already clean, 187 were skipped because caching determined they were already clean
[INFO] BUILD SUCCESS
[INFO] Total time:  59.307 s
```

Ayrıca (implementer tarafından, ayrı çalıştırma): `NvidiaNimAlternateModelLiveTest` gerçek `NVIDIA_API_KEY` ile `-Dsurefire.excludedGroups= -Dtest=NvidiaNimAlternateModelLiveTest` → 1/1 pass, `meta/llama-3.3-70b-instruct` tool-call round trip doğrulandı (M5'teki `503 ResourceExhausted` geçiciymiş, kapasite sorunuymuş).

## Karşılanan requirement/AC
- Session memory aynı/farklı sessionId testi — `SessionMemoryAiServiceTest`: gerçek `AiServices`+`@MemoryId` proxy üzerinden 2. çağrının 1.'yi hatırladığını, farklı sessionId'nin hatırlamadığını `StubChatModel.lastRequest()` ile kanıtlıyor.
- Model spike + `GET /models` yalnızca doğrulananları döner — `NvidiaNimAlternateModelLiveTest` (local-live) + `ModelsControllerTest` (her iki model id de assert ediliyor).
- Quick mode'un daha az tool çağrısı yaptığı VE tam sonuç döndürdüğü — `QuickModeControllerTest`: `PARTIAL_ANALYSIS` yok, `ANOMALY_CONFIRMED`, evidence sayısı thorough ile eşit, `knowledgeReferences` boş (RAG atlandı).
- Belge yükleme sonrası `searchIncidentKnowledge`'ın yeni belgeyi bulduğu — `KnowledgeControllerTest`: gerçek autowired `KnowledgeSearchPort` bean'i (artık `CompositeKnowledgeSearchPort`) üzerinden hem `INC-2026-041` hem yeni yüklenen belge dönüyor.
- `mvn verify` (tüm proje, offline) BUILD SUCCESS — yukarıda.
- ADR-017 yazıldı, ADR-012 ile çelişki açıkça not düşüldü (silinmedi, üzerine yazıldı) — `docs/16-adr.md`.
- `domain` paketi framework-free kaldı (yalnızca `String sessionId` eklendi, Spring/LangChain4j import'u yok).
- Main test suite `NVIDIA_API_KEY` olmadan yeşil; yalnızca `@Tag("local-live")` testler gerçek key gerektiriyor.

## Karşılanmayan / ertelenen
- `ModelsControllerTest`'in tam response body'sini değil model id'lerini assert etmesi (yeterli, ama daha sıkı olabilirdi) — final fix dalgasında düzeltildi.
- `KnowledgeController.upload` her zaman yeni rastgele `documentId` + sabit `version:"1"` üretiyor — gerçek bir versiyonlama/update yolu yok (kapsam dışı, M11 promptunda istenmedi).
- Hash-embedding min-score (`0.10`) NVIDIA-embedding'in min-score'undan (`0.70`) çok daha gevşek — hash-trick embedding'in doğası gereği, regresyon değil.
- Non-live modda `searchIncidentKnowledge` artık her investigation'da gerçek bir pgvector sorgusu yapıyor (önceden salt in-memory'ydi) — mevcut ölçekte önemsiz, trafik büyürse tekrar değerlendirilebilir.
- Quick mode'un sistem promptu hâlâ `searchIncidentKnowledge`'ı 6. adım olarak listeliyor; araç artık guard'a girmeden boş dönüyor ama model yine de bir zararsız round-trip harcıyor — mode-aware prompt (ikinci bir `AiServices` arayüzü) gerektirir, kapsam dışı bırakıldı.

## Spec çelişkisi/belirsizlik (varsa)
Yok — final review'daki quick-mode davranışı kararı (RAG'ı atla + budget'ı 5'e çıkar, sistem promptunu aşırt-ve-reddet yerine) kullanıcıya soruldu ve onaylandı (bkz. oturum içi soru/cevap).

## Sonraki oturum için not
Bağımsız oturum `mvn verify` gerçekten çalıştırıp doğrulamalı; ardından M12 (frontend sohbet arayüzü) promptu verilecek.
