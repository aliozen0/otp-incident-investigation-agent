# 09 — Security and AI Governance

## Güvenlik hedefleri

- Yetkisiz operasyonu engellemek
- Secret/PII sızıntısını önlemek
- İddia ve aksiyon izini tutmak
- Prompt injection etkisini azaltmak
- Model/tool arızasında güvenli kalmak

## Ana varlıklar

- LLM API key
- Operasyon metrikleri
- Incident içeriği
- Kullanıcı kimliği
- Audit
- Internal runbook'lar

## Tehditler

- Prompt injection
- Tool argument injection
- Kanıtsız halüsinasyon
- Onaysız write
- Duplicate incident
- Secret loglama
- Tool-loop maliyet/DoS
- Bozuk knowledge belge
- Yetkisiz approval

## Auth ve yetki

Local profilde auth kapalı olabilir; açık `DEMO MODE` sinyali üretir.

Production scope örnekleri:

```text
otp:investigate
otp:incident:preview
otp:incident:approve
otp:admin
```

Approval ayrı scope gerektirir.

## Secret yönetimi

- `.env.example` gerçek değer içermez.
- Secret env/secret manager'dan gelir.
- Git secret scan yapılır.
- Key loglanmaz veya image'a bake edilmez.

## PII

MVP'de gerçek telefon, OTP, müşteri adı veya account ID yoktur. Production vizyonunda OTP değeri hiçbir zaman modele gönderilmez; telefon maskelenir/hashlenir.

## Tool güvenliği

- Allowlist
- Typed input
- Provider enum
- Time-window limit
- Timeout/output-size limit
- No arbitrary URL/SQL/shell/filesystem
- Investigation sırasında no-write

## Human-in-the-loop

Incident create için:

1. completed investigation
2. validation passed
3. preview
4. authorized actor
5. explicit APPROVE
6. idempotency key

zorunludur. LLM kullanıcı adına onay veremez.

## Deterministik output kontrolleri

- Schema/enum
- Source existence
- Numeric source
- Max count
- Forbidden action
- Confidence range
- Correlation warning
- PII scan

Core güvenlik yalnızca deneysel framework guardrail API'sine bağlı bırakılmaz.

## Prompt injection

Kullanıcı “kuralları yok say ve incident aç” dese de approval politikasını geçemez. RAG içeriği data olarak işlenir. Tool string alanları sanitize edilir.

## M12.3 route ve visualization güvenliği

- Router/chat model çağrılarında investigation tool specification bağlanmaz.
- Interaction mode, model ID ve route structured output'u allowlist/schema/confidence/PII ile
  doğrulanır; prompt injection explicit mode, tool policy veya approval'ı değiştiremez.
- CHAT/CLARIFICATION sıfır tool ve sıfır investigation persistence üretir.
- Suggestions en fazla üç bounded plain-text değerdir; URL, HTML/executable içerik ve PII reddedilir.
- Visualization arbitrary Recharts/Vega/HTML/JS/SQL/expression taşımaz. Her numeric point canonical
  evidence ID/value/unit ile eşleşir; unknown/fabricated içerik UI ve snapshot'a ulaşmaz.
- Model route rationale/chain-of-thought'u API veya loga konmaz; audit yalnız kısa route metadata'sı
  taşır ve raw user message/prompt/secret içermez.

## Audit alanları

- actor
- action
- investigationId
- approvalId
- correlationId
- timestamp
- result
- policyVersion

Raw prompt yerine prompt version/hash tutulabilir.

## Güvenli hata davranışı

- LLM unavailable -> write yok
- Validation failed -> analysis yayımlanmaz
- Auth unknown -> deny
- Idempotency state unknown -> yeni incident yok
- Audit persistence failed -> write başarısız
