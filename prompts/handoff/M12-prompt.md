## Görev: M12 — Agent console frontend (ChatGPT/Claude tarzı sohbet arayüzü, Türkçe, grafikli)

Önce: `git checkout main && git pull && git checkout -b milestone/M12-agent-console-frontend` (bkz. `docs/20-git-workflow.md`).

**Süreç:** M5-M11'deki gibi çalış — `superpowers:writing-plans` ile küçük sıralı task'lara böl, `superpowers:subagent-driven-development` ile her task'ı taze implementer + ayrı reviewer subagent'a yaptır, final whole-branch review yap. **Görsel tasarım için `frontend-design` skill'ini, grafikler için `dataviz` skill'ini kullan** — ikisi de zorunlu.

### Bağlam

M11'de backend'e eklendi: session-scoped chat memory (`sessionId` ile takip soruları hatırlanır — `docs/16-adr.md` ADR-017), model seçimi (`GET /api/v1/models`, doğrulanmış 2 model), hızlı/detaylı mod (`mode: quick|thorough`), belge yükleme (`POST`/`GET /api/v1/knowledge/documents`), session/thread listesi (`GET /api/v1/sessions/{sessionId}/investigations`). Bu milestone bunların hepsini gerçek bir web arayüzüne bağlıyor.

M10'un ürettiği tek-sayfa form→sonuç arayüzü kullanıcı tarafından reddedildi: "bir LLM ile konuşuyor gibi değilim" — proje temelde bir LLM agent + RAG projesi, arayüz bunu gizlememeli, ChatGPT/Claude tarzı bir sohbet uygulaması gibi hissettirmeli.

### Önce oku

- `prompts/handoff/M11-report.md` — yeni endpoint'lerin tam listesi ve davranışı.
- `frontend/src/App.tsx`, `frontend/src/api/types.ts`, `frontend/src/api/client.ts` (M10'da yazıldı) — mevcut yapı, genişletilecek.
- `frontend/src/index.css` — mevcut tasarım token'ları (Tailwind v4 `@theme`, açık/beyaz palet), korunacak.
- Backend: `src/main/java/com/example/otpsentinel/api/SessionController.java`, `ModelsController.java`, `KnowledgeController.java`, `InvestigationController.java` (yeni `sessionId`/`modelId`/`mode` alanları).

### Kapsam

1. **Layout**: sol sidebar (session/thread listesi — `GET /sessions/{id}/investigations` ile doldurulur, "yeni sohbet" butonu yeni bir `sessionId` = `crypto.randomUUID()` üretir), orta panel (sohbet akışı), üst navbar (ayarlar linki).
2. **Sohbet akışı**: mesaj kutusu (textarea, Enter ile gönder). Her mesaj `POST /investigations` çağrısı, aynı `sessionId` ile (aynı thread'de takip soruları backend'deki chat memory sayesinde bağlamı korur — bunu gerçekten test et: bir soru sor, "peki ya provider B?" gibi bir takip sorusu sor, cevabın öncekini referans aldığını doğrula). Kullanıcı mesajı balonu + agent cevap balonu (agent balonunun içinde M10'daki zengin sonuç kartı: evidence/hipotez/actions/knowledge + grafikler). Yükleniyor durumu balon içinde (typing-indicator tarzı).
3. **Ayarlar paneli** (navbar'dan): model seçici (`GET /models`, seçilen `modelId` sonraki `POST /investigations` çağrılarına eklenir), hızlı/detaylı mod toggle (`mode` alanı), RAG bilgisi + belge listesi (`GET /knowledge/documents`) + belge yükleme formu (başlık/tip/içerik → `POST /knowledge/documents`).
4. **Grafikler** (`dataviz` skill ile — form seç, renk paletini mevcut `--color-*` token'larından türet, `node scripts/validate_palette.js` ile doğrula, ince mark/hover tooltip): hipotez olasılık sıralaması (ranked bar — `Hypothesis.probability` zaten sayısal), confidence (kompakt gauge/stat-tile), knowledge similarity (küçük skor çubuğu). Ham-enum `summary` alanını render etme (M10'da bilinen sınırlama) — yerine status+severity+ilk hipotezden sentezlenen tek satır Türkçe özet koy.
5. **Türkçe lokalizasyon**: tek dosyalık sözlük (`frontend/src/lib/labels.ts`) — backend enum'ları (status/severity/sourceType/actionType/error code'lar) → Türkçe etiket, tüm statik UI metni, `tr-TR` locale ile sayı/tarih formatlama. i18n kütüphanesi ekleme.
6. **Zaman aralığı**: native `<input type="datetime-local">`, mevcut checkbox+çıplak-ISO-metin deseni kaldırılır.
7. **Belge yükleme sonucunu doğrula**: yükledikten sonra RAG'in yeni belgeyi gerçekten kullandığını göster (ör. yeni belgeyle ilgili bir soru sor, `knowledgeReferences`'ta göründüğünü kontrol et).

### Kısıtlar

- Koyu tema yok.
- Yeni container/servis yok — Dockerfile'daki frontend build stage aynı kalır.
- Chart kütüphanesi (Recharts önerilir, M10/önceki plan notlarına bak) hariç yeni ağır bağımlılık yok.
- Backend'e dokunma (M11 zaten hazır) — yalnızca frontend.
- Commit'ten önce `mvn spotless:apply`+`mvn verify` (backend, dokunulmadıysa değişmemeli) ve `npm run build`/`npm run test` (frontend) yeşil olduğunu doğrula.

### Bitti sayılması için

- `docker compose up --build` sonrası gerçek Chrome tarayıcısında (`mcp__claude-in-chrome__*`) tam akış Türkçe: yeni sohbet başlat → soru sor → grafikli sonuç → **takip sorusu sor, memory çalıştığını doğrula** → model değiştir → hızlı/detaylı mod dene → belge yükle → RAG'in yeni belgeyi kullandığını gör. Konsol hatasız.
- `node scripts/validate_palette.js` grafik renkleri için PASS.
- Backend `mvn verify` değişmemiş/yeşil. Frontend `npm run build` + `npm run test` yeşil.
- Tasarım/grafik kararları (`frontend-design`+`dataviz` kullanıldığına dair) raporda kısaca açıklanmış.

### Oturum sonu

`prompts/08-session-report.md` kuralına göre `prompts/handoff/M12-report.md` yaz, `SESSION_LOG.md`'ye satır ekle. Branch `milestone/M12-agent-console-frontend`. **Kendi işini VERIFIED yazma** — DONE, bağımsız oturum doğrulayacak (ekran görüntüsü paylaşırsan doğrulama daha hızlı olur).
