## Görev: Bugfix — {KISA_TANIM}

### Semptom

{GOZLENEN_DAVRANIS}

### Beklenen (spec'e göre)

- İlgili acceptance criterion: {AC_ID} (`docs/11-acceptance-criteria.md`)
- İlgili requirement: {FR_veya_AI_ID} (`docs/03-system-requirements.md`)

### Kök neden analizi iste

Düzeltmeden önce:

1. Semptomun geçtiği tüm caller'ları bul (yalnızca ticket'ın gösterdiği yolu değil).
2. Kök nedeni ortak noktada (ör. paylaşılan validator/service) tespit et.
3. Kök nedeni ve önerilen fix noktasını özetle, sonra uygula.

### Kısıtlar

- Yalnızca bu davranışı düzelt; yakın kodda görülen ilgisiz iyileştirmeyi ayrı bırak.
- Fix, ilgili tüm caller'ları kapsayacak yerde olmalı (symptom patch değil).

### Bitti sayılması için

- Önce bug'ı reprodüklayan failing test var.
- Fix sonrası test geçiyor.
- Regresyon: aynı fonksiyonun diğer testleri hâlâ geçiyor.

### Oturum sonu

`prompts/08-session-report.md` kuralına göre rapor yaz ve `SESSION_LOG.md`'ye satır ekle.
