# 04 — User Stories and Use Cases

## Epic E-01 — OTP olayını araştırma

### US-001

Bir operasyon çalışanı olarak OTP düşüşünü doğal dilde sorabilmek istiyorum; böylece farklı ekranlarda manuel araştırma başlatmam.

### US-002

Mevcut dönemin önceki eşit dönemle karşılaştırılmasını istiyorum; böylece değişimin büyüklüğünü görürüm.

### US-003

Provider bazlı başarısızlık dağılımını görmek istiyorum; böylece sorunun genel mi yerel mi olduğunu anlarım.

### US-004

Kuyruk ve consumer sağlığını görmek istiyorum; böylece iç sistem problemini değerlendirebilirim.

### US-005

Deploy/config değişikliklerinin olay başlangıcına yakınlığını görmek istiyorum; böylece inceleme önceliğini belirlerim.

### US-006

Benzer geçmiş incident ve runbook'ları görmek istiyorum; böylece kurumsal bilgi tekrar kullanılır.

## Epic E-02 — Güvenilir analiz

### US-007

Her bulgunun kaynağını görmek istiyorum; böylece AI cevabına körü körüne güvenmem.

### US-008

Doğrulanmamış kök nedenin kesin gerçek olarak sunulmamasını istiyorum.

### US-009

Veri eksikse sistemin bunu açıkça söylemesini istiyorum.

### US-010

Sonucun sabit JSON şemasında olmasını istiyorum; böylece istemciler güvenilir işler.

## Epic E-03 — Incident taslağı

### US-011

Analizden incident taslağı önizlemek istiyorum.

### US-012

Kalıcı taslak oluşmadan açık onay vermek istiyorum.

### US-013

Ağ tekrarı nedeniyle iki incident oluşmamasını istiyorum.

## UC-01 — Ana incident araştırması

### Ön koşullar

- Uygulama çalışır.
- `OTP-DROP-001` yüklüdür.
- Knowledge base ingest edilmiştir.
- Kullanıcı investigation yetkisine sahiptir.

### Ana akış

1. Kullanıcı soruyu gönderir.
2. Sistem `14:15–14:30` aralığını çözer.
3. Genel metrikleri ve önceki dönemi getirir.
4. Anormallik doğrulanır.
5. Error distribution getirilir.
6. Provider health getirilir.
7. Queue health kontrol edilir.
8. Recent changes getirilir.
9. RAG benzer incident'ı getirir.
10. Structured result üretilir.
11. Evidence ve policy validation yapılır.
12. Sonuç döndürülür.

### Başarılı sonuç

- `ANOMALY_CONFIRMED`
- `HIGH`
- Operatör B etkisi
- Connection pool birinci hipotez
- Queue issue birinci hipotez değil
- Onaysız write yok

### Alternatifler

- Anormallik yok: `NO_ANOMALY`
- Provider tool timeout: `PARTIAL_ANALYSIS`
- RAG sonucu yok: live evidence ile warning
- Structured output iki kez bozuk: `FAILED`

## UC-02 — Incident onayı

1. Kullanıcı preview ister.
2. Sistem payload gösterir; incident yaratmaz.
3. Yetkili kullanıcı `APPROVE` + idempotency key gönderir.
4. Sistem yetki ve snapshot'ı doğrular.
5. Taslak oluşturulur ve audit edilir.
6. Aynı key tekrarında eski ID döner.
7. `REJECT` durumunda incident oluşmaz.

## Epic E-04 — Niyet farkındalıklı OTP operasyon sohbeti

### US-014

Bir operasyon çalışanı olarak selamlaşma, model kimliği ve kullanım sorularına toolsuz doğal cevap
almak istiyorum; böylece her mesaj gereksiz investigation başlatmaz.

### US-015

Belirsiz bir OTP sorusunda tek açıklayıcı takip sorusu almak istiyorum; böylece yanlış kapsamda tool
çağrısı yapılmaz.

### US-016

Açık analiz/kıyas/kök neden isteğimin mevcut agentic evidence pipeline'ına yönlenmesini istiyorum.

### US-017

Aynı oturumdaki doğrulanmış final özet üzerinden takip sorusu sorabilmek, farklı oturumlardan bağlam
sızmamasını istiyorum.

### US-018

Agent'ın seçtiği grafikleri yalnız canonical evidence değerleriyle görmek istiyorum; böylece görsel
halüsinasyonla karşılaşmam.

### UC-03 — Uyarlanabilir sohbet mesajı

1. Kullanıcı session/model/interaction mode ile mesaj gönderir.
2. AUTO ise toolsuz LLM router semantic intent üretir; Java route'u doğrular.
3. CHAT toolsuz doğal cevap, CLARIFICATION tek soru döndürür.
4. INVESTIGATION ise zaman aralığı çözülür ve mevcut orchestration/validation hattı çalışır.
5. Yalnız canonical evidence ile doğrulanan grafikler snapshot'a ve UI'a ulaşır.
6. Doğrulanmış kullanıcı/asistan turn'leri bounded session context'e eklenir.
