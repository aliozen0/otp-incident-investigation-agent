# Prompt Templates

Bu klasör, `AGENTS.md` ve `docs/` spec'ine bağlı kalarak Claude Code'a görev vermek için hazır şablonlar içerir.

Kullanım: ilgili şablonu kopyala, `{...}` alanlarını doldur, oturuma yapıştır.

Her şablon zaten şunu varsayar (tekrar yazma):

- `AGENTS.md` okunmuş,
- ilgili `docs/NN-*.md` dosyası source of truth,
- `Specification -> Failing Test -> Minimal Implementation -> Refactor` sırası,
- tek scoped task, ilgisiz iyileştirme yok.

## Şablonlar

| Dosya | Ne zaman kullanılır |
|---|---|
| `01-milestone-task.md` | M0-M8 planındaki bir adımı başlatmak |
| `02-new-tool.md` | Yeni bir agent tool'u (port+adapter) eklemek |
| `03-new-endpoint.md` | Yeni/değişen REST endpoint |
| `04-validation-rule.md` | Yeni claim/policy validation kuralı |
| `05-bugfix.md` | Acceptance criterion'a uymayan davranışı düzeltmek |
| `06-adr.md` | Yeni mimari karar önerisi |
| `07-spec-conflict.md` | Kod yazmadan önce spec çelişkisi/belirsizliği raporlamak |
| `08-session-report.md` | Her çalışma oturumu SONUNDA zorunlu — handoff raporu yazma kuralı |
| `09-verify-session.md` | Ayrı bir oturumda önceki oturumun raporunu doğrulamak (test tekrar çalıştırılır) |

Sıra önerisi: `14-implementation-plan.md`'deki M0 → M8 sırasını izle; her milestone içinde `01-milestone-task.md`'yi tekrar tekrar kullan.

## Çok oturumlu akış (context şişmesin diye)

Context ayrı ayrı oturumlarda dolduğu için her oturum bağımsız ve kendi kendine yeter olmalı:

1. **Çalışma oturumu**: `01-07` şablonlarından biriyle görev ver → sonunda `08-session-report.md` kuralına göre rapor yazması ve `SESSION_LOG.md`'ye satır eklemesi zorunlu.
2. **Doğrulama oturumu**: yeni, temiz bir oturumda `09-verify-session.md` ile önceki raporu doğrulat. Bu oturum tüm spec'i değil, sadece `SESSION_LOG.md` + ilgili `prompts/handoff/{ID}-report.md` + ilgili AC'yi okur.
3. VERIFIED ise sıradaki milestone/task'a geç; REJECTED ise düzeltme için ayrı scoped task aç.

`SESSION_LOG.md` ve `prompts/handoff/*.md` bu zincirin tek hafızasıdır — yeni oturum açarken tüm konuşmayı tekrar anlatmana gerek yok, bu iki yeri okumak yeterli.
