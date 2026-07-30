# 00 — Project Charter

## Proje kimliği

- **Çalışma adı:** OTP Incident Investigation Agent
- **Önerilen ürün adı:** OTP Sentinel
- **Repo adı:** `otp-incident-investigation-agent`
- **Tür:** Production-aware proof of concept
- **Ana kullanıcı:** OTP operasyon mühendisi veya backend geliştirici
- **Uygulama:** Java 21 + Spring Boot modüler monolith
- **AI:** LangChain4j
- **Veri:** PostgreSQL + pgvector

## Problem

OTP teslimat oranı düştüğünde operasyon çalışanı metrik, hata kodu, kuyruk, provider, deploy ve geçmiş incident bilgisini farklı kaynaklardan manuel toplar. Bu süreç ilk değerlendirmeyi geciktirir, kişiye bağlı bilgi üretir ve kanıt ile varsayımın karışmasına neden olabilir.

## Hedef

Doğal dilde verilen operasyon sorusunu alarak:

1. anormalliği doğrulamak,
2. izin verilen salt-okunur tool'larla kanıt toplamak,
3. geçmiş incident ve runbook'ları RAG ile getirmek,
4. en fazla üç hipotez üretmek,
5. güvenli kontrol önerileri sunmak,
6. yalnızca açık kullanıcı onayıyla incident taslağı oluşturmak.

## Başarı tanımı

MVP şu koşulları sağlamalıdır:

- Ana fixture'da status `ANOMALY_CONFIRMED`, severity `HIGH`.
- Operatör B yoğunlaşması belirlenir.
- İç kuyruğun normal olduğu gösterilir.
- Connection pool/provider degradation en güçlü hipotez olur.
- Deploy ilişkisi korelasyon olarak ifade edilir; kesin neden denmez.
- Her operasyonel iddia bir tool veya knowledge kaynağına bağlanır.
- Onaysız incident oluşmaz.
- Aynı idempotency key ikinci incident üretmez.
- `docker compose up --build` ile sistem ayağa kalkar.
- CI ana senaryoyu canlı LLM olmadan doğrular.

## Kapsam içi

- Doğal dil soru
- Zaman aralığı çözümleme
- OTP metrics, errors, queue, provider, recent changes
- RAG
- Tool calling
- Structured output
- Evidence/claim validation
- Human-in-the-loop
- Audit
- Docker
- Unit, integration ve ATDD

## Kapsam dışı

- Gerçek telekom bağlantıları
- Gerçek SMS/OTP gönderimi
- Otomatik remediation
- Fine-tuning
- Kafka/Kubernetes
- Tam UI
- Çok kiracılı SaaS

## Kısıtlar

- Her teknoloji gerçek bir ihtiyacı karşılamalıdır.
- Agent en fazla sekiz tool çağrısı yapmalıdır.
- LLM write yetkisine sahip değildir.
- Canlı model yokken stub profil çalışmalıdır.
- Şirket içi mimari varmış gibi anlatılmamalıdır.

## Kapsam değişikliği filtresi

Yeni özellik ancak şu soruların tümüne “evet” deniyorsa eklenir:

1. Ana OTP senaryosunu güçlendiriyor mu?
2. Acceptance criterion ile ölçülebiliyor mu?
3. Demo güvenilirliğini bozmuyor mu?
4. Gereksiz yeni altyapı eklemiyor mu?
5. Teknik görüşmede açıklanabilir mi?
