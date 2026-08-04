## Görev: M5 — Agent orchestration

Önce: `git checkout main && git pull && git checkout -b milestone/M5-agent-orchestration` (bkz. `docs/20-git-workflow.md`).

Kaynak: `docs/14-implementation-plan.md` → **M5 — Agent orchestration**.

İlgili spec dosyaları: `docs/07-agent-tool-spec.md` (agent kuralları, tool catalog, `ToolResult` envelope, evidence mapping, structured result, validation pipeline özeti, failure behavior tablosu, "chat memory yok"), `docs/05-domain-and-architecture.md` (agentic/deterministik sınır, resilience: tool timeout 2s/1 retry, model timeout 20s, total deadline 30s, schema repair 1), `docs/13-test-strategy.md` (deterministic stub model yaklaşımı), `docs/16-adr.md` ADR-015 (NVIDIA NIM), `docs/19-technology-baseline.md` (`AI_MODE`, `AI_MAX_TOOL_CALLS`, `AI_MAX_REPAIR_ATTEMPTS`, `AI_TIMEOUT_SECONDS`).

### Kapsam

Yalnızca şunu yap:

1. **Chat model seçimi (küçük spike, M4'teki embedding spike gibi):** `NVIDIA_CHAT_MODEL` için NVIDIA katalogundan tool-calling destekleyen bir model seç (ör. bir Llama 3.1/3.3 Instruct ailesi — NIM'de tool/function calling destekleyip desteklemediğini gerçek bir çağrı ile doğrula), `.env`/`.env.example`/`docs/19`'a pinle. LangChain4j `OpenAiChatModel` ile NVIDIA `baseUrl` üzerinden bağlan (ADR-015).
2. **Deterministik stub model** (`docs/13-test-strategy.md`, ADR-011, `AI_MODE=stub`): CI/ana test suite'in gerçek LLM'e ihtiyaç duymaması için, fixture'a göre beklenen tool çağrılarını ve sonucu üreten sahte bir model implementasyonu. Bu, M0-M4 boyunca kurulan her şeyi (fixture tool'lar, RAG) gerçekten uçtan uca (agent aracılığıyla) tetikleyen ilk milestone.
3. LangChain4j `AiService`/tool-calling konfigürasyonu: M2'deki 5 fixture tool'u (`getOtpMetrics, getErrorDistribution, getQueueHealth, getProviderHealth, getRecentChanges`) ve M4'teki `searchIncidentKnowledge`'ı (T-006) `@Tool` olarak bağla. `createIncidentDraft` (T-007) **agent'ın tool setine dahil etme** — docs/07: "Normal agent tool setine açık değildir."
4. Tool budget/allowlist deterministik Java kodda uygulanmalı (docs/05 "agentic/deterministik sınır"): max 8 çağrı (`AI_MAX_TOOL_CALLS`), aynı tool+aynı parametre başarılı çağrı tekrarlanmaz, transient hata için 1 retry, tool timeout 2s. Bunu framework guardrail'ine bırakma — ayrı bir `ToolBudgetGuard`/benzeri sınıf yaz, test et.
5. Evidence mapping: `docs/07` "Evidence ID'leri model değil uygulama üretir" — tool sonucu → uygulama tarafından üretilen `ev-*` id'li `Evidence` (M1 domain tipi) dönüşümü uygulama kodunda olsun, modelin evidence id uydurmasına izin verme.
6. Structured output: `IncidentAnalysisResult` şemasına (docs/07) uyan, LangChain4j'nin structured-output desteğiyle üretilmiş sonuç; JSON parse/schema hatasında **1 repair** denemesi, ikinci hatada `FAILED` (AC-022).
7. Agent'ın ürettiği hipotez/evidence'ı M1'deki `Investigation` aggregate'ine (`proposeAnalysis` vb. metodlarla) map eden bir application/agent servisi (henüz REST'e bağlama, M7).
8. Ana fixture (`OTP-DROP-001`) uçtan uca stub ile test edilsin: beklenen tool sırası (docs/07 "Beklenen ana çağrı akışı"), max 8 çağrı sınırı, `ANOMALY_CONFIRMED`/`HIGH`/connection-pool-birinci-hipotez sonucu.

Henüz **yazma**: numeric claim validator/forbidden-action validator gibi tam governance pipeline (M6 — burada sadece tool budget ve repair-once var, AC-023/M6'nın derin validation'ı değil), REST endpoint (M7).

Bu milestone'un "Kabul" kriteri: ana fixture beklenen tool'ları kullanır ve max 8 çağrı (`docs/14` M5 kabul cümlesi).

### Sırayla

1. İlgili requirement/acceptance criterion'ı belirle (AC-001/002/003/004/005/006/007/010/011/016/017/022, FR-005/006/007/009/011/012, AI-001/002/006/007/008).
2. Değişecek dosyaları listele (`agent` paketi: `AiService` arayüzü, tool binding sınıfı, `ToolBudgetGuard`, stub model, `agent` config; `application` paketinde investigation orchestration servisi).
3. Önce failing test yaz: `OTP-DROP-001` stub ile uçtan uca ("stub model + gerçek fixture tool'lar + gerçek RAG (M4, deterministic embedding testte olduğu gibi) → doğru status/severity/hypothesis"), tool budget aşımı testi, aynı tool+parametre tekrar testi, repair-once testi.
4. Minimum implementasyonla geçir (chat-model spike'tan sonra, ama spike ana test suite'i bloklamasın — M4'teki gibi `local-live` ayrı grup).
5. Refactor.
6. `docs/19`'a seçilen `NVIDIA_CHAT_MODEL`'i ve gerekçesini yaz.

### Kısıtlar

- Ana test suite `NVIDIA_API_KEY` olmadan geçmeli — gerçek chat model çağrısı gerektiren test `@Tag("local-live")` ile ayrılsın (M4 emsali).
- Agent LLM'e write yetkisi verme: `createIncidentDraft` tool set'inde olmayacak (docs/07 T-007 notu, ADR-009/ADR-010).
- Tool budget/allowlist/timeout deterministik kodda, framework'e bırakılmaz (docs/09 "Core güvenlik yalnızca deneysel framework guardrail API'sine bağlı bırakılmaz").
- Evidence id'leri modelin değil uygulamanın ürettiğinden emin ol; buna dair en az bir negatif test yaz (model farklı bir id uydurursa reddedilsin/yok sayılsın).
- Kalıcı chat memory ekleme (ADR-012) — her investigation izole.
- `domain`/`tools`/`rag` paketlerine agent-özel kod sızdırma; `agent` paketi kendi sınırında kalsın.
- Bu milestone dışına taşma: tam validation pipeline (M6), REST (M7).
- Tüm `mvn`/`docker` komutları repository kökünden.
- Commit'ten önce `mvn spotless:apply` + tam `mvn verify` (yalnız yeni paket değil) yeşil olduğunu doğrula.

### Bitti sayılması için

- `OTP-DROP-001` stub ile uçtan uca test geçiyor: doğru tool sırası, max 8 çağrı, `ANOMALY_CONFIRMED`/`HIGH`, connection-pool ilk hipotez, queue ilk hipotez değil, deploy'un korelasyon (nedensellik değil) olarak ifade edilmesi.
- Tool budget/duplicate-call/repair-once/timeout senaryoları ayrı testlerle kanıtlı.
- Ana test suite internet/gerçek NVIDIA key olmadan yeşil; `local-live` chat model testi ayrıca gerçek key ile bir kez çalıştırılıp raporlanmış.
- `mvn verify` (tüm proje) BUILD SUCCESS.
- `docs/17-traceability-risk-dod.md` DoD listesine uyuyor.

### Oturum sonu

`prompts/08-session-report.md` kuralına göre `prompts/handoff/M5-report.md` dosyasını yaz ve `SESSION_LOG.md`'ye satır ekle. Branch adı `milestone/M5-agent-orchestration`, commit convention `docs/20-git-workflow.md`.
