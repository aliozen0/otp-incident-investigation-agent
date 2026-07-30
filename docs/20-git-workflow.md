# 20 — Git Workflow

Bu proje çoklu, izole Claude Code oturumlarıyla ilerliyor (`prompts/` altındaki şablonlar). Her oturum bu kurala uyar.

## Branch stratejisi

- `main`: her zaman yeşil (`mvn verify` geçer). Doğrudan push yok.
- Her milestone/task kendi branch'inde: `milestone/{ID}-{kisa-slug}`, örn. `milestone/M0-bootstrap`, `milestone/M2-fixture-tools`.
- Bugfix: `fix/{kisa-slug}`. ADR/spec çalışması: `docs/{kisa-slug}`.
- Bir branch = bir `prompts/01-07` görevi. Görev bitmeden branch birleştirilmez.

## Commit convention

Conventional Commits:

```text
{tip}({kapsam}): {kısa özet}

{gerekirse gövde: neden, hangi AC/FR karşılandı}
```

Tipler: `feat`, `fix`, `docs`, `test`, `chore`, `refactor`.

Kapsam örnekleri: `bootstrap`, `domain`, `tools`, `rag`, `agent`, `api`, `validation`.

Örnek:

```text
feat(bootstrap): Spring Boot iskeleti ve Docker Compose ekle

M0 kabul kriteri: mvn verify + docker compose up sonrası health UP.
```

Kurallar:

- Bir commit bir mantıksal değişiklik; TDD adımlarını (failing test / impl / refactor) ayrı commit'lere bölmek serbest ama karışık commit yasak.
- Commit mesajında API key, secret, gerçek telefon/OTP değeri olamaz (bkz. `docs/09-security-governance.md`).
- `Co-Authored-By` satırı varsa dokunma.

## Merge kuralı

- Bir branch `main`'e merge olmadan önce ilgili görev `prompts/09-verify-session.md` ile **VERIFIED** olmalı.
- REJECTED branch merge edilmez; düzeltme aynı branch'te devam eder veya yeni scoped task branch'i açılır.
- Merge sonrası branch silinir.
- Fast-forward mümkün değilse `--no-ff` merge veya PR kullan (rebase ile geçmiş bozma).

## Ne asla yapılmaz

- `git push --force` main'e.
- `--no-verify` ile hook atlama.
- `git commit --amend` zaten push edilmiş commit'te.
- Testsiz/rapor'suz merge (bkz. `prompts/08-session-report.md`, `SESSION_LOG.md`).
