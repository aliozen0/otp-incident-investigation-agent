# Session Log

Append-only. Bir satır = bir oturum. Yeni oturum açmadan önce sadece bu dosyanın son birkaç satırını ve ilgili `prompts/handoff/{ID}-report.md` dosyasını oku — tüm spec'i tekrar yüklemene gerek yok.

Format:

`{TARIH} | {ID} | {DURUM: DONE/BLOCKED/FAILED/VERIFIED/REJECTED} | {report dosyası} | {tek satır özet}`

---
