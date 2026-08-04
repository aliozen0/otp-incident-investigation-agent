# M6 — Validation/governance

## Durum
DONE

## Kapsam
docs/07 "Validation pipeline" adım 3/6/7/8 ve docs/09 PII scan'i tek bir `ClaimValidator`
(application katmanı) altında topladım: unknown evidence reference (M5'te vardı, buraya taşındı),
numeric claim source check (AC-023, `%`/`percent` kalıplarını toplanan `Evidence.metricValue`
kümesiyle karşılaştırır), forbidden automatic action (AC-012, `RESTART/ROLLBACK/CONFIG_CHANGE` +
`requiresApproval=false` — domain invariant'ın yakalamadığı, risk seviyesi HIGH/CRITICAL olmayan
durumu kapatan ek katman), correlation wording (kesin nedensellik ifadeleri için WARNING, hard-fail
değil — gerekçe aşağıda) ve PII scan (yeni `PiiScanner`: OTP-etiketli kod, telefon-şekilli rakam
dizisi, api-key/secret-etiketli token; PII bulunursa reject). `IncidentInvestigationService` artık
kendi `citesUnknownEvidence` kopyasını değil `ClaimValidator`'ı çağırıyor; `ValidationReport`
warning listesine kural kodu (`UNSUPPORTED_NUMERIC_CLAIM:`, `FORBIDDEN_AUTOMATIC_ACTION:`, vb.)
yazılıyor. `EvidenceCollector`'a audit-enabled ikinci bir constructor eklendi: knowledge search
sonucu içeriği `ContentSanitizer.hasInstructionPattern` ile taranıyor, eşleşirse yeni
`AuditEventType.PROMPT_INJECTION_SIGNAL` ile `AuditEventRepository`'ye yazılıyor — tool policy veya
investigation sonucu bundan etkilenmiyor (ayrı testle kanıtlandı). M1/M5 domain invariant'ları
(evidence-hypothesis ilişkisi, high-risk aksiyon onayı) değiştirilmedi; `ClaimValidator` bunların
önüne ek bir erken-ret katmanı olarak eklendi.

## Değişen dosyalar
- `src/main/java/com/example/otpsentinel/application/ClaimValidator.java` — yeni: docs/07 pipeline
  adım 3/6/7/8.
- `src/main/java/com/example/otpsentinel/application/PiiScanner.java` — yeni: OTP/telefon/API-key
  regex taraması.
- `src/main/java/com/example/otpsentinel/application/IncidentInvestigationService.java` —
  `citesUnknownEvidence`'ı `ClaimValidator`'a devretti, claim warning'lerini `ValidationReport`'a
  yansıtıyor.
- `src/main/java/com/example/otpsentinel/agent/EvidenceCollector.java` — audit-enabled ikinci
  constructor, `collectKnowledge` içinde prompt-injection sinyali taraması.
- `src/main/java/com/example/otpsentinel/domain/AuditEventType.java` — `PROMPT_INJECTION_SIGNAL`
  eklendi (FR-017 sabit listesinin küçük, gerekçeli genişlemesi).
- `src/test/java/com/example/otpsentinel/application/ClaimValidatorTest.java`,
  `PiiScannerTest.java`, `PromptInjectionSignalTest.java` — yeni.
- `src/test/java/com/example/otpsentinel/agent/EvidenceCollectorTest.java`,
  `src/test/java/com/example/otpsentinel/application/IncidentInvestigationServiceTest.java` — yeni
  test case'leri eklendi (mevcutlar değişmedi).
- `docs/17-traceability-risk-dod.md` — "Prompt injection pass" işaretlendi.

## Testler (gerçek komut + gerçek çıktı, iddia değil)
Komut:
`mvn -B spotless:apply && mvn -B verify -Dsurefire.excludedGroups=local-live`

Çıktı özeti:
```text
[INFO] Tests run: 115, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- spotless:2.43.0:check (spotless-check) @ otp-sentinel ---
[INFO] Spotless.Java is keeping 140 files clean - 0 needs changes to be clean
[INFO] BUILD SUCCESS
[INFO] Total time:  55.653 s
```

İzole validator/PII/prompt-injection testleri:
`mvn -B test -Dtest=ClaimValidatorTest,PiiScannerTest,EvidenceCollectorTest,PromptInjectionSignalTest,IncidentInvestigationServiceTest`
— tamamı ayrı ayrı da çalıştırıldı (yukarıdaki geliştirme adımlarında), her biri BUILD SUCCESS.

Test loglarında secret/OTP/telefon taraması (AC-028, test literalleri kasıtlı sahte değerler,
`sk-abcdef...`, `482913`, `555-123-4567`):
```text
grep -ciE "sk-abcdef|482913|555-123-4567" verify-log => 0
```

## Karşılanan requirement/AC
- AC-023 (Numeric claim validator) — `ClaimValidatorTest.rejectsUnsupportedNumericClaim`, docs/12
  "Reject unsupported numeric claim" senaryosu birebir.
- AC-012 (Forbidden automatic action) — `ClaimValidatorTest.rejectsAutomaticRollback` +
  `IncidentInvestigationServiceTest.forbiddenAutomaticActionIsRejectedAsFailure` (uçtan uca), docs/12
  "Reject automatic rollback".
- AC-013 — değişmedi; `createIncidentDraft` hâlâ agent tool setine bağlı değil (M5'te doğrulandı).
- AC-021 (Prompt injection tool policy/approval'ı değiştirmemeli) —
  `PromptInjectionSignalTest`: sinyal audit'e yazılıyor, tool sırası/sayısı ve investigation sonucu
  değişmiyor.
- AC-022 — M5'te vardı, dokunulmadı (repair-once/FAILED).
- AC-028 (test loglarında secret/OTP/telefon yok) — yukarıdaki grep taraması.
- docs/12 "Evidence validation" feature'ının 5 senaryosu: 2'si (repair/fail-after-two) M5'ten
  değişmeden geçiyor, 3'ü (`ClaimValidatorTest` + entegrasyon testleri) bu milestone'da eklendi.
- docs/12 "Prompt injection" feature'ının tek senaryosu (`PromptInjectionSignalTest`).

## Karşılanmayan / ertelenen
- REST endpoint'leri, persistence/audit orchestration wiring (gerçek `AuditEventRepository`
  bean'inin `IncidentInvestigationService`'e DI ile bağlanması) — M7.
- Correlation wording check'i **reject değil warning** olarak uyguladım: docs/07 M6 prompt'u
  "tespit edip reddet/uyar" diyerek seçimi bıraktı; hiçbir Gherkin senaryosu bunun için sert bir
  fail beklemiyor (yalnızca OTP-DROP-001'in "should not claim caused" pozitif assertion'ı var, o da
  M5'te zaten stub kelime seçimiyle sağlanıyor). Sert reddi seçmedim çünkü model "correlated, not
  caused" gibi meşru ama "caused" kelimesini geçen (örn. negasyonlu) bir cümle kurarsa false-positive
  ile geçerli bir analizi reddetme riski var. Karar `ClaimValidator` Javadoc'unda ve bu raporda not
  düşüldü; M7/canlı model gözlemiyle sert reddin gerekip gerekmediği yeniden değerlendirilebilir.
- PII scan bulgusunda **redaksiyon değil reject** seçildi (basitlik); redaksiyon istenirse ayrı bir
  task.
- Numeric claim validator yalnızca yüzde/percent kalıplarını kontrol ediyor (docs'taki örnek de bu);
  genel sayı taraması (ör. ham sayaç değerleri) kapsam dışı bırakıldı — aşırı mühendislik ve
  false-positive riski.

## Spec çelişkisi/belirsizlik (varsa)
1. `AuditEventType.PROMPT_INJECTION_SIGNAL` FR-017'nin sabit event listesine eklendi — literal
   spec'ten küçük bir sapma, M6 prompt'unda önceden onaylanmıştı ("raporla" notuyla).
2. Correlation wording check için reject/warning seçimi yukarıda gerekçelendirildi; kesin karar
   sonraki oturumda gözden geçirilebilir.

## Sonraki oturum için not
M6 branch'i bağımsız verify'a hazır. M7'de: REST endpoint'leri, `AuditEventRepository`'nin
`IncidentInvestigationService`/`EvidenceCollector`'a gerçek DI ile bağlanması (M6'da yalnızca test
seviyesinde kanıtlandı), `createIncidentDraft` onay akışı.
