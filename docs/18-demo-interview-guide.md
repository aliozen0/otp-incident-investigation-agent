# 18 — Demo and Technical Interview Guide

## 30 saniyelik anlatım

> OTP teslimatında bir düşüş olduğunda operasyon çalışanı farklı sistemlerden veri toplamak zorunda kalıyor. Java, Spring Boot ve LangChain4j ile çalışan bir investigation agent geliştirdim. Agent metrik, hata, kuyruk, provider ve deploy verilerini tool calling ile topluyor; geçmiş incident'ları pgvector tabanlı RAG ile araştırıyor; kanıtları olan hipotezler üretiyor. Write işlemlerini LLM'e bırakmadım; incident oluşturmak açık insan onayı ve idempotency gerektiriyor.

## Vizyon cümlesi

> Mevcut iletişim sistemlerini yeniden yazmak yerine, onların üzerinde çalışan açıklanabilir bir AI araştırma katmanı tasarladım.

## Neden Java?

> Şirketin Java kullandığını öğrendiğim için yalnızca bildiğim Python ekosisteminde kalmadım. Yeni stack'i öğrenme isteğimi çalışan bir sistemle göstermek istedim.

## Neden LangChain4j?

> Tool calling, RAG ve structured output'u Java içinde doğal çözmek için. Framework'ü domain'e yaymadım; agent adapter katmanında tuttum.

## Neden multi-agent değil?

> MVP'de tek araştırma amacı var. Çok agent latency ve hata yüzeyini büyütürdü. Tek investigation agent + deterministik policy daha doğru. Domain sayısı artarsa uzman agent'lar değerlendirilir.

## Neden otomatik rollback yok?

> Hipotez doğrulanmış kök neden değildir. Bu yüzden sistem kontrol ve change proposal önerir; aksiyon insan onayındadır.

## Demo akışı — 5–7 dakika

1. Problemi ve soruyu göster.
2. Swagger'dan investigation request gönder.
3. Tool trace'i göster: metrics, errors, queue, provider, changes, RAG.
4. Sonuçta şunları vurgula:
   - %98.1 -> %72.1
   - Operatör B
   - Queue normal
   - Connection pool hipotezi
   - Correlation, not causation
   - Citation
5. Preview iste; incident oluşmadığını göster.
6. Approve et; incident ID göster.
7. Aynı key ile tekrar et; duplicate olmadığını göster.
8. Zaman kalırsa provider timeout veya prompt injection failure senaryosu.

## Zor teknik sorular

### “Bu agent mı, workflow mu?”

> Time resolution, validation ve approval deterministik workflow. Hangi diagnostik tool'ların çağrılacağı ve evidence'dan hipotez üretimi agentic. Bilerek hybrid tasarladım.

### “Halüsinasyonu nasıl engelledin?”

> Tamamen engellediğimi söylemiyorum. Evidence ID'lerini uygulama üretiyor, structured output doğrulanıyor, source'suz sayı reddediliyor, kritik aksiyonlar model dışında.

### “RAG neden?”

> Anlık metrik kök neden bilgisini taşımaz. Geçmiş incident ve runbook kontrol adımı sağlar. Historical knowledge live evidence'dan güçlü kabul edilmiyor.

### “Neden pgvector?”

> MVP'de transactional data ve vector search'ü tek DB'de tutarak operasyonel karmaşıklığı azalttım. Ölçek ihtiyacı kanıtlanırsa ayrı store düşünülür.

### “Bu gerçek bir kurum sistemi mi?”

> Hayır. Herhangi bir kurumun iç mimarisini temsil etmiyor. Agentic AI ve Java yaklaşımını göstermek için sentetik verilerle hazırlanmış bir proof of concept.

### “PHP nerede?”

> AI servisi bağımsız REST API. Mevcut PHP servisleri JSON üzerinden çağırabilir; yeniden yazmak gerekmez.

## Gösterilmemesi gerekenler

- API key
- debug stack trace
- raw uzun prompt
- yarım UI
- çalışmayan live model
- şirket içi mimari iddiası
- “AI kesin kök nedeni buldu” sözü

## Kapanış

> Amacım yalnızca LLM kullanmak değildi. Yeni Java stack'ini öğrenip tool calling, RAG, güvenlik, test, onay ve gözlemlenebilirliği dar bir iş problemi etrafında birleştirdim.
