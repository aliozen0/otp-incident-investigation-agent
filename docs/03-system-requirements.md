# 03 — System Requirements Specification

## Gösterim

- `FR`: Functional Requirement
- `NFR`: Non-functional Requirement
- `AI`: AI-specific Requirement
- `DATA`: Data Requirement
- `SEC`: Security Requirement

## Functional requirements

### FR-001 — Investigation oluşturma

Sistem soru ve isteğe bağlı zaman aralığı içeren request'i kabul etmelidir.

### FR-002 — Zaman çözümleme

- Explicit `startAt/endAt` önceliklidir.
- Göreli süre sunucu saatine göre çözülür.
- Zaman ifadesi yoksa son 15 dakika güvenli varsayılan olarak kullanılır.
- Tüm timestamp'ler UTC saklanır.
- Çözümlenen veya varsayılan aralık 1 dakika–24 saat sınırını aşamaz.

### FR-003 — Dönem karşılaştırma

Mevcut aralık, eşit uzunluktaki önceki dönemle karşılaştırılmalıdır.

### FR-004 — Sonuç durumu

```text
NO_ANOMALY
ANOMALY_CONFIRMED
INSUFFICIENT_DATA
PARTIAL_ANALYSIS
FAILED
```

### FR-005 — Tool allowlist

Agent yalnızca tanımlı tool'ları çağırmalıdır.

### FR-006 — Tool budget

- En fazla 8 çağrı
- Aynı tool + aynı parametre için başarılı çağrı tekrarlanmaz
- Transient hata için en fazla 1 retry

### FR-007 — Read-only investigation

Investigation aşamasında tüm operasyon tool'ları salt-okunur olmalıdır.

### FR-008 — Knowledge retrieval

En fazla 5 knowledge chunk döndürülmeli; belge ID, sürüm ve chunk ID korunmalıdır.

### FR-009 — Structured result

AI çıktısı tanımlı `IncidentAnalysisResult` şemasına uymalıdır.

### FR-010 — Claim validation

Sayısal veya sistem durumu belirten her iddia geçerli source reference taşımalıdır.

### FR-011 — Hipotezler

- En fazla 3
- Olasılığa göre sıralı
- Her biri supporting evidence içerir

### FR-012 — Confidence

`0.0–1.0` aralığında olmalıdır; otomatik aksiyon izni sağlamaz.

### FR-013 — Incident preview

Preview kalıcı incident oluşturmamalıdır.

### FR-014 — Onay

Kalıcı taslak yalnızca yetkili kullanıcının explicit onayıyla oluşturulmalıdır.

### FR-015 — Idempotency

Aynı investigation ve idempotency key aynı incident kimliğini döndürmelidir.

### FR-016 — Re-fetch

Tamamlanan investigation ID ile tekrar görüntülenebilmelidir.

### FR-017 — Audit

Şu olaylar kaydedilmelidir:

- request accepted
- time window resolved
- tool called/completed/failed
- RAG completed
- LLM completed
- validation passed/failed
- preview generated
- approval/rejection
- incident created

### FR-018 — Agent console

- Aynı session içindeki investigation'lar sohbet akışında gösterilmelidir.
- Doğrulanmış model allowlist'i composer içinden seçilebilmelidir.
- Structured result içindeki doğal dil özeti canonical snapshot'ta saklanmalıdır.
- RAG citation alanları uygulamanın topladığı canonical metadata'dan üretilmelidir.

### FR-019 — Knowledge explorer

- Knowledge belgeleri tam metadata, güvenli içerik ve chunk ayrıntılarıyla listelenebilmelidir.
- Kullanıcı en fazla 5 sonuç döndüren salt-okunur retrieval preview çalıştırabilmelidir.
- Preview sonucu document ID, version, title, chunk ID ve similarity score taşımalıdır.

### FR-020 — Structured intent routing

`AUTO` mesajları seçili allowlist LLM ile, hiçbir tool bağlanmadan, `CHAT | CLARIFICATION |
INVESTIGATION` structured sonucuna yönlendirilmelidir. İlk bozuk output için en fazla bir repair;
ikinci bozuk output için `502 INTENT_ROUTING_FAILED` dönmelidir. Production kararı keyword/regex
tabanlı olmamalıdır.

### FR-021 — Interaction override ve analysis depth

`interactionMode=AUTO|CHAT|INVESTIGATION`, `investigationMode=QUICK|THOROUGH` olmalıdır. Explicit
CHAT toolsuz responder'a, explicit INVESTIGATION mevcut güvenli investigation pipeline'ına gider.

### FR-022 — Normal conversation

CHAT yanıtı seçili model, sınırlı OTP rolü ve bounded semantic session context ile doğal üretilmeli;
investigation tool'u, repository save veya incident akışı çalışmamalıdır.

### FR-023 — Clarification

Belirsiz mesaj tek doğrulanmış takip sorusu döndürmeli; tool veya investigation çalıştırmamalıdır.

### FR-024 — Chat API

`POST /api/v1/chat/messages` 1–2000 karakter mesaj, UUID session, verified model ve mode allowlist'i
kabul etmeli; her başarılı response dolu `assistantMessage` ve discriminated `responseType`
taşımalıdır. Time window yalnız investigation dalında çözülmelidir.

### FR-025 — Semantic context

Son kullanıcı/asistan semantic turn'leri, investigation tool-call memory'sinden ayrı, session-scoped,
LRU/message-bounded ve restart ile silinen bellekte tutulmalıdır. Session'lar arası sızıntı olmaz.

### FR-026 — Visualization schema

Investigation en fazla dört visualization; grafik başına en fazla dört series ve toplam 40 point
taşıyabilir. Tipler `LINE|BAR|GROUPED_BAR|GAUGE|TABLE`, birimler
`PERCENT|RATIO|COUNT|MILLISECONDS|CONNECTIONS|NONE` allowlist'indedir.

### FR-027 — Visualization evidence binding

Her numeric point canonical evidence ID'ye bağlı olmalı ve değer evidence metric'iyle yalnız
deterministik percent/ratio dönüşümü toleransı içinde eşleşmelidir. Prose evidence sayısal point
olamaz; uyumsuz birimler ve uydurma değerler reddedilir.

### FR-028 — Visualization persistence

Doğrulanmış visualization canonical investigation snapshot'ında saklanmalı; POST sonrası GET aynı
listeyi dönmelidir. Eski kayıtlar boş liste olarak okunmalıdır.

### FR-029 — Adaptive console

Composer interaction mode ile investigation depth'i ayrı sunmalı; kısa fakat boş olmayan chat
mesajlarını kabul etmelidir. CHAT/CLARIFICATION sade, INVESTIGATION canonical analiz panelleri ve
doğrulanmış grafikleriyle render edilmelidir.

### FR-030 — Suggestions

En fazla üç plain-text suggestion dönebilir. Her biri uzunluk, PII ve HTML/executable içerik
validasyonundan geçer ve hiçbir zaman otomatik aksiyon değildir.

## AI requirements

### AI-001 — Evidence bounded

Model yalnızca tool ve knowledge contextindeki veriyi gerçek olarak kullanmalıdır.

### AI-002 — Uncertainty language

Doğrulanmamış kök neden kesinlik diliyle sunulmamalıdır.

### AI-003 — No hidden remediation

Model gerçek sistem aksiyonu gerçekleştiremez.

### AI-004 — Retrieved content isolation

RAG içeriği talimat değil veri olarak işlenmelidir.

### AI-005 — Prompt injection resistance

Belgedeki “kuralları yok say” benzeri içerik politika değiştirmemelidir.

### AI-006 — Deterministic fallback

Stub profil ana fixture için deterministik sonuç üretmelidir.

### AI-007 — Model failure

Timeout/invalid JSON durumunda başarılı sonuç uydurulmamalıdır.

### AI-008 — Reproducibility metadata

Model provider/name, prompt version ve schema version saklanmalıdır.

- **AI-009:** Router semantik kararı LLM verir; Java enum/schema/confidence/policy doğrular.
- **AI-010:** CHAT responder'a ve router'a investigation tools veya RAG talimatları bağlanmaz.
- **AI-011:** Model evidence/citation kimliği veya visualization sayısal değeri üretemez; yalnız
  application contextinde verilen canonical ID/değerleri seçebilir.

## Data requirements

- **DATA-001:** Timestamps UTC ve ISO-8601.
- **DATA-002:** Gerçek telefon, OTP veya müşteri kimliği yok.
- **DATA-003:** Knowledge belge sürümü taşır.
- **DATA-004:** Embedding model/version saklanır.
- **DATA-005:** Audit append-only yaklaşımında tutulur.
- **DATA-006:** Canonical investigation snapshot doğal dil özetini ve zengin knowledge citation
  metadata'sını saklar.

## Non-functional requirements

- **NFR-001:** Stub investigation p95 < 3 s; live demo < 30 s.
- **NFR-002:** Domain katmanı Spring/LangChain4j'den ayrılmalıdır.
- **NFR-003:** Docker Compose ile portable olmalıdır.
- **NFR-004:** Canlı LLM olmadan test edilebilmelidir.
- **NFR-005:** REST API Java/PHP istemciyle plain JSON tüketilebilmelidir.
- **NFR-006:** API `/api/v1` altında versiyonlanmalıdır.
- **NFR-007:** Hatalar tutarlı problem-details formatında olmalıdır.
- **NFR-008:** Tool timeout, retry ve total deadline bulunmalıdır.
- **NFR-009:** Hipotez-evidence ilişkisi kullanıcıya görünmelidir.
- **NFR-010:** Agent console masaüstü ve mobil genişliklerde kullanılabilir, klavye ile erişilebilir
  ve tüm statik metinleri Türkçe olmalıdır.
- **NFR-011:** Semantic context en fazla yapılandırılmış mesaj/session sayısında tutulur, kalıcı genel
  chat/CRM deposu oluşturulmaz.
- **NFR-012:** Frontend unknown/invalid visualization ile çökmemeli; arbitrary config, raw HTML,
  dynamic component veya eval çalıştırmamalıdır.

## Security requirements

- **SEC-001:** Production endpoint'leri authentication gerektirir.
- **SEC-002:** Incident approval ayrı yetkidir.
- **SEC-003:** Secret repository'de tutulmaz.
- **SEC-004:** Log'da API key, OTP, telefon veya auth header yoktur.
- **SEC-005:** User/RAG içeriği typed validation olmadan tool parametresi olmaz.
- **SEC-006:** Write işlemleri idempotent ve audit edilmelidir.
- **SEC-007:** CHAT ve CLARIFICATION için tool çağrısı ve investigation persistence sıfır olmalıdır.
- **SEC-008:** Intent, suggestion ve visualization model output'u typed, bounded, PII/HTML-safe Java
  doğrulamasından geçmelidir.
