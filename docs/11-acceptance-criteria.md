# 11 — Acceptance Criteria

## Ana senaryo

### AC-001

`OTP-DROP-001` için status `ANOMALY_CONFIRMED`, severity `HIGH` olmalıdır.

### AC-002

Mevcut başarı yaklaşık `%72.1`, önceki başarı yaklaşık `%98.1` ve source reference ile gösterilmelidir.

### AC-003

Operatör B başarı oranı yaklaşık `%51.7` olmalı ve ana yoğunlaşma noktası olarak gösterilmelidir.

### AC-004

Queue healthy iken iç kuyruk problemi birinci hipotez olmamalıdır.

### AC-005

Birinci hipotez connection pool kapasitesi/connection release problemi veya anlamca eşdeğeri olmalıdır.

### AC-006

Provider tarafı geçici yavaşlama alternatif hipotez olabilir.

### AC-007

Gateway v2.4 ilişkisi korelasyon olarak sunulmalı; kesin nedensellik iddia edilmemelidir.

### AC-008

`INC-2026-041` ve sürüm bilgisi knowledge reference içinde olmalıdır.

### AC-009

Her hypothesis geçerli evidence ID içermelidir.

### AC-010

En fazla üç hypothesis olmalıdır.

### AC-011

Confidence 0–1 aralığında, ana fixture için 0.80–0.92 olmalıdır.

## Güvenlik ve onay

### AC-012

Restart/rollback/config otomatik yürütülmemelidir.

### AC-013

Approval olmadan createIncidentDraft çağrılmamalıdır.

### AC-014

Aynı idempotency key iki çağrıda tek incident üretmelidir.

### AC-015

REJECT kararında incident oluşmamalı, neden audit edilmelidir.

### AC-016

Investigation en fazla sekiz tool çağrısı yapmalıdır.

### AC-017

Aynı başarılı tool+parametre kombinasyonu tekrarlanmamalıdır.

## Failure davranışı

### AC-018

Provider tool timeout olduğunda diğer kanıtlarla `PARTIAL_ANALYSIS` ve eksik veri uyarısı dönmelidir.

### AC-019

getOtpMetrics başarısızsa başarılı analysis üretilmemeli, `FAILED` dönmelidir.

### AC-020

RAG sonucu yoksa live evidence ile devam edilmeli, knowledge reference boş ve warning dolu olmalıdır.

### AC-021

Knowledge içindeki prompt-injection talimatı tool policy veya approval akışını değiştirmemelidir.

### AC-022

Invalid structured output için en fazla bir repair; ikinci hatada `FAILED`.

### AC-023

Tool'da bulunmayan sayısal iddia validation failure üretmelidir.

## API ve platform

### AC-024

Gelecek zaman veya 24 saatten uzun interval `400 INVALID_TIME_WINDOW` dönmelidir.

### AC-025

Temiz ortamda `docker compose up --build` sonrası app ve DB healthy olmalıdır.

### AC-026

LLM API key olmadan stub profilde ana senaryo çalışmalıdır.

### AC-027

Başarılı investigation request, tool, RAG, validation ve completed audit kayıtlarını üretmelidir.

### AC-028

Test loglarında API key, OTP veya telefon numarası bulunmamalıdır.

### AC-029

Response zorunlu alanları ve geçerli enum değerlerini taşımalıdır.

### AC-030

GET endpoint aynı canonical result snapshot'ını döndürmelidir.

## M12.1 agent console ve RAG explorer

### AC-031

POST ve sonraki GET response'ları aynı doğal dil `summary` değerini döndürmelidir.

### AC-032

Knowledge reference `documentId`, `version`, `title`, `chunkId` ve canonical similarity score
taşımalıdır; model bilinmeyen citation metadata'sı ekleyememelidir.

### AC-033

Model seçici composer içinde görünmeli ve yalnızca canlı compatibility testiyle doğrulanmış allowlist
değerlerini investigation request'e göndermelidir.

### AC-034

Knowledge explorer yüklenen belgeleri metadata, sanitize edilmiş içerik ve chunk ayrıntılarıyla
göstermelidir.

### AC-035

Retrieval preview en fazla beş sonuç döndürmeli ve yeni yüklenen alakalı belgeyi citation alanlarıyla
gösterebilmelidir.

### AC-036

WSL2 üzerinden `docker compose up --build` sonrasında SPA, API ve PostgreSQL healthy olmalı; stub
akışı API key olmadan çalışmalıdır.

## M12.2 investigation request düzeltmesi

### AC-037

`timeWindow` gönderilmediğinde `son 15 dakikada` ifadesi sunucu saatine göre tam 15 dakikalık
UTC aralığına dönüştürülmeli; zaman ifadesi yoksa son 15 dakika varsayılanı kullanılmalıdır.

### AC-038

Composer'daki `datetime-local` değeri tarayıcının yerel saatinden ISO-8601 UTC'ye çevrilmeli;
yerel saat ham olarak `Z` ekiyle gönderilmemelidir.

### AC-039

10 karakterden kısa soru API'ye gönderilmemeli; composer kullanıcıya OTP metriği, operatör ve
zaman bağlamı içeren daha açıklayıcı bir soru yazması için inline yönlendirme göstermelidir.

### AC-040

Live demo auto-ingest kataloğu en az 16 benzersiz sentetik OTP belgesi içermeli; incident,
runbook, provider playbook, error reference ve change policy türlerinin tamamını kapsamalı ve
ikinci başlangıçta duplicate üretmemelidir.

## M12.3 intent-aware OTP operational chat

### AC-041
Normal selam/yetenek sorusu CHAT döner, 0 tool ve 0 investigation kaydı üretir.

### AC-042
Model kimliği sorusu seçili allowlist modelini doğru söyler ve analiz başlatmaz.

### AC-043
Açık analiz talebi INVESTIGATION döner ve mevcut evidence validation hattından geçer.

### AC-044
Belirsiz operasyon mesajı CLARIFICATION döner ve 0 tool çağırır.

### AC-045
Explicit interaction mode semantik routing'i güvenli biçimde override eder.

### AC-046
Aynı session CHAT/INVESTIGATION turn'leri arasında bounded semantic context taşır; session'lar
arasında sızıntı olmaz ve tool memory ile karışmaz.

### AC-047
LLM visualization yalnız canonical metric evidence ile render edilir; uydurma değer, unknown
evidence/type veya incompatible unit reddedilir.

### AC-048
`OTP-DROP-001` anomaly investigation en az bir valid current/previous başarı karşılaştırma grafiği
döndürür ve POST/GET aynı canonical grafiği taşır.

### AC-049
Normal chat/clarification UI'ında investigation panelleri görünmez; investigation cevabında canonical
evidence, hypothesis, RAG, visualization ve approval bölümleri görünür.

### AC-050
Prompt injection interaction mode, tool allowlist, human approval veya visualization validation'ı
değiştiremez.

### AC-051
Ana suite NVIDIA key olmadan deterministik router/chat/clarification/investigation yollarını doğrular.

### AC-052
WSL2 Compose stub ve yalnız onaylı sentetik fixture kullanan live smoke akışları çalışır.
