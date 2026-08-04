## Görev: M8 — Demo readiness (son milestone)

Önce: `git checkout main && git pull && git checkout -b milestone/M8-demo-readiness` (bkz. `docs/20-git-workflow.md`).

**Süreç:** M5-M7'deki gibi çalış — `superpowers:writing-plans` ile küçük sıralı task'lara böl, `superpowers:subagent-driven-development` ile her task'ı taze implementer + ayrı reviewer subagent'a yaptır, bitince final whole-branch review yap (M7'de 1 Critical + 4 Important bulgu bulundu, bunu atlama).

Kaynak: `docs/14-implementation-plan.md` → **M8 — Demo readiness**.

İlgili spec dosyaları: `docs/18-demo-interview-guide.md` (30 saniyelik anlatım, demo akışı, zor sorular, gösterilmemesi gerekenler), `docs/17-traceability-risk-dod.md` (MVP release checklist), `README.md` (mevcut proje açıklaması, güncellenmeli).

### Önce oku — M0-M7 zaten ne yaptı

Sistem fonksiyonel olarak tamam: domain (M1), fixture tool'lar (M2), persistence/audit (M3), RAG (M4), agent orchestration (M5), validation/governance (M6), REST/approval (M7) — 137/137 test, hepsi VERIFIED ve main'de. M8 **yeni özellik eklemiyor**, sadece demo'ya hazır hale getiriyor.

Bilinen küçük açık uçlar (raporlarda not düşülmüş, buraya taşınabilir veya bilinçli atlanabilir — triage et):
- M7 raporunda 1 Minor bulgu parkta bırakılmış (internal-only exception mislabeling, client'a görünmüyor) — burada düzeltilebilir veya "bilinen sınırlama" olarak README'ye not düşülebilir.
- `docker compose up --build`, host'ta 5432 portu doluysa çakışıyor (kod sorunu değil) — `.env`'deki `POSTGRES_PORT` zaten çözüm; README'de bunu netleştir.

### Kapsam

1. **README.md güncelle:** Quickstart (`docker compose up --build`), örnek `curl` komutları (investigation POST/GET, preview, approve/reject — `docs/06-api-contracts.md`'deki örneklerden), mimari diyagram (docs/05'teki mermaid diyagramları kopyalanabilir/referans verilebilir), "bu bir mock/PoC" açık uyarısı (docs/18 "Gösterilmemesi gerekenler": şirket içi mimari iddiası yapma).
2. **Swagger/OpenAPI örnekleri:** Spring Boot'ta zaten springdoc/swagger varsa örnek request/response'ları `docs/06`'daki gibi zenginleştir; yoksa `spring-boot-starter` ekleme kararını gerekçelendir (yeni bağımlılık = onay gerektirir, `docs/19` dependency kategorilerine bak).
3. **Seed/demo script:** `OTP-DROP-001` senaryosunu adım adım çalıştıran bir script veya README bölümü (soru gönder → tool trace göster → sonucu göster → preview iste → approve et → aynı key ile tekrar et, duplicate olmadığını göster) — docs/18 "Demo akışı" ile birebir.
4. **Failure demo:** En az bir başarısız senaryo (provider timeout → `PARTIAL_ANALYSIS`, veya prompt injection → sinyal ama policy değişmiyor) da demo script'ine/README'ye eklenebilir (opsiyonel, zaman kalırsa — docs/18 madde 8).
5. **Log temizliği:** Demo sırasında stack trace/debug log/uzun raw prompt görünmesin (docs/18 "Gösterilmemesi gerekenler") — log seviyelerini gözden geçir.
6. **MVP release checklist'i (`docs/17-traceability-risk-dod.md`) tek tek geç:** her satırı gerçekten çalıştırıp işaretle, iddia etme.
7. **Opsiyonel minimal UI** (docs/02 "yalnızca zaman kalırsa") — zaman yoksa atla, atlandığını raporla.

Henüz **yazma**: yeni domain/business kuralı, yeni tool, yeni endpoint. Bu milestone yalnızca var olanı görünür/sunulabilir/temiz hale getiriyor.

Bu milestone'un "Kabul" kriteri: temiz bilgisayarda tek komut, 5-7 dakikalık demo (`docs/14` M8 kabul cümlesi).

### Sırayla

1. `docs/17-traceability-risk-dod.md` MVP release checklist'ini oku, her maddeyi sırayla ele al.
2. Değişecek dosyaları listele (README.md, varsa `scripts/demo.sh` veya benzeri, Swagger config).
3. Her adım için: yap, gerçekten çalıştırıp doğrula (özellikle temiz ortamda `docker compose up --build`).
4. Refactor/temizlik.

### Kısıtlar

- Kod davranışını değiştirme (bug düzeltmesi hariç, gerçek bir şey bulursan `05-bugfix.md` şablonundaki gibi kök nedene git, ayrı raporla).
- Yeni bağımlılık eklemeden önce gerekçelendir (`docs/00-project-charter.md` "Kapsam değişikliği filtresi": ana senaryoyu güçlendiriyor mu, ölçülebiliyor mu, demo güvenilirliğini bozmuyor mu, gereksiz altyapı eklemiyor mu, teknik görüşmede açıklanabilir mi).
- Tüm `mvn`/`docker` komutları repository kökünden.
- Commit'ten önce `mvn spotless:apply` + tam `mvn verify` yeşil olduğunu doğrula.
- Şirket içi mimari varmış gibi anlatma; her yerde "bu bir mock/PoC" açık kalsın (docs/18).

### Bitti sayılması için

- `docs/17-traceability-risk-dod.md` MVP release checklist'inin tamamı gerçekten çalıştırılıp işaretlendi (iddia değil).
- Temiz bir ortamda (yeni `docker compose down -v` sonrası) `docker compose up --build` tek komutla çalışıyor, health UP.
- README quickstart + curl örnekleri gerçekten çalıştırılıp doğrulandı.
- `mvn verify` (tüm proje) BUILD SUCCESS.

### Oturum sonu

`prompts/08-session-report.md` kuralına göre `prompts/handoff/M8-report.md` dosyasını yaz ve `SESSION_LOG.md`'ye satır ekle. Branch adı `milestone/M8-demo-readiness`, commit convention `docs/20-git-workflow.md`. **Kendi işini kendin "VERIFIED" yazma** — DONE yaz, ayrı/bağımsız bir oturum doğrulayacak. Bu proje planındaki **son milestone** — bitince tüm M0-M8 tamamlanmış olacak.
