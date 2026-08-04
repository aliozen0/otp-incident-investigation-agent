# M5 oturum durumu (2026-07-30)

Bu dosya, M5 "Agent orchestration" milestone'ının subagent-driven-development ile yürütülen oturumunun ara durumudur. Session context limiti nedeniyle burada duruldu — devam eden bir Claude Code oturumu bu dosyayı okuyup kaldığı yerden sürdürebilir.

## Ortam

- Çalışma dizini: `.claude/worktrees/milestone+M5-agent-orchestration`
- Branch: `milestone/M5-agent-orchestration` (bu dal AYNI ANDA ana repo klasöründe checkout edilemez — git kısıtı. Ana repo klasörü `main`'de kalmalı, tüm M5 işi bu worktree klasöründen yürütülmeli. VS Code'da görmek için bu worktree klasörünü ayrı aç.)
- Maven komut kalıbı:
  ```bash
  mvn -B test -Dtest=XyzTest
  ```
- Live spike credential'ı yalnız environment üzerinden sağlanmalı; commit veya log içine yazılmamalıdır.

## Süreç

Plan: `docs/superpowers/plans/2026-07-30-m5-agent-orchestration.md` (10 task, superpowers:writing-plans ile yazıldı).
Yürütme yöntemi: superpowers:subagent-driven-development — her task için taze implementer subagent + ayrı reviewer subagent, gerekirse fix-loop.
Ledger (ilerleme kaydı): `.superpowers/sdd/2026-07-30-m5-agent-orchestration/progress.md` (worktree içinde, gitignore'lu — git history asıl kayıt).

## Bitti: Task 1-6 (6/6, hepsi review'dan temiz geçti)

| Task | Commit(ler) | Özet |
|---|---|---|
| 1 | `06afea9` | NVIDIA chat model spike. `NVIDIA_CHAT_MODEL=meta/llama-3.1-8b-instruct` pinlendi (`.env.example`, `docs/19-technology-baseline.md`). `meta/llama-3.3-70b-instruct` NVIDIA endpoint'inde 503 kapasite hatası verdi (tool-calling sorunu değil), 8b model gerçek endpoint'e karşı 2 kez doğrulandı. **Önemli yan etki:** `pom.xml`'e `dev.langchain4j:langchain4j` bağımlılığı eklendi (M4'te sadece embedding kullanıldığı için yoktu; `AiServices` bu artifact'te). LangChain4j 1.18.1 gerçek API şekli sources jar'lardan doğrulandı (`ChatModel.chat(ChatRequest)`, `AiServices.builder(...).chatModel(...)`, vs. — Task 6/7 dispatch'lerinde bu bulgular doğrudan implementer'lara verildi). |
| 2 | `da193ca` | `EvidenceReference`, `KnowledgeReference`, `IncidentAnalysisResult` (structured-output DTO, domain tiplerini reuse ediyor). |
| 3 | `d3e6601` + fix `5c9a793` | `ToolBudgetGuard` (budget/dedup/timeout/retry, plain Java). Review'da Important bulgu çıktı: exception fırlatan çağrılar ledger'a hiç yazılmıyordu → budget'tan muaf kalıyordu. Aynı implementer'a fix-round 1 gönderildi, düzeltildi, re-review temiz. |
| 4 | `c7df9e1` | `EvidenceCollector` (tool sonucu → uygulama üretimli `ev-*` id'li Evidence). Brief'teki `event.changeType()` hatalı çıktı (gerçek alan `event.type()`), implementer kaynağı okuyup düzeltti. |
| 5 | `b054369` | `AgentTools` (5 fixture tool + RAG search → LangChain4j `@Tool`). `createIncidentDraft` bilinçli olarak dahil değil (doğrulandı). Her tool çağrısı `ToolBudgetGuard` + `EvidenceCollector` üzerinden geçiyor, bypass yok (review'da tek tek doğrulandı). |
| 6 | `dece38c` | `StubChatModel` (deterministik sahte `ChatModel`, `StubScript`/`StubScriptStep`). Brief'teki el yapımı JSON serializer'da escape bug'ı vardı (quote/backslash kaçırma yok) — implementer Jackson `ObjectMapper`'a geçti (zaten classpath'te, spring-boot-starter-web'den), gerçek bug'ı düzeltti. |

Her task'ın brief'i: `.superpowers/sdd/2026-07-30-m5-agent-orchestration/task-N-brief.md`
Her task'ın implementer raporu: `.superpowers/sdd/2026-07-30-m5-agent-orchestration/task-N-report.md`

## Deferred (minor, ledger'da kayıtlı, final review'da triage edilecek)

- Task 1: `NvidiaNimChatServiceLiveTest.java` javadoc'u yanlış flag gösteriyor (`-Dgroups=local-live` çalışmıyor, doğrusu `-Dsurefire.excludedGroups=`).
- Task 4: `EvidenceCollector.knownEvidenceIds()` test edilmemiş (trivial delegasyon, düşük risk).
- Task 5: 6 `@Tool` metodundan sadece 2'si (`getOtpMetrics`, `searchIncidentKnowledge`) doğrudan test edilmiş.
- Task 6: yeni escaping testi sadece well-formedness kontrol ediyor, tam round-trip fidelity değil.

## Kaldı: Task 7-10 (henüz implementer dispatch edilmedi)

Sıradaki adım: Task 7'nin brief'i zaten çıkarılmıştı (`.superpowers/sdd/2026-07-30-m5-agent-orchestration/task-7-brief.md`), implementer dispatch edilecekti — devam eden oturum buradan başlayabilir.

- **Task 7** — `IncidentAnalysisAiService` arayüzü (LangChain4j `AiService`, `@SystemMessage`/`@UserMessage`) + `AgentConfig` (Spring `@Configuration`, `AI_MODE` ile stub/live `ChatModel` bean seçimi). BASE: `dece38cccfbfe34a02e2cd61c14fab51c604e534` (Task 6 HEAD'i).
- **Task 8** — `IncidentInvestigationService` (application paketi): `Investigation` lifecycle'ını sürüyor (`receive → startCollectingEvidence → ... → complete/partial/fail`), repair-once (schema/JSON parse hatasında), hallucinated evidence id reddi (negatif test dahil).
- **Task 9** — OTP-DROP-001 uçtan uca kabul testi: beklenen tool sırası (`getOtpMetrics → getErrorDistribution → getQueueHealth → getProviderHealth → getRecentChanges → searchIncidentKnowledge`), max 8 çağrı, `ANOMALY_CONFIRMED`/`HIGH`, connection-pool 1. hipotez, deploy'un korelasyon (nedensellik değil) olarak ifadesi. **Bu milestone'un asıl kabul kriteri.**
- **Task 10** — `mvn spotless:apply` + tam `mvn verify` (tüm proje), live chat spike'ı bir kez daha çalıştırıp kayıt, `docs/17-traceability-risk-dod.md` güncelle, `prompts/handoff/M5-report.md` yaz, `SESSION_LOG.md`'ye satır ekle.

Task 10 bitince final whole-branch review (en yetenekli model) + gerekirse tek fix dalgası + `superpowers:finishing-a-development-branch`.

## Devam etmek için

1. Bu worktree klasöründen çalışmaya devam et (yukarıdaki path).
2. `docs/superpowers/plans/2026-07-30-m5-agent-orchestration.md` Task 7'yi oku.
3. `.superpowers/sdd/2026-07-30-m5-agent-orchestration/task-7-brief.md` zaten hazır.
4. superpowers:subagent-driven-development akışına devam: implementer dispatch → review → ledger güncelle → Task 8/9/10.
