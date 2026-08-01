# M10 — Frontend (portföy demo UI)

## Durum
DONE

## Kapsam

`superpowers:writing-plans` ile 7 task'lık plan yazıldı (`docs/superpowers/plans/2026-08-01-m10-frontend.md`), `frontend-design` skill'i ile önce tasarım kararları (renk paleti, tipografi, "citation threading" imza elementi) sabitlendi, ardından `superpowers:subagent-driven-development` ile her task taze implementer + ayrı reviewer subagent çiftine yaptırıldı. React + TypeScript + Vite + Tailwind v4 tek sayfa uygulaması `frontend/` altında kuruldu; Dockerfile'a yeni bir build stage eklendi (`node:20-alpine` → `npm run build` → çıktı `src/main/resources/static/`'e kopyalanıyor), ayrı container/servis eklenmedi. Uygulama gerçek REST API'ye (`docs/06-api-contracts.md`) bağlı: soru formu → yükleniyor durumu → sonuç ekranı (status/severity rozet, evidence ledger + hipotezler arasında mono chip ile "citation threading", recommended actions, knowledge references, confidence, validation warnings) → preview → approve/reject → idempotent replay göstergesi. Hata durumları (problem-details) kullanıcı dostu, "sistem dürüst davranıyor" çerçevesinde mesajlara çevriliyor. Footer'da mock/PoC açıklaması var.

6 kodlanabilir task + final whole-branch review tamamlandı. Final review (opus, en yetenekli model) 6 Important bulgu buldu; 5'i tek fix dalgasıyla düzeltildi (KnowledgeReference tipinin gerçek backend şeklini yansıtması, `validation`/dizi alanlarının API sınırında normalize edilmesi — `client.ts`'te tek merkezi normalizer, per-component patch değil —, `toUserMessage`'ın asla `undefined` dönmemesi, 2 eksik hata kodu eşlemesi, scaffold README'nin değiştirilmesi), 1'i (backend `summary` alanının enum adını olduğu gibi döndürmesi) frozen backend kapsamı dışında olduğu için rapor edildi, düzeltilmedi. Scoped re-review temiz. Ayrıca Task 7'nin manuel doğrulaması sırasında **gerçek, önceden bilinmeyen bir bug** bulundu ve düzeltildi: gerçek tarayıcıda (Chrome, `docker compose up --build`, stub mod) form gönderilince sayfa tamamen boşaldı — konsol hatası `Cannot read properties of undefined (reading 'toFixed')`. Kök neden: backend'in `KnowledgeReferenceDto`'su yalnızca `documentId` dolduruyor (`version`/`chunkId`/`title`/`similarityScore` hiç gönderilmiyor), ama `KnowledgeReferences.tsx` bu alanların hep dolu olduğunu varsayıyordu. Savunmacı render ile düzeltildi (commit `5784988`), tekrar gerçek tarayıcıda doğrulandı.

İki bulgu insan hakemliğine sunuldu (plan-mandated / brief-internal çelişki), ikisi de "olduğu gibi kalsın" kararıyla parklandı: (1) `QuestionForm.tsx`'teki `bg-white/40`/`text-white` sabit token listesinde yok ama marka paleti ihlali değil; (2) `IncidentDecisionPanel`'in `approvalRequired`/`status` prop'ları brief'in Files bölümünde istenmiş ama brief'in kendi Step 2 kod örneği bunları içermiyor, implementer kod örneğini birebir takip etti, işlevsel eksiklik yok.

Docker bu oturumun asıl ortamında (Windows Bash tool) yoktu; WSL2'de hem Docker hem `sdkman` (Java 21/Maven) mevcut olduğu keşfedildi ve **tüm Task 7 doğrulaması gerçekten çalıştırıldı** (subagent iddiası değil, kontrolör tarafından bizzat): backend `mvn verify`, frontend `npm run build`/`npm run test`, stub mod tam uçtan uca akış (gerçek Chrome tarayıcısı, `mcp__claude-in-chrome__*` araçlarıyla — form → sonuç → preview → approve → idempotent replay → hata yolu), ve **gerçek `AI_MODE=live` ile gerçek `NVIDIA_API_KEY` kullanılarak tam bir canlı koşu** (submit → ~40s gerçek bekleme → gerçek model sonucu → preview → approve → gerçek incident oluşturma), audit_event trace'i ile kanıtlı.

## Değişen dosyalar
- `frontend/` (yeni, 38 dosya) — Vite+React+TS+Tailwind v4 projesi:
  - `src/api/types.ts`, `client.ts` — `docs/06`'daki tiplerle birebir uyumlu TS tipleri + tipli API client (`createInvestigation`, `getInvestigation`, `previewIncidentDraft`, `submitIncidentDecision`), `ApiError`, `normalizeInvestigation` (API sınırında `validation`/dizi alanlarını null-safe hale getiren merkezi normalizer).
  - `src/lib/idempotency.ts` — `generateIdempotencyKey()` (`crypto.randomUUID()`).
  - `src/lib/errors.ts` — problem-details → kullanıcı mesajı eşlemesi, tüm dallarda garantili non-empty string.
  - `src/components/` — `Header`, `Footer` (mock/PoC açıklaması), `QuestionForm`, `LoadingState`, `StatusBadge`, `EvidenceLedger`, `HypothesisList`, `ActionsList`, `KnowledgeReferences`, `ResultCard`, `ErrorPanel`, `IncidentDecisionPanel`.
  - `src/App.tsx` — `idle → loading → result → error` state machine.
  - `src/index.css` — sabit tasarım token'ları (`@theme`, Tailwind v4 CSS-first), `@fontsource` ile self-hosted Space Grotesk/IBM Plex Sans/IBM Plex Mono.
  - `README.md` — proje-özel (scaffold boilerplate değil).
  - `*.test.ts` (3 dosya, 11 test) — API client, idempotency, error mapping için Vitest.
- `Dockerfile` — `frontend-build` stage eklendi (`node:20-alpine`), çıktı Maven stage'inde `src/main/resources/static`'e kopyalanıyor.
- `.dockerignore` (yeni) — `frontend/node_modules`, `frontend/dist`, `target`, `.git`.
- `docs/superpowers/plans/2026-08-01-m10-frontend.md` — plan.

## Testler (gerçek komut + gerçek çıktı, iddia değil)

Frontend build:
Komut: `cd frontend && npm run build`
```
✓ built in 391ms
```
0 TypeScript hatası.

Frontend testler:
Komut: `cd frontend && npm run test`
```
 Test Files  3 passed (3)
      Tests  11 passed (11)
```

Backend (WSL2, sdkman ile gerçek Java 21/Maven — Windows ortamında `mvn` yoktu, bu yüzden WSL kullanıldı, `wslpath` ile aynı repoya `/mnt/c/...` üzerinden erişildi):
Komut: `source ~/.sdkman/bin/sdkman-init.sh && mvn -B spotless:apply && mvn -B verify -Dsurefire.excludedGroups=local-live`
```
[INFO] Tests run: 148, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- spotless:2.43.0:check (spotless-check) @ otp-sentinel ---
[INFO] Spotless.Java is keeping 170 files clean
[INFO] BUILD SUCCESS
```
M9 baseline ile aynı (148 test) — backend'e dokunulmadı, beklenen.

Stub mod uçtan uca (gerçek Chrome tarayıcı, `docker compose up --build`, WSL2 Docker):
- `curl http://localhost:8080/` → gerçek UI HTML (Swagger değil), `<title>OTP Sentinel</title>`.
- Gerçek tarayıcıda form gönderildi → **bug bulundu** (`KnowledgeReferences.tsx` çöküyordu, konsol: `TypeError: Cannot read properties of undefined (reading 'toFixed')`) → düzeltildi (commit `5784988`) → container yeniden build edildi → tekrar denendi: sonuç tam render oldu (status/severity/evidence/hipotez chip eşleşmesi/actions/knowledge references), konsol hatasız (`mcp__claude-in-chrome__read_console_messages` ile doğrulandı).
- Preview → "no incident exists yet" mesajı doğru göründü.
- Approve → `Incident DEMO-INC-8E50907B created`, `incidentDraftId` göründü.
- Aynı idempotency key ile "Resubmit" → `Idempotent replay — the same decision was already recorded, no duplicate was created.` banner'ı göründü, aynı incident ID korundu.
- `startAt > endAt` ile submit → `The investigation could not be started / That time window is not valid...` (raw JSON değil, mapped mesaj) doğru göründü.

Final-review fix dalgası sonrası tekrar doğrulama: container yeniden build edildi, form tekrar gönderildi, konsol hatasız, sonuç doğru render oldu (`index-BMz1Q1mn.js` — yeni bundle hash, eski cache'lenmiş hata mesajları `ctrl+shift+r` ile temizlendi ve gerçek hatasız durum doğrulandı).

**Canlı mod (`AI_MODE=live`, gerçek `NVIDIA_API_KEY`, `.env`'de zaten mevcuttu):**
- `AI_MODE=live docker compose up --build -d` → `{"status":"UP"}`, startup hatasız.
- Gerçek tarayıcıda form gönderildi, loading state ~40 saniye gerçek bekleme sonrası gerçek model sonucu döndü (sahte/anlık değil).
- Sonuç: `investigationId=99fe5eff-27e9-4dc7-b59c-8bc3e73fee8a`, `status=ANOMALY_CONFIRMED`, `severity=MEDIUM`, `confidence=0.8` — stub'dan farklı (HIGH/0.85), gerçek model stokastikliğini gösteriyor (M9'daki gözlemle tutarlı). 2 hipotez ("Provider timeout" 0.7, "Rate limited" 0.2), knowledge references boş ("No similar historical incidents were found" doğru render oldu), konsol hatasız.
- Preview → Approve → `Incident DEMO-INC-F33C3642 created` — gerçek canlı koşuda incident oluşturma çalıştı.
- Gerçek `audit_event` trace'i (psql, WSL2 Docker DB container'ından):
  ```
  REQUEST_ACCEPTED, TIME_WINDOW_RESOLVED,
  TOOL_CALLED/TOOL_COMPLETED getOtpMetrics,
  TOOL_CALLED/TOOL_COMPLETED getErrorDistribution,
  TOOL_CALLED/TOOL_COMPLETED getQueueHealth,
  TOOL_CALLED/TOOL_FAILED getProviderHealth ("No fixture data for provider default" — model yanlış provider adı geçti, sistem TOOL_FAILED olarak düzgün işledi, UI'da bu etkilenmedi çünkü diğer evidence'lar yeterliydi),
  TOOL_CALLED/TOOL_COMPLETED getRecentChanges (ids=[]),
  RAG_COMPLETED (results=0),
  LLM_COMPLETED, VALIDATION_PASSED,
  PREVIEW_GENERATED,
  APPROVAL_DECIDED (APPROVE),
  INCIDENT_CREATED (DEMO-INC-F33C3642)
  ```
  UI'daki tıklama sırasıyla (submit → preview → approve) birebir eşleşiyor.

## Karşılanan requirement/AC
- Gerçek uçtan uca (sahte/mockup değil) — hem stub hem `AI_MODE=live` ile gerçek Chrome tarayıcısında kanıtlandı, yukarıdaki testler.
- Açık/beyaz tema, koyu tema yok — `frontend/src/index.css`'te `prefers-color-scheme`/dark varyant yok, final review'da doğrulandı (grep).
- "Yapay zeka şablonu" hissi vermeme — `frontend-design` skill'i ile kasıtlı token sistemi (Space Grotesk/IBM Plex Sans/IBM Plex Mono, özel renk paleti), "citation threading" imza elementi (evidence ↔ hipotez arasında birebir eşleşen mono chip'ler) final review'da doğrulandı.
- Az/gerçekçi mock veri — `OTP-DROP-001` fixture, `docs/15` ile birebir.
- Ana akış (docs/12 Gherkin) — soru formu, yükleniyor, sonuç ekranı (tüm alanlar), preview, approve/reject, idempotent replay — hepsi hem stub hem canlı modda elle doğrulandı.
- Hata durumları — problem-details → dürüst, "sistem davranışı" çerçeveli mesajlar (`docs/06`'daki 6 hata kodu + backend'in gerçekte döndürdüğü 2 ek kod, final review'da bulundu ve eklendi).
- Mock/PoC açıklaması — footer'da her zaman görünür (`Footer.tsx`).
- Docker tek komut kuralı bozulmadı — yeni container yok, `docker compose up --build` tek komutla hem stub hem canlı modda (env var ile) çalıştı.
- Backend'e dokunulmadı (CORS zaten M9'da hazırdı) — `mvn verify` 148/148, M9 baseline ile aynı.

## Karşılanmayan / ertelenen
- GET ile önceki investigation'ı tekrar getirme — plan'da açıkça opsiyonel ("zaman kalırsa") olarak işaretlenmişti, `getInvestigation` client fonksiyonu yazıldı ama UI'da hiçbir yerden çağrılmıyor (final review'da "untested dead code" olarak not edildi, blocking değil).
- Backend `KnowledgeReferenceDto`'nun yalnızca `documentId` doldurması (`docs/06`'daki tam şekil — `version`/`chunkId`/`title`/`similarityScore` — hiçbir modda gönderilmiyor) — frontend tipi gerçeğe göre düzeltildi (opsiyonel alanlar), backend DTO'su bu oturumun frozen kapsamı dışında bırakıldı, düzeltilmedi.
- Backend `summary` alanının (hem `Investigation.summary` hem `IncidentDraftPreview.summary`) çoğu durumda enum adını olduğu gibi döndürmesi (ör. "ANOMALY_CONFIRMED") gerçek düzyazı yerine — final review'da bulundu, frontend `summary`'yi olduğu gibi render ediyor (plan'ın kuralı gereği doğru davranış), ama backend verisi zayıf; demo kalitesini doğrudan etkiliyor.
- Final review'un Minor listesi (EvidenceChip tekrarının tek component'e çıkarılması, kullanılmayan `icons.svg`/`favicon.svg`, a11y `role="status"`/`role="alert"` eksikliği, canlı modda olası duplicate React key riski, resubmit sırasında görsel "in-progress" metni eksikliği) — ledger'da deferred olarak kayıtlı, blocking değil.

## Spec çelişkisi/belirsizlik (varsa)
- **Backend/spec ayrışması (yeni keşif, bu oturumda bulundu):** `docs/06-api-contracts.md`'nin `knowledgeReferences` örneği tüm alanları (`version`/`chunkId`/`title`/`similarityScore`) dolu gösteriyor, ama gerçek backend implementasyonu (`InvestigationResponseDto.KnowledgeReferenceDto`) yalnızca `documentId` dolduruyor. Bu M10'un frozen backend kapsamı dışında — frontend gerçeğe uyum sağladı, backend/doc'un kendisi ayrı bir oturumda ele alınmalı.
- Task 3 review'da bulunan `bg-white/40`/`text-white` (design token listesi dışı) ve Task 6 review'da bulunan `IncidentDecisionPanel` prop uyuşmazlığı (brief'in Files bölümü vs Step 2 kod örneği) — ikisi de kullanıcıya soruldu, "olduğu gibi kalsın" kararı alındı, plan/ledger'da kayıtlı (`.superpowers/sdd/2026-08-01-m10-frontend/progress.md`).

## Sonraki oturum için not
M10 bağımsız doğrulama için hazır. Kendi işim **VERIFIED değil DONE** — bağımsız oturum doğrulayacak. Branch: `milestone/M10-frontend`. Doğrulayacak oturum için ipucu: bu oturumda Windows ortamında `docker`/`mvn` yoktu ama WSL2'de ikisi de (sdkman ile Java 21) mevcuttu — `wslpath` ile aynı repoya `/mnt/c/Users/Ali/Downloads/otp-incident-agent` üzerinden erişilebilir, aynı yöntem doğrulama için kullanılabilir. `AI_MODE=live` testi için `.env`'de gerçek `NVIDIA_API_KEY` zaten mevcut. Backend `KnowledgeReferenceDto`/`summary` alanı zayıflığı (yukarıda not edildi) ayrı bir M11 kapsamı olabilir — kullanıcıya sorulmalı.
