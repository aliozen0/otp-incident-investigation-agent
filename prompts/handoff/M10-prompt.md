## Görev: M10 — Frontend (portföy demo UI)

Önce: `git checkout main && git pull && git checkout -b milestone/M10-frontend` (bkz. `docs/20-git-workflow.md`).

**Süreç:** M5-M9'daki gibi çalış — `superpowers:writing-plans` ile küçük sıralı task'lara böl, `superpowers:subagent-driven-development` ile her task'ı taze implementer + ayrı reviewer subagent'a yaptır, final whole-branch review yap. **Görsel tasarım için `frontend-design` skill'ini kullan** — bu milestone'un en kritik kısmı bu, atlanmaz.

Kaynak: `docs/16-adr.md` ADR-016 (bu milestone'un gerekçesi ve tech stack kararı).

### Bağlam

M9'da kanıtlandı: `AI_MODE=live` ile gerçek NVIDIA chat model + gerçek pgvector RAG + gerçek agentic tool-calling uçtan uca çalışıyor (bir koşuda tam `ANOMALY_CONFIRMED`/`HIGH`, audit_event trace'i ile kanıtlı). Şimdi bu gerçek API'nin üzerine, kullanıcının iş görüşmelerinde/portföyünde göstereceği bir web UI kuruluyor.

Kullanıcının talepleri (birebir):
- Gerçek uçtan uca çalışsın — sahte/mockup ekran değil, gerçek REST API'ye bağlı.
- Açık/beyaz tema. **Koyu tema yok.**
- "Yapay zeka ile üretilmiş şablon" hissi vermeyecek — sıradan olmayan, kasıtlı tasarım kararları olan bir arayüz.
- Az ama gerçekçi mock veri (zaten var: `OTP-DROP-001` fixture) — çok fazla veri gerekmiyor, sistemin gerçekten kurulu ve çalışır göründüğü hissi önemli.
- Uzman/işe alan biri baktığında "iyi yapılmış" desin — portföy kalitesinde.

### Tech stack (ADR-016'da kararlaştırıldı)

- **React + TypeScript + Vite + Tailwind CSS.**
- Backend'in REST API'sini doğrudan tüketir (`docs/06-api-contracts.md`).
- Build çıktısı Dockerfile'a yeni bir stage olarak eklenir, Spring Boot'un `src/main/resources/static/` dizinine kopyalanır — ayrı container/servis yok, `docker compose up --build` tek komut kuralı bozulmaz.
- Geliştirme sırasında Vite dev server (`localhost:5173`) kullanılabilir — M9'da bunun için `DevCorsConfig` (`@Profile("dev")`) zaten hazır.

### Kapsam

1. **Proje iskeleti:** `frontend/` dizininde Vite + React + TS + Tailwind kurulumu. Dockerfile'a build stage ekle (`npm ci && npm run build` → çıktıyı Spring Boot static'e kopyala).
2. **Tasarım — önce `frontend-design` skill'ini invoke et.** Rastgele bir component kütüphanesi/şablonu (ör. varsayılan shadcn görünümü, generic admin dashboard teması) kullanma; skill'in yönlendirdiği gibi kasıtlı tipografi, renk paleti (beyaz/açık zemin, sınırlı ve amaçlı vurgu rengi), spacing kararları ver.
3. **Ana akış (tek sayfa veya birkaç adımlı akış, docs/12 Gherkin senaryolarını birebir yansıtacak şekilde):**
   - Soru formu: `question` (textarea) + opsiyonel `timeWindow`. Örnek/varsayılan olarak `docs/15`'teki OTP-DROP-001 sorusu önerilebilir (kullanıcı değiştirebilir).
   - Gönderince: yükleniyor durumu (gerçek API çağrısı `AI_MODE=live` iken saniyeler sürebilir — M9 raporunda gözlemlendi, bunu UI'da gerçekçi şekilde göster, sahte anlık sonuç değil).
   - Sonuç ekranı: `status`/`severity` rozet, `summary`, `evidence` listesi (her biri `sourceReference` ile — kanıt-kaynak ilişkisini görsel olarak vurgula, bu projenin temel değer önerisi), `hypotheses` (rank + probability + supporting evidence), `recommendedActions` (`requiresApproval` açıkça görünür), `knowledgeReferences` (RAG citation'ları), `confidence`, `validation.warnings` (varsa, ör. correlation-language uyarısı).
   - Preview butonu → `incident-draft/preview` çağır, taslağı göster (henüz kayıt yok vurgusu).
   - Approve/Reject butonları → `incident-draft/decisions`, idempotency key'i UI otomatik üretsin (`crypto.randomUUID()`). Approve sonrası incident ID görünsün. Aynı investigation'ı tekrar approve etmeye çalışınca `idempotentReplay=true` durumunu görünür şekilde göster (bu sistemin somut bir güvenlik özelliği, gizlenmemeli).
   - GET ile daha önceki bir investigation'ı tekrar getirme (opsiyonel, zaman kalırsa).
4. **Hata durumları:** API'nin problem-details hatalarını (`docs/06`) kullanıcıya anlaşılır şekilde göster (ör. `PARTIAL_ANALYSIS`, `FAILED`, timeout) — M9'un canlı koşularında bunlar gerçekten oluştu, UI bunları "hata" değil sistemin dürüst davranışı olarak sunmalı (ör. "Bazı kanıtlar toplanamadı" gibi).
5. **"Bu bir mock/PoC" açıklaması** UI'da bir yerde (footer/about) görünür olsun (docs/18 "Gösterilmemesi gerekenler" — şirket içi mimari iddiası yapılmaz).

Henüz **yazma**: backend değişikliği (gerekmedikçe — CORS zaten M9'da hazır), yeni domain/business kuralı.

### Sırayla

1. `frontend-design` skill'ini oku ve tasarım kararlarını (renk, tipografi, layout) buna göre ver.
2. Vite+React+TS+Tailwind iskeleti kur, Dockerfile'a stage ekle, `docker compose up --build` ile static dosyaların Spring Boot'tan servis edildiğini doğrula.
3. API client (TypeScript, `docs/06`'daki tiplerle uyumlu) yaz.
4. Soru formu → sonuç ekranı akışını kur, gerçek API'ye (stub modda) karşı test et.
5. Preview/approve/reject/idempotency akışını ekle.
6. `AI_MODE=live` ile gerçek bir uçtan uca deneme yap (M9'daki gibi, gerçek key ile), UI'ın gerçek yükleme süresini/hata durumlarını doğru yansıttığını doğrula.
7. Final whole-branch review.

### Kısıtlar

- Koyu tema yok, yalnızca açık/beyaz.
- Yeni backend container/servis ekleme — frontend statik dosya olarak aynı image'a gömülür.
- Ana `mvn verify` test suite'i etkilenmemeli (frontend kendi test/lint sürecine sahip olabilir, ayrı).
- Gerçek `NVIDIA_API_KEY` gerektiren adımlar otomatik CI/test'e girmez (M4/M5/M9 emsali, `@Tag("local-live")` benzeri ayrım frontend tarafında da geçerli — elle doğrulama).
- Commit'ten önce `mvn spotless:apply` + tam `mvn verify` (backend) yeşil olduğunu doğrula; frontend için `npm run build`'in hatasız geçtiğini doğrula.

### Bitti sayılması için

- `docker compose up --build` sonrası `http://localhost:8080/` gerçek UI'ı gösteriyor (Swagger değil).
- Stub modda tam akış (soru → sonuç → preview → approve → idempotent replay) elle denenip çalıştığı gösterilmiş (ekran görüntüsü/adım adım açıklama raporda).
- En az bir kez `AI_MODE=live` ile gerçek uçtan uca deneme yapılmış, UI'ın gerçek API davranışını (yükleme süresi, olası PARTIAL/FAILED durumları) doğru yansıttığı gösterilmiş.
- `mvn verify` (backend) hâlâ BUILD SUCCESS.
- Tasarım kararları (`frontend-design` skill kullanıldığına dair) raporda kısaca açıklanmış.

### Oturum sonu

`prompts/08-session-report.md` kuralına göre `prompts/handoff/M10-report.md` yaz, `SESSION_LOG.md`'ye satır ekle. Branch `milestone/M10-frontend`. **Kendi işini VERIFIED yazma** — DONE, bağımsız oturum doğrulayacak (ekran görüntüsü de paylaşırsan doğrulama daha hızlı olur).
