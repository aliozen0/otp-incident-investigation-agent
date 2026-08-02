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

1. Kullanıcı mesajı ve etkileşim modu gönderir.
2. `AUTO` modunda seçili LLM, toolsuz structured intent routing yapar.
3. Java route şemasını, confidence değerini ve model allowlist'ini doğrular.
4. `CHAT` toolsuz doğal yanıt; `CLARIFICATION` toolsuz tek takip sorusu üretir.
5. `INVESTIGATION` dalında zaman aralığı çözülür ve genel metriklerle anormallik doğrulanır.
6. Agent gerekli salt-okunur tool'ları seçer; `THOROUGH` modda RAG kullanabilir.
7. Agent structured sonuç ve kanıta bağlı visualization önerileri üretir.
8. Deterministik validator iddiaları, evidence ID'lerini ve grafik değerlerini kontrol eder.
9. Kullanıcı response türüne uygun yoğunlukta sonucu görür.
10. Kullanıcı incident preview ister; açık onay sonrası idempotent taslak oluşturulur.

## Özellikler

### F-01 — Investigation request

Soru ve isteğe bağlı zaman aralığı kabul edilir.

### F-02 — Time-window resolution

Explicit tarih varsa kullanılır; `son N dakika/saat` gibi göreli süre sunucu saatine göre
çözümlenir. Zaman ifadesi yoksa incident konsolu için güvenli varsayılan son 15 dakikadır;
1 dakika–24 saat sınırı her durumda deterministik olarak uygulanır.

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

MVP (M0-M8) için Swagger yeterliydi. M9/M10'da bu revize edildi: proje artık portföy/demo amacıyla
birinci sınıf bir web UI içerir — bkz. `docs/16-adr.md` ADR-016. Swagger hâlâ API sözleşmesi için
mevcut kalır; UI onun üzerine, aynı REST API'yi tüketen ayrı bir SPA olarak eklenir.

M12.1 ile web UI kurumsal bir agent console olarak ele alınır: model seçimi doğrudan sohbet
composer'ında görünür, agent'ın doğrulanmış doğal dil özeti sohbet mesajı olarak sunulur ve RAG
bilgi tabanı belge envanteri, güvenli içerik/chunk ayrıntısı ile read-only retrieval preview sağlar.
Konsol projeyi genel amaçlı bir chatbot'a dönüştürmez; yalnızca OTP incident investigation akışını
görselleştirir.

M12.3 bu sınırı, genel amaçlı chatbot'a dönüşmeden, sınırlı OTP operasyon sohbeti lehine revize
eder. Asistan selamlaşma, seçili model/kimlik/yetenek, kullanım ve mevcut OTP investigation bağlamı
hakkında konuşabilir. Kapsam dışı talepleri toolsuz reddeder. Production routing keyword/regex ile
değil LLM structured output ile yapılır; Java yalnız deterministik emniyet kapılarını uygular.

### F-13 — Intent-aware operational chat

`AUTO | CHAT | INVESTIGATION` etkileşim modu ile `QUICK | THOROUGH` investigation derinliği ayrı
kavramlardır. CHAT ve CLARIFICATION yolları sıfır tool çağrısı ve sıfır investigation kaydı üretir.

### F-14 — Clarification

Belirsiz operasyon mesajı, investigation başlatmadan tek net takip sorusuna dönüştürülür.

### F-15 — Evidence-bound visualizations

Model yalnız dar `LINE | BAR | GROUPED_BAR | GAUGE | TABLE` şemasından grafik seçebilir. Her numeric
point canonical application-minted evidence ID ve doğrulanmış metric değeri taşır; arbitrary
HTML/JavaScript/config kabul edilmez.
