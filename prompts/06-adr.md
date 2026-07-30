## Görev: ADR önerisi — {KARAR_BASLIGI}

Kaynak: `docs/16-adr.md` (mevcut ADR'lerle çelişip çelişmediğini kontrol et).

### İçerik iste (kod yazma, önce sadece ADR taslağı)

- **Status:** Proposed
- **Bağlam:** {NEDEN_BU_KARAR_GEREKLI}
- **Karar:** {ONERILEN_KARAR}
- **Alternatifler:** {DEGERLENDIRILEN_SECENEKLER}
- **Sonuç/etki:** {TRADE_OFF}
- **Scope filtresi kontrolü** (`docs/00-project-charter.md`): ana senaryoyu güçlendiriyor mu, ölçülebilir mi, demo güvenilirliğini bozmuyor mu, gereksiz altyapı eklemiyor mu, görüşmede açıklanabilir mi.

### Kısıtlar

- Kafka/Redis/Kubernetes/Python servisi/çoklu agent/başka AI framework öneriyorsa: onay olmadan uygulamaya geçme, sadece ADR olarak sun.
- Mevcut ADR'lerden biriyle çelişiyorsa önce bunu açıkça belirt.

### Bitti sayılması için

- ADR taslağı `docs/16-adr.md` formatına uyuyor.
- Kullanıcı onayı olmadan implementasyona geçilmedi.

### Oturum sonu

`prompts/08-session-report.md` kuralına göre rapor yaz ve `SESSION_LOG.md`'ye satır ekle.
