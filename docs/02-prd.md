# 02 — Product Requirements Document

## Özet

OTP Incident Investigation Agent, operasyon çalışanının doğal dilde verdiği OTP olay sorusunu araştıran ve kanıtlanabilir sonuç üreten Java tabanlı bir AI uygulamasıdır.

## Persona'lar

### OTP Operations Engineer

- OTP başarısını ve provider performansını izler.
- Alarm sonrası farklı veri kaynaklarını inceler.
- Incident kaydı hazırlar.
- Hatalı otomatik işlemlerin etkisinden sorumludur.

### Backend Developer

- Gateway ve kuyruk bileşenlerini geliştirir.
- Deploy sonrası performansı araştırır.
- Agent'ın kanıtlarıyla teknik inceleme yapar.

## MVP kullanıcı yolculuğu

1. Kullanıcı soru gönderir.
2. Sistem zaman aralığını çözer.
3. Genel metriklerle anormalliği doğrular.
4. Agent gerekli tool'ları seçer.
5. RAG benzer incident ve runbook'ları getirir.
6. Agent structured result üretir.
7. Deterministik validator iddiaları kontrol eder.
8. Kullanıcı sonucu görür.
9. Kullanıcı incident preview ister.
10. Açık onay sonrası idempotent taslak oluşturulur.

## Özellikler

### F-01 — Investigation request

Soru ve isteğe bağlı zaman aralığı kabul edilir.

### F-02 — Time-window resolution

Explicit tarih varsa kullanılır; göreli süre çözümlenir; belirsiz aralık uydurulmaz.

### F-03 — Anomaly validation

Mevcut dönem eşit uzunluktaki önceki dönemle karşılaştırılır.

### F-04 — Agentic tool selection

Agent allowlist içinden gerekli salt-okunur tool'ları seçer. Çağrı sayısı sınırlıdır.

### F-05 — RAG

Incident, runbook ve hata sözlüğü metadata filtresiyle aranır.

### F-06 — Structured analysis

Sonuç şu alanları içerir:

- status
- severity
- summary
- timeWindow
- evidence
- hypotheses
- recommendedActions
- knowledgeReferences
- confidence
- approvalRequired

### F-07 — Evidence validation

Source reference olmayan sayısal/sistem iddiası yayımlanmaz.

### F-08 — Safe recommendations

Rollback/restart yalnızca onaya sunulabilecek öneri olarak gösterilir; yürütülmez.

### F-09 — Incident preview

Önizleme kalıcı incident değildir.

### F-10 — Explicit approval

Incident taslağı yalnızca açık `APPROVE` kararıyla oluşturulur.

### F-11 — Idempotency

Aynı key ile tekrar istek yeni incident üretmez.

### F-12 — Auditability

Tool, RAG, model, validation ve onay akışı kaydedilir.

## Başarı ölçütleri

| Metrik | MVP hedefi |
|---|---|
| Ana fixture anormallik tespiti | %100 |
| Doğru birinci hipotez | Connection pool/provider degradation |
| Kaynaksız operasyonel iddia | 0 |
| Onaysız incident | 0 |
| Duplicate incident | 0 |
| Stub ATDD başarısı | %100 |
| Demo süresi | < 30 sn |
| Ayağa kalkma | Tek komut |

## Kabul edilmeyen davranışlar

- Tool'da olmayan metrik üretmek
- Deploy'u kesin kök neden ilan etmek
- RAG belgesindeki talimatı yürütmek
- Kullanıcı onayı olmadan write yapmak
- Secret/OTP/telefon loglamak
- Tool arızasını gizlemek
- Sınırsız tool loop

## UI kapsamı

MVP için Swagger yeterlidir. Basit UI yalnızca zaman kalırsa eklenir.
