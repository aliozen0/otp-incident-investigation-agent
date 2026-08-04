## Görev: M5 — Agent orchestration'a devam (Task 7-10)

Bu, önceki bir oturumun context limiti nedeniyle yarım bıraktığı işin devamı. Task 1-6 bitti ve bağımsız doğrulandı (91/91 test, `mvn verify` BUILD SUCCESS, commit `0beaf3d`). Senin işin Task 7, 8, 9, 10'u tamamlamak.

### Ortam — dikkatli oku

- Bu iş **ayrı bir git worktree'de** yürütülüyor, ana repo klasöründe değil:
  `.claude/worktrees/milestone+M5-agent-orchestration`
  Branch: `milestone/M5-agent-orchestration`.
- **Bu worktree klasöründen çalış.** Repository kökü `main` branch'inde kalmalı — aynı branch iki yerde checkout edilemez (git kısıtı), ana klasöre dokunma.
- Komutları worktree kökünden çalıştır. Komut kalıbı:
  ```bash
  mvn -B verify -Dsurefire.excludedGroups=local-live
  ```
- Task 7-10'da canlı model credential'ına ihtiyaç **yok** — ana kabul testi (Task 9) deterministik `StubChatModel` kullanıyor.

### Önce oku (sırayla)

1. `prompts/handoff/M5-session-status.md` — önceki oturumun tam durumu, ne yapıldı, neden.
2. `docs/superpowers/plans/2026-07-30-m5-agent-orchestration.md` — 10 task'lık planın tamamı (Task 7-10 detaylı tarif burada).
3. `.superpowers/sdd/2026-07-30-m5-agent-orchestration/task-7-brief.md` — Task 7 için brief zaten yazılmış, hazır.
4. `AGENTS.md`, `docs/07-agent-tool-spec.md`, `docs/05-domain-and-architecture.md` — proje kuralları/source of truth.
5. `docs/20-git-workflow.md`, `prompts/08-session-report.md` — commit convention ve oturum sonu rapor formatı.

### Yapılacaklar (özet — tam detay plan dosyasında)

- **Task 7** — `IncidentAnalysisAiService` (LangChain4j `AiService`, `@SystemMessage`/`@UserMessage`) + `AgentConfig` (`AI_MODE` ile stub/live `ChatModel` bean seçimi). Base commit: `dece38c`.
- **Task 8** — `IncidentInvestigationService`: `Investigation` lifecycle'ını sürüyor (`receive → collecting → ... → complete/partial/fail`), JSON/schema hatasında 1 repair, sonra `FAILED`; model'in uydurduğu evidence id'sini reddet (negatif test şart).
- **Task 9 — asıl kabul kriteri.** `OTP-DROP-001` uçtan uca test: beklenen tool sırası (`getOtpMetrics → getErrorDistribution → getQueueHealth → getProviderHealth → getRecentChanges → searchIncidentKnowledge`), max 8 çağrı, sonuç `ANOMALY_CONFIRMED`/`HIGH`, connection-pool ilk hipotez, queue ilk hipotez değil, deploy'un nedensellik değil korelasyon olarak ifade edilmesi.
- **Task 10** — `mvn spotless:apply` + tam `mvn verify` (tüm proje, yalnız yeni paket değil), live chat spike'ı bir kez daha çalıştırıp kaydet, `docs/17-traceability-risk-dod.md` güncelle, `prompts/handoff/M5-report.md` yaz (`08-session-report.md` şablonuyla), `SESSION_LOG.md`'ye satır ekle.

### Süreç kuralı

Her task için: önce failing test yaz, minimum implementasyonla geçir, refactor, **kendi işini eleştirel gözden geçir** (Task 3/4/6'da önceki oturum tam da bunu yaparak gerçek bug'lar buldu — budget-bypass, yanlış alan adı, JSON escape hatası), sonra ayrı commit at (`docs/20-git-workflow.md` convention, `feat(agent): ...`). Bir task bitmeden diğerine geçme. Task 1-6'nın dosyalarına dokunma (gerçek bir bug bulmadıkça).

### Kısıtlar

- Agent'a `createIncidentDraft` tool'unu **verme** — o ayrı, onay gerektiren bir akış (M7).
- Kalıcı chat memory ekleme, her investigation izole kalsın.
- Ana test suite `NVIDIA_API_KEY` olmadan yeşil kalmalı.
- Bu milestone dışına taşma: tam validation pipeline (M6), REST endpoint (M7).

### Bitti sayılması için

- Task 9 (ana kabul testi) geçiyor.
- Tüm proje `mvn verify` BUILD SUCCESS (yalnızca yeni testler değil — önceki oturumlarda bu atlanıp yanlış "preexisting failure" sonucuna varılmıştı, tekrarlama).
- `prompts/handoff/M5-report.md` yazıldı, `SESSION_LOG.md` güncellendi.
- Commit'ler `milestone/M5-agent-orchestration` branch'inde, worktree'den push edilebilir durumda.
