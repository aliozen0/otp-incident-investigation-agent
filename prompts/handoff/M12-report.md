# M12 — Agent console frontend (ChatGPT/Claude tarzı sohbet arayüzü, Türkçe, grafikli)

## Durum
DONE

## Kapsam
M10'un tek-sayfa form→sonuç arayüzü, kullanıcının "bir LLM ile konuşuyor gibi değilim" geri bildirimi üzerine tamamen ChatGPT/Claude tarzı bir sohbet konsoluna dönüştürüldü: sol sidebar (client-tracked sohbet thread listesi — backend'de "tüm session'ları listele" endpoint'i olmadığı için `localStorage`'da tutuluyor), orta panel (kullanıcı/agent balonlu sohbet akışı, typing-indicator, `Enter` ile gönder), üst navbar (ayarlar paneli: model seçici, hızlı/detaylı mod, bilgi tabanı belge listesi+yükleme formu). Agent balonunun içinde M10'un zengin sonuç kartı korunmuş, üç Recharts grafiği eklenmiş (hipotez-olasılık sıralı çubuk, güven göstergesi, bilgi-benzerlik çubuğu — `dataviz` skill'i ile form seçildi, renk paleti mevcut `--color-signal` token'ından türetilip `node scripts/validate_palette.js` ile PASS doğrulandı), ham backend `summary` alanı (sadece enum adı) hiç render edilmiyor, yerine status+severity+ilk hipotezden sentezlenen tek satır Türkçe özet var. Tüm statik metin ve backend enum'ları (status/severity/probability/mode/documentType/sourceType/actionType/errorCode) tek dosyalık `labels.ts` sözlüğünden Türkçe'ye çevriliyor, sayı/tarih `tr-TR` formatında. Zaman aralığı artık native `datetime-local` input (eski checkbox+çıplak-ISO-metin deseni kaldırıldı). Süreç M5-M11 gibi işledi: `superpowers:writing-plans` ile 9 küçük task'a bölündü, `superpowers:subagent-driven-development` ile her task taze implementer + ayrı reviewer subagent'a yaptırıldı (`frontend-design` ve `dataviz` skill'leri tasarım/grafik kararları için kullanıldı — bkz. aşağıda). 9/9 task review sonunda temiz (4 task'ta 1 fix round: sessionStore'da guardsız `JSON.parse` + sonradan bulunan flaky UUID-tiebreaker; test-setup.ts'de eksik `ponytail:` yorum + tiebreaker'ın regresyon testi yoktu; ResultCard alt bileşenlerinde 3 çevrilmemiş İngilizce/ham-enum string; SettingsPanel'de zayıf test kapsamı + sessiz fetch hataları). Final whole-branch review (opus) 4 Important + 1 ertelenmiş Minor buldu (hata mesajları hâlâ İngilizce'ydi, `UI_TEXT.errorTitle` tanımlı ama kullanılmıyordu; `EvidenceLedger`/`ActionsList` ham `sourceType`/`actionType` render ediyordu; `index.html` hâlâ `lang="en"`, Türkçe büyük harf dönüşümünü bozuyordu; `SettingsPanel`'deki model `<select>` tutmadığı bir seçimi gösteriyordu — `modelId` null iken tarayıcı ilk modeli seçili gösteriyor ama state hiç güncellenmiyordu; ayrıca ertelenmiş `SEVERITY_LABEL_TR` HIGH/CRITICAL etiket takası) — tek fix dalgasında hepsi kök nedenden düzeltildi, scoped re-review tümünü "ADDRESSED, no new breakage" doğruladı.

## Tasarım/grafik kararları (frontend-design + dataviz)
- **frontend-design**: Mevcut M10 token sistemi (Tailwind `@theme` — paper/ink/signal/alert/confirm/danger) korunarak sohbet arayüzüne uyarlandı; yeni bir görsel kimlik icat edilmedi (proje zaten bir tasarım diline sahipti, brief bunu korumayı istiyordu). Sinyal rengi (mavi) sohbet balonlarında kullanıcı mesajı arka planı olarak, agent balonu ise mevcut kart stiliyle ayrıştırıldı.
- **dataviz**: Hipotez olasılığı (LOW/MEDIUM/HIGH) sıralı bir ölçek olduğu için **ordinal** tek-hue mavi rampa seçildi (kategorik değil — sıra anlam taşıyor). Brief'teki aday üçlü (`#B7CDF3,#5B87DE,#1D4ED8`) ilk validator çalıştırmasında ışık-ucu kontrastta FAIL verdi (1.50:1, 2:1 eşiğinin altı); skill'in snap-to-passing prosedürüyle LOW adımı `#8FAEEA`'ya kadar koyulaştırıldı, ikinci deneme PASS etti (2.08:1). Nihai doğrulanmış üçlü: `LOW=#8FAEEA, MEDIUM=#5B87DE, HIGH=#1D4ED8` — `node scripts/validate_palette.js "#8FAEEA,#5B87DE,#1D4ED8" --mode light --ordinal --surface "#F7F7F4"` ile bu oturumda bağımsız olarak yeniden doğrulandı, tüm kontroller PASS. Confidence ve knowledge-similarity için ayrı tek-hue sequential dolgu çubukları (aynı validator kategorik/ordinal palet için değil, tek renk sequential rampalar için tasarlanmadığından ayrıca koşulmadı — skill'in kendi notu: bu beklenen bir durum, "iyi bir rampayı" kategorik validator'a zorlamak yanlış).
- **Bilinen backend sınırlaması**: `KnowledgeReferenceDto` gerçek backend'de yalnızca `documentId` dönüyor (`similarityScore`/`title`/`version`/`chunkId` hiç dolmuyor — `InvestigationDtoMapper.toDto` sadece `documentId`'yi map'liyor, Controller'daki Swagger örneği daha zengin ama gerçek koddan farklı). `SimilarityBar` bileşeni bu yüzden savunmacı: `similarityScore` yoksa hiçbir çubuk/grafik render etmiyor, sadece düz metin listesine geri düşüyor — bu bir frontend bug'ı değil, backend'in mevcut bir boşluğu (M11'de kapsam dışı bırakılmıştı, M12'de backend'e dokunulmadığı için burada da düzeltilemedi).

## Değişen dosyalar
- `frontend/src/api/types.ts`, `frontend/src/api/client.ts` — `sessionId`/`modelId`/`mode` alanları, `listSessionInvestigations`/`listModels`/`listKnowledgeDocuments`/`uploadKnowledgeDocument`
- `frontend/src/lib/labels.ts` — yeni, Türkçe sözlük (status/severity/probability/mode/documentType/sourceType/actionType/errorCode + `tr-TR` formatlayıcılar)
- `frontend/src/lib/summarize.ts` — yeni, ham `summary` yerine sentezlenen Türkçe tek satır
- `frontend/src/lib/sessionStore.ts` — yeni, `localStorage`-backed thread listesi + soru önbelleği (backend soruyu/session listesini hiç döndürmediği için)
- `frontend/src/components/charts/{HypothesisChart,ConfidenceGauge,SimilarityBar}.tsx` — yeni, Recharts grafikleri
- `frontend/scripts/validate_palette.js` — yeni, dataviz skill'inden birebir kopya
- `frontend/src/components/{ChatMessage,TypingIndicator,ChatComposer,Sidebar,SettingsPanel}.tsx` — yeni, sohbet arayüzü bileşenleri
- `frontend/src/components/{ResultCard,StatusBadge,HypothesisList,ActionsList,KnowledgeReferences,EvidenceLedger,IncidentDecisionPanel,ErrorPanel,Header}.tsx` — Türkçe etiketler, grafik entegrasyonu, navbar dönüşümü
- `frontend/src/App.tsx` — üç panelli sohbet konsolu (sidebar + akış + ayarlar drawer), session/model/mode wiring
- `frontend/src/components/QuestionForm.tsx` — silindi (composer'la değiştirildi)
- `frontend/index.html` — `lang="tr"`
- `frontend/package.json` — `recharts` eklendi (tek yeni bağımlılık)
- `docs/superpowers/plans/2026-08-02-m12-agent-console-frontend.md` — implementasyon planı
- Backend: **dokunulmadı** (M11 zaten hazırdı, plan kısıtı buydu)

## Testler (gerçek komut + gerçek çıktı, iddia değil)
Frontend:
```
$ npm run test
 Test Files  12 passed (12)
      Tests  44 passed (44)
```
```
$ npm run build
✓ built in 620ms
```
```
$ node scripts/validate_palette.js "#8FAEEA,#5B87DE,#1D4ED8" --mode light --ordinal --surface "#F7F7F4"
  [PASS] Lightness monotone
  [PASS] Adjacent ΔL
  [PASS] Light-end contrast     #8FAEEA at 2.08:1 vs surface
  [PASS] Single hue
  → ALL CHECKS PASS
```
Backend (WSL2, `NVIDIA_API_KEY` olmadan, dokunulmadı ama doğrulandı):
```
$ mvn -o verify
[INFO] Tests run: 167, Failures: 0, Errors: 0, Skipped: 0
[INFO] Spotless.Java is keeping 187 files clean
[INFO] BUILD SUCCESS
[INFO] Total time:  01:58 min
```

**Yapılamadı (bu oturumda ortam kısıtı):** `docker compose up --build` + gerçek Chrome tarayıcısında (`mcp__claude-in-chrome__*`) uçtan uca manuel akış (yeni sohbet → soru → grafikli sonuç → takip sorusu ile memory doğrulama → model değiştir → mod değiştir → belge yükle → RAG'in yeni belgeyi kullandığını gör). Bu oturumun ortamında `docker` komutu mevcut değil (ne native Windows'ta ne WSL2'de). Otomatik doğrulanabilen her şey (frontend build/test, palet, backend `mvn verify`) yapıldı; canlı tarayıcı akışı bağımsız oturuma kalıyor.

## Karşılanan requirement/AC
- Layout (sidebar/orta panel/navbar) — Task 7 (Sidebar) + Task 9 (App.tsx) ile.
- Sohbet akışı + aynı `sessionId` ile takip sorusu — `App.tsx`'in `handleSubmit`'i `activeSessionId`'yi her `POST /investigations`'a ekliyor; `App.test.tsx` iki ardışık sorunun aynı `sessionId`'yi taşıdığını gerçek mock-fetch çağrı body'lerinden assert ediyor (prose değil, gerçek davranış testi) — ancak gerçek backend session-memory davranışı (LangChain4j `@MemoryId`) yalnızca canlı `docker compose` + Chrome ile doğrulanabilir, bu oturumda yapılamadı.
- Ayarlar paneli (model/mod/RAG belge listesi+yükleme) — Task 8 (SettingsPanel).
- Grafikler (`dataviz` skill, palet doğrulaması) — Task 4, yukarıda detaylandırıldı.
- Türkçe lokalizasyon (`labels.ts` tek dosya, i18n kütüphanesi yok) — Task 2, 5, ve final review fix dalgasıyla tamamlandı (hata mesajları + sourceType/actionType dahil).
- Zaman aralığı `datetime-local` — Task 6 (ChatComposer).
- `mvn spotless:apply`+`mvn verify` (backend değişmemiş) ve `npm run build`/`npm run test` yeşil — yukarıda.

## Karşılanmayan / ertelenen
- **Belge yükleme sonrası RAG kullanımının canlı doğrulanması** ("yeni belgeyle ilgili soru sor, `knowledgeReferences`'ta göründüğünü kontrol et") — docker/Chrome erişimi olmadığı için yapılamadı. Kod tarafı hazır (`SettingsPanel` upload + `KnowledgeReferences`/`SimilarityBar` render), ama canlı doğrulama bağımsız oturuma kalıyor.
- `frontend/src/App.tsx`'teki `selectSession`'ın sessiz `catch { setTurns([]) }`'ı — thread yükleme hatası ile boş thread ayırt edilemiyor (final review Minor #6, backend hatası nadir olduğu için ertelendi).
- `busy` state global (thread-bazlı değil) — aynı anda iki thread'de paralel soru sorulamıyor (final review Minor #7, tek-kullanıcılı demo için kabul edilebilir).
- Tamamlanan turn'ün id'si client-UUID'den `investigationId`'ye değişiyor, bu da `ChatMessage` alt ağacını remount ediyor ve `IncidentDecisionPanel` state'ini sıfırlıyor (final review Minor #8).
- `ChatComposer`'da zaman-aralığı satırı textarea'nın üstünde (planın istediği sıranın tersi) — brief'in kendi test kodundaki `getAllByDisplayValue('')` belirsizliğini çözmek için JSX sırası değiştirildi, test sorgusu yerine (final review Minor #9, davranış değişikliği yok).
- Kullanılmayan `LoadingState.tsx`/`Footer.tsx` ve 8 kullanılmayan `UI_TEXT` anahtarı, belge yükleme formunda `effectiveFrom` input'unun label'ı yok (final review Minor #10).
- `formatDateTime` geçersiz bir zaman damgasında `RangeError` fırlatabilir, error boundary yok (final review Minor #11).
- `SettingsPanel`'deki `loadError` başarılı bir refresh'ten sonra otomatik temizlenmiyor (final review Minor #12).
- `client.ts`'te `sessionId` URL path'ine `encodeURIComponent` olmadan enjekte ediliyor — sömürülebilir değil (her zaman client-üretimi UUID) ama ucuz bir düzeltme olurdu (final review Minor #13).
- Thread listesi `localStorage`'da — farklı tarayıcı/profil/gizli modda sidebar boş görünür (backend session'ları hâlâ tutuyor olsa bile). Backend'in "tüm session'ları listele" endpoint'i olmamasının kaçınılmaz bir sonucu, veri kaybı değil.

## Spec çelişkisi/belirsizlik (varsa)
- Backend `KnowledgeReferenceDto`, plan'ın grafik gereksinimi yazıldığı sırada varsayılan zengin alanları (`similarityScore`/`title`) döndürmüyor — yalnızca `documentId`. Kod tarafında savunmacı ele alındı (bkz. yukarıda "Bilinen backend sınırlaması"), backend'e dokunulmadığı için düzeltilemedi.
- `SEVERITY_LABEL_TR`'deki HIGH/CRITICAL etiket takası (plan'ın kendi Task 2 kod bloğunda vardı) final review'de bulundu ve kullanıcıya sorulmadan düzeltildi (anlamsal bir isimlendirme hatasıydı, plan'ın niyetiyle çelişmiyordu — "Kritik" CRITICAL'ın direkt karşılığı olduğu için HIGH'a atanması tutarsızdı).

## Sonraki oturum için not
Bağımsız oturum: (1) `mvn verify`'i gerçekten çalıştırıp doğrulamalı (167/167 bekleniyor, bu branch backend'e dokunmadı); (2) `npm run build`+`npm run test` gerçekten çalıştırıp doğrulamalı (44/44 bekleniyor); (3) **asıl eksik**: `docker compose up --build` + gerçek Chrome tarayıcısında tam akışı bizzat denemeli — özellikle takip sorusu ile session-memory'nin gerçekten çalıştığını, model değiştirmenin etkili olduğunu (final review'de bulunan `<select>` state bug'ı düzeltildi, ama canlı doğrulanmadı), ve belge yükleyip RAG'in kullandığını görmeli. Kendi işim VERIFIED değil DONE — bağımsız oturum doğrulayacak.
