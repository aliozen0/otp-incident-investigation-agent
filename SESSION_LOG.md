# Session Log

Append-only. Bir satır = bir oturum. Yeni oturum açmadan önce sadece bu dosyanın son birkaç satırını ve ilgili `prompts/handoff/{ID}-report.md` dosyasını oku — tüm spec'i tekrar yüklemene gerek yok.

Format:

`{TARIH} | {ID} | {DURUM: DONE/BLOCKED/FAILED/VERIFIED/REJECTED} | {report dosyası} | {tek satır özet}`

---

2026-07-30 | M0 | DONE | prompts/handoff/M0-report.md | Java21+Spring Boot iskeleti, Flyway+Actuator+Dockerfile+Compose+CI+Spotless kuruldu; mvn verify ve docker compose up --build (WSL Docker ile) gerçekten çalıştırıldı, health UP.
2026-07-30 | M0 | VERIFIED | prompts/handoff/M0-report.md | Bağımsız tekrar çalıştırıldı: mvn verify BUILD SUCCESS, docker compose up --build sonrası app+db healthy, dosya listesi rapor ile eşleşti, scope taşması yok.
