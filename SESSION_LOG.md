# Session Log

Append-only. Bir satır = bir oturum. Yeni oturum açmadan önce sadece bu dosyanın son birkaç satırını ve ilgili `prompts/handoff/{ID}-report.md` dosyasını oku — tüm spec'i tekrar yüklemene gerek yok.

Format:

`{TARIH} | {ID} | {DURUM: DONE/BLOCKED/FAILED/VERIFIED/REJECTED} | {report dosyası} | {tek satır özet}`

---

2026-07-30 | M0 | DONE | prompts/handoff/M0-report.md | Java21+Spring Boot iskeleti, Flyway+Actuator+Dockerfile+Compose+CI+Spotless kuruldu; mvn verify ve docker compose up --build (WSL Docker ile) gerçekten çalıştırıldı, health UP.
2026-07-30 | M0 | VERIFIED | prompts/handoff/M0-report.md | Bağımsız tekrar çalıştırıldı: mvn verify BUILD SUCCESS, docker compose up --build sonrası app+db healthy, dosya listesi rapor ile eşleşti, scope taşması yok.
2026-07-30 | M1 | DONE | prompts/handoff/M1-report.md | Investigation/IncidentDraft aggregate, VO'lar, 4 enum + yaşam döngüsü enum'ları, repository portları saf Java ile yazıldı; 9 invariant her biri pozitif+negatif testle kanıtlandı (34 test), mvn verify BUILD SUCCESS, domain paketinde Spring/LangChain4j import'u yok (grep ile doğrulandı).
2026-07-30 | M1 | VERIFIED | prompts/handoff/M1-report.md | Bağımsız tekrar çalıştırıldı: mvn verify 34/34 test BUILD SUCCESS, grep framework izolasyonu doğrulandı, dosya listesi eşleşti. Raporda 4 spec belirsizliği not düşülmüş (InvestigationPhase/Status ayrımı, current/previous evidence yorumu, invariant 9 kapsamı, probability/risk tip seçimi) — engelleyici değil, M2/M6'da gözden geçirilecek.
