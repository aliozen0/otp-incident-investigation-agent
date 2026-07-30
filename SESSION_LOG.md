# Session Log

Append-only. Bir satır = bir oturum. Yeni oturum açmadan önce sadece bu dosyanın son birkaç satırını ve ilgili `prompts/handoff/{ID}-report.md` dosyasını oku — tüm spec'i tekrar yüklemene gerek yok.

Format:

`{TARIH} | {ID} | {DURUM: DONE/BLOCKED/FAILED/VERIFIED/REJECTED} | {report dosyası} | {tek satır özet}`

---

2026-07-30 | M0 | DONE | prompts/handoff/M0-report.md | Java21+Spring Boot iskeleti, Flyway+Actuator+Dockerfile+Compose+CI+Spotless kuruldu; mvn verify ve docker compose up --build (WSL Docker ile) gerçekten çalıştırıldı, health UP.
2026-07-30 | M0 | VERIFIED | prompts/handoff/M0-report.md | Bağımsız tekrar çalıştırıldı: mvn verify BUILD SUCCESS, docker compose up --build sonrası app+db healthy, dosya listesi rapor ile eşleşti, scope taşması yok.
2026-07-30 | M1 | DONE | prompts/handoff/M1-report.md | Investigation/IncidentDraft aggregate, VO'lar, 4 enum + yaşam döngüsü enum'ları, repository portları saf Java ile yazıldı; 9 invariant her biri pozitif+negatif testle kanıtlandı (34 test), mvn verify BUILD SUCCESS, domain paketinde Spring/LangChain4j import'u yok (grep ile doğrulandı).
2026-07-30 | M1 | VERIFIED | prompts/handoff/M1-report.md | Bağımsız tekrar çalıştırıldı: mvn verify 34/34 test BUILD SUCCESS, grep framework izolasyonu doğrulandı, dosya listesi eşleşti. Raporda 4 spec belirsizliği not düşülmüş (InvestigationPhase/Status ayrımı, current/previous evidence yorumu, invariant 9 kapsamı, probability/risk tip seçimi) — engelleyici değil, M2/M6'da gözden geçirilecek.
2026-07-30 | M2 | DONE | prompts/handoff/M2-report.md | 5 fixture tool (T-001..T-005) port+adapter+ortak ToolResult zarfı yazıldı; OTP-DROP-001 sayıları docs/15 ile birebir 10/10 testte doğrulandı, OTP-PARTIAL-001 üzerinden getProviderHealth timeout senaryosu testli. mvn verify preexisting CRLF/spotless ve Testcontainers-docker-socket sorunlarından BUILD FAILURE veriyor (M2 öncesi mevcut, dokunulmadı) — M2'nin kendi testleri izole 10/10 yeşil.
2026-07-30 | M2 | VERIFIED | prompts/handoff/M2-report.md | Bağımsız doğrulama: raporun "preexisting CRLF" iddiası yanlış çıktı — kök neden Windows core.autocrlf idi (.gitattributes + normalize ile düzeltildi, fix(build) commit); ardından 16 dosyada gerçek google-java-format ihlali bulundu (M2'ye ait, preexisting değil), spotless:apply ile düzeltildi. Sonrasında mvn verify 44/44 test BUILD SUCCESS. Dosya listesi rapor ile eşleşti.
2026-07-30 | M3 | DONE | prompts/handoff/M3-report.md | V2 Flyway şeması (investigation/incident_draft/audit_event), JdbcTemplate tabanlı repository implementasyonları (JSONB + reconstitute factory'ler), append-only audit (DB trigger) ve idempotency (DB unique constraint) yazıldı; Testcontainers ile 8 yeni entegrasyon testi dahil mvn verify 52/52 test BUILD SUCCESS.
