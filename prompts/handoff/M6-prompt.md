## Görev: M6 — Validation/governance

Önce: `git checkout main && git pull && git checkout -b milestone/M6-validation-governance` (bkz. `docs/20-git-workflow.md`).

Kaynak: `docs/14-implementation-plan.md` → **M6 — Validation/governance**.

İlgili spec dosyaları: `docs/07-agent-tool-spec.md` ("Validation pipeline" — 9 adım listesi), `docs/09-security-governance.md` ("Deterministik output kontrolleri", PII/prompt injection), `docs/11-acceptance-criteria.md` (AC-012, AC-013, AC-021, AC-022, AC-023, AC-028), `docs/12-atdd-gherkin.md` ("Evidence validation" ve "Prompt injection" feature'ları).

### Önce oku — M5'te zaten yapılmış olanı tekrar yazma

M5 (`IncidentInvestigationService`, bkz. `prompts/handoff/M5-report.md`) şunları **zaten** yaptı:

- JSON parse/schema hatasında 1 repair, ikinci hatada `FAILED` (AC-022) — mevcut.
- Model'in uydurduğu evidence id'sini reddetme (`citesUnknownEvidence`, hem result hem hypothesis seviyesinde) — mevcut.

Bunları tekrar yazma; üzerine inşa et veya gerekirse `ValidationReport`'a (M1 domain tipi) daha zengin bir sonuç (hangi kural, hangi kod) döndürecek şekilde küçük bir refactor yap.

### Kapsam

M6'da eksik olan validation adımlarını ekle:

1. **Numeric claim validator (AC-023):** Sonuçtaki/hipotezdeki her sayısal iddianın (`metricValue` taşıyan `Evidence`'lar dışında, örn. summary/hypothesis metninde geçen yüzde/sayı) gerçekten toplanan tool evidence'ından geldiğini doğrula. Tool'da olmayan bir sayı iddia edilirse `UNSUPPORTED_NUMERIC_CLAIM` ile validation failure üret (docs/12 "Reject unsupported numeric claim" senaryosu).
2. **Forbidden action validator (AC-012):** `RecommendedAction` listesinde `actionType` `RESTART`/`ROLLBACK`/`CONFIG_CHANGE` olup `requiresApproval=false`/`executionMode` otomatik yürütmeyi ima ediyorsa reddet, `FORBIDDEN_AUTOMATIC_ACTION` (docs/12 "Reject automatic rollback" senaryosu). Bu M1'deki domain invariant'ıyla (high-risk aksiyon otomatik değil) örtüşüyorsa, iki katmanı da (domain invariant + bu validator) birbirini tekrar etmeden konumlandır — validator agent çıktısını erken reddetsin, domain invariant son güvenlik ağı olarak kalsın.
3. **Correlation wording check (AC-007/AC-021 ile ilişkili):** Deploy/config değişikliği ile olay arasındaki ilişki anlatılırken "kesin nedensellik" ifade eden kalıpları (`"neden oldu"`, `"caused"`, `"is the root cause"` gibi, dil-agnostik bir liste) tespit edip reddet/uyar. M5'in E2E testi bunu stub script'in kelime seçimiyle "doğal" sağlıyordu (anti-assert) — burada bunu **deterministik bir kural** haline getir, canlı model farklı kelime seçse bile yakalansın.
4. **PII scan (docs/09, AC-028):** Sonuç/summary/audit içinde OTP değeri, telefon numarası benzeri kalıp (basit regex, aşırı mühendislik yapma), API key/secret kalıbı geçerse reddet veya redakte et; en azından test edilebilir bir tarama fonksiyonu olsun.
5. **Prompt injection sinyali (docs/12 "Ignore embedded instruction" senaryosu):** M4'teki `ContentSanitizer` zaten instruction-pattern sinyali üretiyordu ama audit'e yazılmıyordu (M4 raporunda bilinçli ertelenmişti). Burada: retrieved knowledge'da böyle bir sinyal varsa, M3'teki `AuditEventRepository`'ye bir sinyal event'i yaz (yeni bir `AuditEventType` değeri eklemek gerekebilir — FR-017'nin sabit event listesini genişletmek küçük bir spec sapmasıdır, raporla) ve investigation sonucunun/approval akışının bundan etkilenmediğini (politika değişmediğini) testle kanıtla.
6. Tüm bu kuralları tek bir `ClaimValidator`/`AnalysisValidator` gibi bir sınıfta veya `docs/07`'deki 9 adımlık pipeline'a uyacak sırayla topla; `ValidationReport`'a (PASSED/FAILED + warning listesi + hangi kuralın tetiklendiği) yansıt.

Henüz **yazma**: REST endpoint (M7).

Bu milestone'un "Kabul" kriteri: güvenlik acceptance testleri (docs/12 "Evidence validation" ve "Prompt injection" feature'larının tamamı) geçer (`docs/14` M6 kabul cümlesi).

### Sırayla

1. İlgili requirement/acceptance criterion'ı belirle (AC-012, AC-013, AC-021, AC-023, AC-028; AI-002 uncertainty language, AI-004/AI-005 retrieved content isolation/prompt injection).
2. Değişecek dosyaları listele (muhtemelen yeni bir `application`/`agent` alt-paketi veya mevcut `IncidentInvestigationService`'e ek sınıflar; `AuditEventType` genişletmesi gerekirse M3 dosyaları).
3. Önce failing test yaz: docs/12'deki 5 "Evidence validation" senaryosu + "Prompt injection" senaryosu, birebir.
4. Minimum implementasyonla geçir.
5. Refactor.
6. Gerekirse `docs/17-traceability-risk-dod.md`'yi güncelle.

### Kısıtlar

- Bu kurallar deterministik Java kodda yaşar, LLM/framework guardrail'ine bırakılmaz (docs/09).
- M1 domain invariant'larını (özellikle high-risk aksiyon otomatik değil, evidence-hypothesis ilişkisi) bozma; validator bunların üzerine ek bir erken-ret katmanı, yerine geçen bir şey değil.
- Bu milestone dışına taşma: REST, persistence şema değişikliği (audit event tipi genişletmesi hariç, o M3'ün küçük bir uzantısı).
- Tüm `mvn`/`docker` komutları WSL2 üzerinden.
- Commit'ten önce `mvn spotless:apply` + tam `mvn verify` (tüm proje) yeşil olduğunu doğrula.

### Bitti sayılması için

- docs/12'deki "Evidence validation" (5 senaryo) ve "Prompt injection" (1 senaryo) feature'larının tamamı testli ve geçiyor.
- `mvn verify` (tüm proje) BUILD SUCCESS.
- `docs/17-traceability-risk-dod.md` DoD listesine uyuyor.

### Oturum sonu

`prompts/08-session-report.md` kuralına göre `prompts/handoff/M6-report.md` dosyasını yaz ve `SESSION_LOG.md`'ye satır ekle. Branch adı `milestone/M6-validation-governance`, commit convention `docs/20-git-workflow.md`. **Kendi işini kendin "VERIFIED" yazma** — DONE yaz, ayrı/bağımsız bir oturum doğrulayacak (bkz. M5'teki süreç ihlali ve düzeltmesi).
