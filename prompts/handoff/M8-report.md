# M8 — Demo readiness

## Durum
DONE

## Kapsam
M8 planının (`docs/superpowers/plans/2026-08-01-m8-demo-readiness.md`) 7 task'ını uyguladım: M7
final review'da parkedilen exception-mislabeling düzeltmesi (Task 1), Swagger/OpenAPI örnekleri
(Task 2), log temizliği (Task 3), README quickstart/mimari diyagram/curl walkthrough/mock
disclaimer (Task 4), `scripts/demo.sh` (Task 5), Task 4 sırasında canlı `docker compose` ile
bulunan ikinci bir gerçek bug'ın (`StubChatModel` tek instance, container ömrü boyunca ikinci
investigation'da script tükeniyordu) düzeltmesi, ve bu görev: Task 7 — tüm MVP release checklist'ini
gerçekten çalıştırıp `docs/17`'yi kanıtla işaretlemek. Bu oturumda (Task 7) HEAD `85ac762` idi;
checklist doğrulaması `c6cefbc` commit'inde. Kendi işim VERIFIED değil DONE — bağımsız oturum
doğrulayacak.

## Değişen dosyalar (bu oturum — Task 7)
- `docs/17-traceability-risk-dod.md` — MVP release checklist'in 14/15 kutusu `[x]` yapıldı (kanıtla),
  "5–7 dakika demo" kutusu insan-anlatımlı zamanlama yapılmadığı için açık bırakıldı ve gerekçesi
  eklendi; yeni `### M8 status` bölümü eklendi (gerçek komut çıktılarının özeti).

(Önceki task'ların dosya değişiklikleri için: `git log --oneline main..milestone/M8-demo-readiness`
— README.md, pom.xml, scripts/demo.sh, docs/17, ve agent/api/config paketindeki bugfix'ler.)

## Testler (gerçek komut + gerçek çıktı)

### Adım 1 — Full clean build
Komut: `mvn -B spotless:apply verify`

Çıktı özeti:
```
[INFO] Results:
[INFO]
[INFO] Tests run: 141, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] --- spring-boot:3.3.5:repackage (repackage) @ otp-sentinel ---
[INFO] --- spotless:2.43.0:check (spotless-check) @ otp-sentinel ---
[INFO] Spotless.Java is keeping 166 files clean - 0 needs changes to be clean, 0 were already clean, 166 were skipped because caching determined they were already clean
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  44.512 s
```
141 = beklenen sayı (137 `main`'de mevcut + bu branch'teki 4 yeni test: exception-mislabeling
regression'ından 3 test + `secondInvestigationOnTheSameOrchestratorInstanceStillSucceeds`
stub-script-exhaustion bugfix testinden 1 test — `prompts/handoff/M8-bugfix-stub-script-exhaustion.md`).

### Adım 2 — Clean-environment compose up
Komut: `docker compose down -v` — çıktı: `Volume otp-incident-agent_pgdata Removed`.

Komut: `docker compose up --build -d` — image cache'ten build edildi, `db` ve `app` container'ları
oluşturuldu ve başlatıldı; `db` sağlık kontrolünden `Healthy` geçti, `app` `Started`.

Poll (`docker compose ps`), yaklaşık 26 saniye sonra:
```
NAME                       IMAGE                    ...  STATUS
otp-incident-agent-app-1   otp-incident-agent-app   ...  Up 20 seconds (healthy)
otp-incident-agent-db-1    pgvector/pgvector:pg16   ...  Up 26 seconds (healthy)
```

Health check: `curl -s http://localhost:8080/actuator/health` -> `{"status":"UP"}`.

Host port 5432 boştu; `POSTGRES_PORT` override'ına gerek olmadı.

### Adım 3 — `scripts/demo.sh`, zamanlı
Komut: `time ./scripts/demo.sh`
```
real    0m0.446s
user    0m0.141s
sys     0m0.032s
```
Akış: investigation `ANOMALY_CONFIRMED`/`HIGH` ile oluşturuldu, GET ile aynı sonuç persisted olarak
geldi, preview `requiresExplicitApproval=true` döndürdü, decisions APPROVE ile
`incidentDraftId=0de1f2c8-a807-42b0-b7c2-d87f37780d18`, `externalIncidentId=DEMO-INC-4A8472AF`,
`idempotentReplay=false` oluşturuldu; aynı Idempotency-Key ile tekrar POST edildiğinde
**aynı** `incidentDraftId`/`externalIncidentId` ve `idempotentReplay=true` döndü. Script çalışma
süresi (<1 saniye) "5-7 dakikalık demo" gereksinimindeki sunucu-anlatımı süresinin çok altında —
`docs/18-demo-interview-guide.md`'deki anlatım metniyle doldurulacak kısım.

### Adım 4 — Secret scan
Komut: `git log -p milestone/M8-demo-readiness ^main | grep -iE "nvapi-|api[_-]?key.*=|password.*="`
-> çıktı yok, `|| echo clean` fallback'i tetiklendi: `clean`.

Komut: `grep -rn "nvapi-" . --include=*.md --include=*.yml --include=*.java --include=*.env* | grep -v node_modules`
-> yalnızca bu görevin brief/plan dosyalarında (`.superpowers/sdd/.../task-7-brief.md`,
`docs/superpowers/plans/2026-08-01-m8-demo-readiness.md`) *literal string* `"nvapi-"` dokümantasyon
metni olarak geçiyor (bu komutun kendisini anlatan cümlelerde) — gerçek bir key değil.

Komut: `git ls-files .env` -> boş çıktı (untracked, doğrulandı). `.env` gitignore'da ve repo'ya hiç
commit'lenmemiş; gerçek NVIDIA API key'i içeriyor ama bu raporda veya herhangi bir komut çıktısında
basılmadı.

### Adım 5 — README quickstart / API walkthrough, verbatim tekrar
`docker compose up --build` (Adım 2'de yapıldı), `curl -s http://localhost:8080/actuator/health`
-> `{"status":"UP"}`, Swagger UI: `curl -o /dev/null -w '%{http_code}' http://localhost:8080/swagger-ui/index.html`
-> `200`.

5 adımlık curl walkthrough'u (`scripts/demo.sh`'tan bağımsız, README'deki komutları harfiyen
kopyalayarak) ikinci bir defa canlı container'a karşı çalıştırdım:
```
INV_ID=b585797a-2965-4bd3-8e9c-b46102a5ed03
"ANOMALY_CONFIRMED"
true
{"incidentDraftId":"90f18247-c090-47fc-b3e4-11437ff2ea39","externalIncidentId":"DEMO-INC-3FC39740","status":"CREATED","idempotentReplay":false}
{"incidentDraftId":"90f18247-c090-47fc-b3e4-11437ff2ea39","externalIncidentId":"DEMO-INC-3FC39740","status":"CREATED","idempotentReplay":true}
```
Tüm komutlar dokümante edilenle birebir aynı, ekstra flag gerekmedi. Bu, M8'in ikinci bugfix'inin
(`chatModelFactory` — stub script artık her investigation için sıfırlanıyor) kalıcı çalıştığını da
teyit ediyor: aynı container üzerinde ikinci kez `POST /investigations` çağrıldığında hâlâ
`ANOMALY_CONFIRMED` dönüyor, ilk bugfix öncesindeki gibi `FAILED` değil.

## Karşılanan requirement/AC
- `docs/17` MVP release checklist — 16 kutunun 15'i kanıtla işaretlendi (`c6cefbc`).
- US-011/012/013 (preview/approval/idempotency) — Adım 3 ve Adım 5'te canlı ortamda tekrar kanıtlandı.
- R-09 (secret loglama) — Adım 4 secret scan'i çalıştırıldı, temiz.
- R-13 (mock algısı) — README "Bu bir mock/PoC'tur" bölümü canlı ortamda mevcut ve tutarlı.

## Karşılanmayan / ertelenen
- **"5–7 dakika demo" kutusu açık bırakıldı.** Script'in kendisi <1 saniyede koşuyor (gereksinim
  "5-7 dakika" *sunucu anlatımı* dahil, script'in kendisinin dakikalarca sürmesi değil —
  `docs/18-demo-interview-guide.md`), ama bu oturumda bir insan anlatıcıyla gerçek zamanlı
  5-7 dakikalık bir prova yapılmadı; bu nedenle kutuyu kanıtsız işaretlemedim
  (`AGENTS.md`: kanıtsız tik yasak).
- **Opsiyonel "failure demo" (`DEMO_FIXTURE=OTP-PARTIAL-001`/`OTP-INJECTION-001` ile stub uçtan uca
  gösterim) bilinçli olarak atlandı.** Kök neden: `AgentConfig.chatModelFactory` stub modda her
  zaman `OtpDropOneOhOneScript.build()`'e sabit; `DEMO_FIXTURE` sadece tool fixture verisini
  değiştiriyor, stub script'i değiştirmiyor. İkinci bir stub script eklemek M8 kapsamının dışında
  yeni özellik işi olurdu (M8 planı `docs/superpowers/plans/2026-08-01-m8-demo-readiness.md:27`'de
  bunu M8-prompt'un kendi "opsiyonel... atlandığını raporla" talimatına göre bilinçli skip olarak
  kayıtlı). Bu negatif senaryolar sadece `AI_MODE=live` ile gerçek bir modelle gösterilebilir.
- **Opsiyonel UI atlandı** — M8 kapsamı REST API + curl/Swagger ile sınırlı, ayrı bir web UI
  planlanmadı/yapılmadı (README ve API walkthrough demo arayüzü olarak kullanılıyor).

## Whole-branch review (main..milestone/M8-demo-readiness)
`git log --oneline main..milestone/M8-demo-readiness` (Task 7 commit'i hariç 10 commit) ve
`git diff --stat main..milestone/M8-demo-readiness` ile tüm diff'i (README.md, docs/17, pom.xml,
2 bugfix handoff dosyası, scripts/demo.sh, ve agent/api/config paketindeki 8 kaynak dosya) tek
oturumda okudum. Bulgular:
- İki gerçek davranış değişikliği var, ikisi de ayrı handoff raporlarıyla belgelenmiş ve testli:
  (1) `IllegalArgumentException` -> `IllegalStateException` (2 throw site, M7-parked finding,
  `fb96e38`), (2) `ChatModel` singleton -> `Supplier<ChatModel>` factory (stub script exhaustion
  fix, `a23af20`) — her ikisi de kapsamı net, testleri var, API sözleşmesini değiştirmiyor.
  Bunların dışında davranış değişikliği yok (dokümantasyon + Swagger annotation + log level +
  yeni demo script).
  Bu, M7 final review'unun bulduğu "1 Critical + 4 Important" ölçeğinde bir bulgu **bulunmadı**;
  M8'in kapsamı zaten dar (dokümantasyon/demo-readiness), yeni REST/domain davranışı eklenmedi.
- `pom.xml`'e eklenen tek yeni dependency (`springdoc-openapi-starter-webmvc-ui:2.6.0`) sadece
  Swagger UI/OpenAPI üretimi için, runtime davranışını değiştirmiyor.
- `application.yml`'e eklenen `logging.level` bloğu sadece INFO seviyesini pinliyor (Task 3, demo
  log temizliği) — işlevsel etkisi yok.
- Küçük bir tutarsızlık: `IncidentDraftController`/`InvestigationController`'da Spring'in
  `@RequestBody` ile Swagger'ın `@RequestBody` annotation'ı aynı dosyada çakıştığı için parametre
  tam-nitelikli isimle (`@org.springframework.web.bind.annotation.RequestBody`) kullanılmış —
  çalışıyor (derleniyor, testler geçiyor) ama okunabilirlik açısından ideal değil; davranışı
  bozmadığı ve M8 kapsamının dışında (kozmetik) olduğu için düzeltmedim, not olarak bırakıyorum.

## Sonraki oturum için not
Bağımsız bir doğrulama oturumu bu raporu ve `docs/17`'deki `### M8 status` kanıtlarını okuyup
`mvn verify` + `docker compose up --build` + secret scan'i tekrar çalıştırarak VERIFIED işaretleyebilir.
`main`'e merge bu oturumda yapılmadı — `docs/20-git-workflow.md`'ye göre ayrı bir doğrulama
oturumunun işi.
