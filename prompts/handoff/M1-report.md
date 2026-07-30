# M1 — Domain foundation

## Durum
DONE

## Kapsam
`domain` paketinde Spring/LangChain4j'den tamamen bağımsız saf Java ile `Investigation` ve `IncidentDraft` aggregate'leri, value object'ler (`TimeWindow`, `Evidence`, `Hypothesis`, `RecommendedAction`, `Approval`, `ValidationReport`), enum'lar (`InvestigationStatus`, `Severity`, `ActionType`, `ExecutionMode`, ayrıca lifecycle için `InvestigationPhase`, `IncidentDraftStatus`, `ApprovalDecision`, `ValidationStatus`) ve repository portları (`InvestigationRepository`, `IncidentDraftRepository`) yazıldı; 9 domain invariant'ının hepsi constructor/factory method içinde zorlanıyor ve her biri en az bir pozitif + bir negatif JUnit 5 testiyle kanıtlandı.

## Değişen dosyalar
- `src/main/java/com/example/otpsentinel/domain/InvestigationId.java` — yeni, aggregate id VO
- `src/main/java/com/example/otpsentinel/domain/IncidentDraftId.java` — yeni, aggregate id VO
- `src/main/java/com/example/otpsentinel/domain/InvestigationStatus.java` — yeni, FR-004 sonuç enum'u
- `src/main/java/com/example/otpsentinel/domain/InvestigationPhase.java` — yeni, süreç yaşam döngüsü enum'u (bkz. Spec çelişkisi)
- `src/main/java/com/example/otpsentinel/domain/Severity.java` — yeni
- `src/main/java/com/example/otpsentinel/domain/ActionType.java` — yeni
- `src/main/java/com/example/otpsentinel/domain/ExecutionMode.java` — yeni
- `src/main/java/com/example/otpsentinel/domain/IncidentDraftStatus.java` — yeni
- `src/main/java/com/example/otpsentinel/domain/ApprovalDecision.java` — yeni
- `src/main/java/com/example/otpsentinel/domain/ValidationStatus.java` — yeni
- `src/main/java/com/example/otpsentinel/domain/TimeWindow.java` — yeni, VO (min 1dk/max 24s invariant)
- `src/main/java/com/example/otpsentinel/domain/Evidence.java` — yeni, VO
- `src/main/java/com/example/otpsentinel/domain/Hypothesis.java` — yeni, VO (invariant 3)
- `src/main/java/com/example/otpsentinel/domain/RecommendedAction.java` — yeni, VO (invariant 6)
- `src/main/java/com/example/otpsentinel/domain/Approval.java` — yeni, VO
- `src/main/java/com/example/otpsentinel/domain/ValidationReport.java` — yeni, VO
- `src/main/java/com/example/otpsentinel/domain/Investigation.java` — yeni, aggregate root (invariant 1,2,4,5,9)
- `src/main/java/com/example/otpsentinel/domain/IncidentDraft.java` — yeni, aggregate root (invariant 7,8)
- `src/main/java/com/example/otpsentinel/domain/InvestigationRepository.java` — yeni, port (implementasyon yok)
- `src/main/java/com/example/otpsentinel/domain/IncidentDraftRepository.java` — yeni, port (implementasyon yok)
- `src/test/java/com/example/otpsentinel/domain/*Test.java` (6 dosya, 34 test) — yeni

## Testler (gerçek komut + gerçek çıktı, iddia değil)
Bu makinede yerel JDK/Maven yok; M0'da olduğu gibi WSL2 Docker Engine üzerinden çalıştırıldı.

Komut: `docker run --rm --network host -v /mnt/c/Users/Ali/Downloads/otp-incident-agent:/build -v maven-repo:/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock -w /build maven:3.9-eclipse-temurin-21 mvn verify`

Çıktı özeti:
```
[INFO] Running com.example.otpsentinel.domain.EvidenceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.otpsentinel.domain.HypothesisTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.otpsentinel.domain.IncidentDraftTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.otpsentinel.domain.InvestigationTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.otpsentinel.domain.RecommendedActionTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.otpsentinel.domain.TimeWindowTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.otpsentinel.OtpSentinelApplicationSmokeTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] BUILD SUCCESS
```

Framework izolasyon kontrolü (gerçek komut çıktısı):
```
$ grep -rn "org.springframework\|dev.langchain4j\|javax.persistence\|jakarta.persistence" src/main/java/com/example/otpsentinel/domain/
NO FRAMEWORK IMPORTS FOUND
```

## Karşılanan requirement/AC
- FR-004 — `InvestigationStatus` enum (`NO_ANOMALY, ANOMALY_CONFIRMED, INSUFFICIENT_DATA, PARTIAL_ANALYSIS, FAILED`) tanımlandı, `Investigation.complete/partial/fail` tarafından set ediliyor.
- FR-011 — `Hypothesis`: max 3 (`Investigation.proposeAnalysis`), rank alanı ile sıralama, supporting evidence zorunlu.
- FR-012 — `Investigation.proposeAnalysis` confidence 0.0–1.0 aralığını zorluyor.
- FR-015 — `IncidentDraft.create` idempotency key'e bağlı draft başına tek kez efektif; ikinci çağrı no-op.
- DATA-001 — Tüm zaman alanları `java.time.Instant` (UTC/ISO-8601 doğası gereği).
- AC-009 — `Investigation.proposeAnalysis` hypothesis'in supporting/contradicting evidence id'lerini toplanan evidence kümesiyle doğruluyor (invariant 9).
- AC-010 — `rejectsMoreThanThreeHypotheses` / `acceptsExactlyThreeHypotheses` testleri.
- AC-011 — `rejectsConfidenceAboveOne` / `acceptsConfidenceWithinRange` testleri.
- 9 domain invariant (docs/05) — hepsi `Investigation`/`IncidentDraft`/`Hypothesis`/`RecommendedAction` constructor veya state-transition metodlarında zorlanıyor; her biri pozitif+negatif testle kanıtlı (bkz. `InvestigationTest`, `IncidentDraftTest`, `HypothesisTest`, `RecommendedActionTest`).

## Karşılanmayan / ertelenen
- Repository implementasyonu, JPA/persistence, REST, tool, RAG — kapsam dışı (M2+), yazılmadı.

## Spec çelişkisi/belirsizlik (varsa)
1. **Status alanı ikiye ayrıldı.** `docs/05` Investigation aggregate'inde tek bir "Status" alanı (yaşam döngüsü: RECEIVED→...→COMPLETED/PARTIAL/FAILED) tanımlıyor, ama görev metninde istenen `InvestigationStatus` enum'u (NO_ANOMALY/ANOMALY_CONFIRMED/...) FR-004'teki **sonuç sınıflandırması**, farklı bir kavram. İkisini ayrı enum olarak modelledim: `InvestigationPhase` (süreç) ve `InvestigationStatus` (sonuç, `resultStatus` alanı). Görev metninde `InvestigationPhase` adı geçmiyordu; gerekli gördüm çünkü aksi halde yaşam döngüsü hiç temsil edilemezdi.
2. **Invariant 2 (ANOMALY_CONFIRMED → current+previous metrik)** — `Evidence` VO'sunda ayrı bir "current/previous" alanı yok (docs/05 böyle bir alan tanımlamıyor). "En az 2 metrik evidence" (metricName dolu) kuralı olarak yorumladım; gerçek current/previous ayrımı muhtemelen M2'de tool sonucu mapping'inde (`OtpMetricsResult.previousPeriod`) netleşecek.
3. **Invariant 9 (tool'da olmayan sayı evidence olamaz)** — Domain katmanında tool truth verisi yok; bunu "hypothesis, investigation'a eklenmiş evidence id'lerinden başkasına referans veremez" şeklinde uyguladım. Sayısal iddianın gerçekten tool çıktısıyla eşleştiği kontrolü (AC-023, numeric claim validator) M6'nın işi.
4. **RecommendedAction.risk / Hypothesis.probability** — `docs/06-api-contracts.md` örneğinde `"probability": "HIGH"` string görünüyor ama görev metni sadece 4 enum istiyor (Severity dahil, ayrı bir Probability enum yok). `probability`'i `double` (0–1) yaptım, `risk` alanı için `Severity` enum'unu yeniden kullandım. API/sunum katmanında sayısal→etiket dönüşümü gerekebilir (M7 kapsamı).

## Sonraki oturum için not
M2 — fixture tools için devam edilebilir; `Evidence`/`InvestigationId` gibi domain tipleri tool adaptörlerinde kullanılacak. Yukarıdaki 4 madde (özellikle current/previous metrik ayrımı) M2'de tool mapping tasarlanırken gözden geçirilmeli.
