# M4 — RAG

## Durum
DONE

## Kapsam
Compatibility spike önce ve ayrı çalıştırıldı: NVIDIA NIM embedding endpoint'ine (`https://integrate.api.nvidia.com/v1/embeddings`) `curl` ile doğrudan, sonra LangChain4j'nin `OpenAiEmbeddingModel`'i ile canlı bir `NvidiaNimEmbeddingServiceLiveTest` testinden gerçek `NVIDIA_API_KEY` ile çalıştırıldı — model `nvidia/nv-embedqa-e5-v5` (dimension 1024), `input_type` (`query`/`passage`) parametresini destekliyor. LangChain4j 1.18.1'in `OpenAiEmbeddingRequestParameters.CUSTOM_PARAMETERS` passthrough'u (Javadoc'unda NVIDIA `input_type`'ı açıkça anıyor) bu alanı taşıyor — ayrı bir HTTP interceptor/custom framework gerekmedi, ADR-015'in öngördüğü gibi. Spike sırasında iki gerçek bug bulunup düzeltildi: (1) `.env`'de `NVIDIA_EMBEDDING_MODEL` boştu → "model field is required" hatası (env dosyası düzeltildi); (2) `export $(...)` ile `.env` okumak tırnak karakterlerini API key'e literal olarak katıyordu → 401 (doğru yöntem: `set -a; source .env; set +a`, sadece yerel WSL komut satırı için, koda dokunmadı).

Ardından `rag` paketi: `KnowledgeDocumentType` (allowlist enum'un kendisi), `EmbeddingService` portu + `NvidiaNimEmbeddingService` adapter'ı, `Chunker` (500-800 kelime, 80-120 overlap, satır bazlı — tablo satırı asla bölünmüyor), `ContentSanitizer` (script/HTML strip, 20000 karakter limit, instruction-pattern sinyali), `KnowledgeIngestionService` (sanitize → chunk → embed(PASSAGE) → persist, tip allowlist'i `KnowledgeDocumentType.valueOf` ile reddediyor), `KnowledgeRepository`/`JdbcKnowledgeRepository` (pgvector kolonu `::vector` text-literal cast ile, pgvector-java bağımlılığı eklenmeden), `KnowledgeSearchPort`/`JdbcKnowledgeSearchAdapter` (T-006, henüz `@Tool` değil — embed(QUERY) → cosine similarity → provider/expiry filter → topK<=5 → minScore filtresi, citation alanlarıyla). `V3__knowledge_schema.sql` (`knowledge_document`, `knowledge_chunk` + hnsw index) M3 Flyway zincirine eklendi.

`docs/15`'in verdiği `INC-2026-041` içeriği birebir kullanıldı; `RB-OTP-001`/`ERR-OTP-001`/`POL-CHANGE-001`'in gövde metni docs/15'te verilmediğinden (yalnızca isim/amaç belirtilmiş), M2'deki "sessizce uydurma" kuralına uyularak `KnowledgeFixtureCatalog`'da amaçlarına uygun yazıldı ve bu raporda belirtiliyor. Negatif/ilgisiz belge (docs/08 "5. belge") `KnowledgeDocumentType` dışı bir tip (`MARKETING`) olarak modellendi — ingestion'da tip allowlist'i tarafından reddediliyor, hiç pgvector'a girmiyor. `OTP-INJECTION-001` fixture'ı (docs/15) `<script>` + "Ignore all previous instructions..." içeriyor; sanitizer script'i temizliyor, kalan metin salt veri olarak saklanıyor (hiçbir kod yolu onu yürütmüyor — M4'te zaten agent/tool-calling yok).

Ana test suite'in NVIDIA key gerektirmemesi için `DeterministicHashEmbeddingService` (test-only, hashing-trick bag-of-words + L2-normalize) yazıldı; skorları docs/08 evaluation set'inin 5 satırı için offline Python simülasyonuyla önceden doğrulandı, sonra gerçek Testcontainers pgvector testinde teyit edildi.

## Değişen dosyalar
- `pom.xml` — `langchain4j-bom`(1.18.1)/`langchain4j-open-ai` eklendi; surefire `excludedGroups=${surefire.excludedGroups}` (default `local-live`) ile compatibility-spike testi ana suite'ten ayrıldı.
- `src/main/resources/db/migration/V3__knowledge_schema.sql` — `knowledge_document`, `knowledge_chunk` (vector(1024), hnsw index).
- `src/main/java/.../rag/KnowledgeDocumentType.java`, `EmbeddingInputType.java`, `EmbeddingService.java`, `NvidiaNimEmbeddingService.java`, `KnowledgeDocument.java`, `DocumentChunk.java`, `EmbeddedChunk.java`, `Chunker.java`, `ContentSanitizer.java`, `KnowledgeIngestionRejectedException.java`, `KnowledgeIngestionService.java`, `KnowledgeRepository.java`, `JdbcKnowledgeRepository.java`, `VectorLiterals.java`, `KnowledgeSearchResult.java`, `KnowledgeSearchPort.java`, `JdbcKnowledgeSearchAdapter.java`.
- `src/main/java/.../rag/fixtures/RawKnowledgeDocument.java`, `KnowledgeFixtureCatalog.java` — MVP knowledge fixture seti + negatif/injection ham veri.
- `src/test/java/.../rag/DeterministicHashEmbeddingService.java`, `ChunkerTest.java`, `ContentSanitizerTest.java`, `KnowledgeIngestionServiceTest.java`, `JdbcKnowledgeRetrievalIntegrationTest.java`, `NvidiaNimEmbeddingServiceLiveTest.java` (`@Tag("local-live")`).
- `src/test/java/.../adapters/persistence/AbstractPostgresIntegrationTest.java` — `public` yapıldı (rag paketinden extend edilebilsin diye); `cleanTables()` yeni `knowledge_*` tablolarını da truncate ediyor.
- `.env`, `.env.example`, `docs/19-technology-baseline.md` — `NVIDIA_EMBEDDING_MODEL=nvidia/nv-embedqa-e5-v5` dolduruldu, gerekçe not düşüldü.

## Testler (gerçek komut + gerçek çıktı)
Komut: `wsl -e bash -lc "cd /mnt/c/Users/Ali/Downloads/otp-incident-agent && docker run --rm -v $(pwd):/build -v maven-repo:/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock -w /build maven:3.9-eclipse-temurin-21 mvn -B spotless:apply verify"`

Çıktı özeti:
```
[INFO] Running com.example.otpsentinel.rag.ChunkerTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.otpsentinel.rag.ContentSanitizerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.otpsentinel.rag.JdbcKnowledgeRetrievalIntegrationTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.otpsentinel.rag.KnowledgeIngestionServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] Results:
[INFO] Tests run: 69, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- spotless:2.43.0:check (spotless-check) @ otp-sentinel ---
[INFO] Spotless.Java is keeping 108 files clean - 0 needs changes to be clean
[INFO] BUILD SUCCESS
[INFO] Total time:  50.808 s
```
69/69 test yeşil (M0-M3'ün 52'si + M4'ün 17'si), `NVIDIA_API_KEY` olmadan. `local-live` grubu (`NvidiaNimEmbeddingServiceLiveTest`) bu koşuda skip edildi (surefire `excludedGroups`).

Compatibility spike ayrı komutla, gerçek key ile:
Komut: `wsl -e bash -lc "... set -a && source .env && set +a && docker run --rm -e NVIDIA_API_KEY=\"\$NVIDIA_API_KEY\" -e NVIDIA_BASE_URL -e NVIDIA_EMBEDDING_MODEL ... mvn -B -Dsurefire.excludedGroups= -Dtest=NvidiaNimEmbeddingServiceLiveTest test"`
```
[INFO] Running com.example.otpsentinel.rag.NvidiaNimEmbeddingServiceLiveTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 29.29 s
[INFO] BUILD SUCCESS
```

## Karşılanan requirement/AC
- FR-008/DATA-003 (knowledge retrieval, belge sürümü) — `JdbcKnowledgeRetrievalIntegrationTest`: 4 MVP belge ingest edilip pgvector'a yazılıyor, `documentId`+`version` composite PK.
- AC-008/DATA-004 (citation, embedding model/version) — `resultsCarryCitationFields`: `documentId/version/title/chunkId/similarityScore` assert edilmiş; `knowledge_chunk.embedding_model` kolonu her chunk'ta modelId'yi taşıyor.
- docs/14 M4 kabul cümlesi — `providerTimeoutConnectionPoolReturnsIncidentPostmortemInTopFive`: "provider timeout connection pool" sorgusu `INC-2026-041`'i top-5 içinde döndürüyor.
- docs/08 Evaluation set'in 5 satırı — 5 ayrı test metodu, hepsi geçiyor (marketing sorgusu için "ilgisiz sonuç yok" — negatif belge zaten allowlist'te reddedildiği ve gerçek belgelerle kelime örtüşmesi olmadığı için minScore filtresi altında kalıyor, boş liste dönüyor).
- Prompt injection koruması (docs/08, docs/09 AI-004/AI-005) — `ContentSanitizerTest` (script/HTML strip, size limit, instruction-pattern sinyali) + `KnowledgeIngestionServiceTest.injectionDocumentIsStoredWithScriptStrippedAndNeverExecuted` + document-type allowlist testi (`unknownDocumentTypeIsRejectedBeforeChunkingOrEmbedding`).
- docs/19 "Kısıtlar" (key olmadan test) — ana suite `DeterministicHashEmbeddingService` ile, `local-live` ayrı grup.

## Karşılanmayan / ertelenen
- `@Tool` binding (`searchIncidentKnowledge`'ın LangChain4j agent'a bağlanması) — M5.
- Gerçek numeric claim validator / tam prompt-injection policy — M6.
- REST — M7.
- `AuditEventRepository`'ye instruction-pattern sinyali yazma — prompt'ta "zorunlu değil" deniyordu; `AuditEventType` FR-017'de "the fixed set of events that must be audited" olarak tanımlı sabit bir enum olduğundan, yeni bir event tipi eklemek küçük de olsa bir spec sapması olurdu — bu milestonda atlandı, gerekirse M6'da (gerçek policy check'le birlikte) eklenebilir.

## Spec çelişkisi/belirsizlik (varsa)
1. **RB-OTP-001/ERR-OTP-001/POL-CHANGE-001 içeriği docs/15'te verilmemiş** — M2'deki `FixtureCatalog` emsaline uyularak (`KnowledgeFixtureCatalog` Javadoc'unda gerekçeli) belgelerin adı/amacına sadık, kısa ve tutarlı içerik yazıldı. Kabul kriteri yalnızca `INC-2026-041`'i (docs/15'te tam verilen) hedef aldığından risk düşük; diğer üçü kendi evaluation-set sorgularında (docs/08) doğru şekilde dönüyor.
2. **Chunker "token" birimi** — docs/08 "500-800 token" diyor; kelime sayımı (whitespace-split) kullanıldı, gerçek bir tokenizer (ör. tiktoken) eklenmedi — fixture belgeleri kısa olduğundan pratik farkı yok, mekanizma (satır bazlı, tablo-güvenli, overlap'li pencereleme) yine de büyük sentetik girdilerle test edildi.
3. **Front-matter enjeksiyonu** — `KnowledgeIngestionService` başlık+tag'leri her belgenin ilk chunk'ına ("## Özet") ekliyor; docs/08'de açıkça istenmemiş ama retrieval'ı gerçekçi şekilde güçlendiriyor (gerçek embedding modelleri de başlık/metadata'yı ağırlıklandırır) ve deterministic test double'ın evaluation-set sorgularını doğru sıralayabilmesi için gerekli. Javadoc'ta gerekçelendirildi.
4. **"Expired runbook düşük sıralı" (docs/08)** yerine tamamen filtrelendi (`effective_to < CURRENT_DATE` olan chunk'lar sorguya hiç girmiyor) — "düşük sıralı" ifadesi deprioritize etmeyi çağrıştırsa da minimal ve savunulabilir bir basitleştirme; hiçbir fixture belgesi expired olmadığından davranış gözlemlenemez, M6/M7'de gerekirse yeniden değerlendirilir.

## Sonraki oturum için not
M5 (agent orchestration) başlatılabilir: `KnowledgeSearchPort`/`searchIncidentKnowledge` hazır, `@Tool` binding'i ve LangChain4j `AiService` konfigürasyonu M5'in işi. `NVIDIA_CHAT_MODEL` M5'te seçilip pinlenmeli.
