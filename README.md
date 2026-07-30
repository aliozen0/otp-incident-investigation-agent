# OTP Incident Investigation Agent

Java tabanlı, kanıta dayalı OTP operasyon inceleme agent'ı için **spec-driven development** belge paketi.

## Amaç

Sistem, OTP teslimat performansındaki düşüşleri doğal dilde verilen bir operasyon sorusu üzerinden araştırır. Anlık operasyon verilerini tool calling ile toplar, geçmiş incident ve runbook belgelerini RAG ile getirir, kanıtlarla desteklenen hipotezler üretir ve kullanıcı onayıyla incident taslağı oluşturur.

Bu sistem bir metric dashboard veya tam otonom remediation ürünü değildir. Mevcut operasyon araçlarının üzerinde çalışan bir **araştırma ve karar destek katmanıdır**.

## Sabit teknoloji tabanı

- Java 21
- Spring Boot
- LangChain4j
- PostgreSQL + pgvector
- Maven
- Docker Compose
- JUnit 5
- Testcontainers
- Yapılandırılabilir LLM ve embedding sağlayıcısı

## MVP senaryosu

> Son 15 dakikada OTP başarı oranı yaklaşık %98'den %72'ye düştüğünde agent, sorunun Operatör B gateway bağlantı havuzu veya provider yavaşlamasıyla ilişkili olabileceğini kanıtlarıyla araştırır.

## MVP dışı

- Gerçek OTP gönderimi
- Gerçek müşteri veya telefon verisi
- Otomatik rollback/restart/config değişikliği
- NETGSM'in iç mimarisini temsil etme iddiası
- Çoklu agent gösterisi
- Tam dashboard

## Belge haritası

| Belge | Amaç |
|---|---|
| `docs/00-project-charter.md` | Proje hedefi, kapsam ve başarı tanımı |
| `docs/01-product-vision.md` | Ürün vizyonu ve değer önerisi |
| `docs/02-prd.md` | Ürün gereksinimleri |
| `docs/03-system-requirements.md` | İşlevsel ve işlevsel olmayan gereksinimler |
| `docs/04-user-stories.md` | Kullanıcı hikâyeleri ve use case'ler |
| `docs/05-domain-and-architecture.md` | Domain modeli ve mimari |
| `docs/06-api-contracts.md` | REST API sözleşmeleri |
| `docs/07-agent-tool-spec.md` | Agent ve tool sözleşmeleri |
| `docs/08-rag-spec.md` | RAG tasarımı |
| `docs/09-security-governance.md` | Güvenlik ve AI yönetişimi |
| `docs/10-observability-slo.md` | Log, metric, trace ve SLO |
| `docs/11-acceptance-criteria.md` | Kabul kriterleri |
| `docs/12-atdd-gherkin.md` | ATDD/Gherkin senaryoları |
| `docs/13-test-strategy.md` | Test yaklaşımı |
| `docs/14-implementation-plan.md` | Uygulama planı ve backlog |
| `docs/15-demo-fixtures.md` | Ana demo verisi |
| `docs/16-adr.md` | Mimari karar kayıtları |
| `docs/17-traceability-risk-dod.md` | İzlenebilirlik, risk ve Definition of Done |
| `docs/18-demo-interview-guide.md` | Teknik görüşme sunum rehberi |
| `docs/19-technology-baseline.md` | Sürüm politikası ve resmî kaynaklar |
| `docs/20-git-workflow.md` | Branch stratejisi, commit convention, merge kuralı |

## Temel tasarım ilkesi

> LLM doğal dil yorumlama, araştırma planlama ve hipotez üretmede kullanılır. Yetki, idempotency, onay, veri doğrulama ve operasyonel aksiyonlar deterministik kod tarafından yönetilir.

## Geliştirme sırası

1. Ana fixture ve domain modelleri
2. Mock tool adapter'ları
3. PostgreSQL/pgvector ve RAG
4. LangChain4j tool calling
5. Structured output ve claim validation
6. Human-in-the-loop incident taslağı
7. ATDD ve Testcontainers
8. Docker, demo ve README
