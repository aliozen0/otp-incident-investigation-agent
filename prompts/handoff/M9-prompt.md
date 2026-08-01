## Görev: M9 — Canlı mod uçtan uca kanıtlama (demo altyapısı)

Önce: `git checkout main && git pull && git checkout -b milestone/M9-live-demo-mode` (bkz. `docs/20-git-workflow.md`).

**Süreç:** M5-M8'deki gibi çalış — `superpowers:writing-plans` ile küçük sıralı task'lara böl, `superpowers:subagent-driven-development` ile her task'ı taze implementer + ayrı reviewer subagent'a yaptır, final whole-branch review yap.

Kaynak: `docs/16-adr.md` ADR-016 (yeni — bu milestone'un gerekçesi burada).

### Bağlam

Kullanıcı bu projeyi portföy/iş görüşmesi demosu olarak kullanacak. Şu ana kadar (M0-M8) `AI_MODE=live` yalnızca **izole spike'larla** doğrulandı (M4: tek embed çağrısı, M5: tek chat çağrısı). Gerçek bir investigation'ın **uçtan uca canlı modda** (gerçek NVIDIA chat model + gerçek pgvector RAG + gerçek agentic tool-calling, scripted stub değil) çalıştığı hiç kanıtlanmadı. M10'da yazılacak UI'ın "sahte demo" değil gerçek bir sistem gösterdiğine güvenmek için önce bu kanıtlanmalı.

### Kapsam

1. **Knowledge ingestion'ı canlı moda bağla:** M4'teki `KnowledgeIngestionService` + `NvidiaNimEmbeddingService` gerçek NVIDIA embedding ile çalışıyor (compatibility spike'ta doğrulanmıştı) ama demo akışında (`docker compose up` sonrası) `docs/15-demo-fixtures.md`'deki 4 belgenin gerçekten ingest edilip edilmediği belirsiz. Uygulama başlangıcında (`AI_MODE=live` iken) knowledge fixture'larının otomatik ingest edildiğini bir `ApplicationRunner`/init mekanizmasıyla sağla (idempotent — tekrar başlatmada duplicate yazmasın).
2. **Gerçek uçtan uca canlı investigation'ı bir kez çalıştır ve kanıtla:** `AI_MODE=live`, gerçek `NVIDIA_API_KEY` ile, `POST /api/v1/investigations` çağrısının gerçekten NVIDIA modeline gidip gerçek tool-calling yaptığını (log/trace ile), gerçek pgvector'dan RAG sonucu döndürdüğünü, ve makul bir `IncidentAnalysisResult` ürettiğini göster. Bu **otomatik test değil**, canlı bir doğrulama koşusu (M4/M5'teki `local-live` tag'li testler gibi, `@Tag("local-live")`) — gerçek çıktıyı raporda göster.
3. **Agent'ın canlı modda gerçekten doğru tool sırasını takip edip mantıklı bir sonuç ürettiğini değerlendir.** Scripted stub'daki gibi birebir aynı olması gerekmez (gerçek model), ama: max 8 çağrı sınırına uyuyor mu, connection-pool/provider teması hipotez olarak çıkıyor mu, evidence id'leri uygulama tarafından mı üretiliyor (model uydurmuyor mu), forbidden action reddi çalışıyor mu? Sorunluysa (model tool seçmiyor, saçma JSON dönüyor, vs.) kök nedeni bul ve düzelt — sistem promptu/tool açıklamaları yetersiz olabilir, `IncidentAnalysisAiService`'in `@SystemMessage`'ını iyileştir.
4. **Demo-mode config'i netleştir:** `.env.example`'a `AI_MODE=live` ile çalıştırmak için gereken tüm değişkenlerin (chat model, embedding model, base url) dolu/doğru olduğunu doğrula; README'ye "canlı demo nasıl çalıştırılır" bölümü ekle (gerçek key gerektiğini, stub'dan farkını açıkla).
5. **CORS:** M10'da eklenecek frontend aynı origin'den (Spring Boot static resources içine gömülü) servis edilecek (ADR-016) — bu nedenle CORS config'e muhtemelen gerek yok, ama frontend geliştirme sırasında (Vite dev server, farklı port) ihtiyaç olacaksa yalnızca `local`/`dev` profilinde dar bir CORS izni ekle (production/demo image'ında gerekmez çünkü aynı origin).

Henüz **yazma**: frontend kodu (M10), yeni domain/business kuralı.

### Sırayla

1. Mevcut live-mode wiring'i oku (`AgentConfig`, `KnowledgeIngestionService`, M4/M5 raporları).
2. Knowledge auto-ingest mekanizmasını yaz, test et (idempotent — iki kez başlatmada duplicate yok).
3. Gerçek `NVIDIA_API_KEY` ile bir kez uçtan uca canlı investigation çalıştır, çıktıyı kaydet.
4. Bulunan sorunları (varsa) düzelt, tekrar dene.
5. README + `.env.example` güncelle.
6. `mvn spotless:apply` + tam `mvn verify` (stub/offline path hâlâ yeşil kalmalı — bunu bozma).

### Kısıtlar

- Ana test suite (`mvn verify`, CI) hâlâ `NVIDIA_API_KEY` olmadan yeşil kalmalı (NFR-004) — canlı doğrulama ayrı, otomatik olmayan bir adım.
- LLM'e hâlâ write yetkisi yok (`createIncidentDraft` tool set'inde değil).
- Yeni altyapı/container ekleme (CORS için Spring config yeterli, ayrı bir API gateway vb. gerekmez).
- Tüm `mvn`/`docker` komutları WSL2 üzerinden.

### Bitti sayılması için

- Knowledge auto-ingest testli ve idempotent.
- Gerçek canlı uçtan uca investigation çalıştırılmış, çıktı raporda gösterilmiş, mantıklı (bkz. madde 3 kriterleri).
- `mvn verify` (offline/stub path) hâlâ BUILD SUCCESS.
- README canlı demo talimatı içeriyor.

### Oturum sonu

`prompts/08-session-report.md` kuralına göre `prompts/handoff/M9-report.md` yaz, `SESSION_LOG.md`'ye satır ekle. Branch `milestone/M9-live-demo-mode`. **Kendi işini VERIFIED yazma** — DONE, bağımsız oturum doğrulayacak. Bitince M10 (frontend) promptu verilecek.
