## Görev: {MILESTONE_ID} — {KISA_BASLIK}

Kaynak: `docs/14-implementation-plan.md` → **{MILESTONE_ID}**.

İlgili spec dosyaları: {DOC_LIST, örn: docs/05-domain-and-architecture.md, docs/03-system-requirements.md}

### Kapsam

Yalnızca şunu yap: {TEK_CUMLE_TASK}

Bu milestone'un "Kabul" kriteri: {MILESTONE_KABUL_KRITERI}

### Sırayla

1. İlgili requirement/acceptance criterion'ı belirle ({FR_veya_AC_ID}).
2. Değişecek dosyaları listele.
3. Önce failing test yaz.
4. Minimum implementasyonla geçir.
5. Refactor.
6. Etkilenen dokümanı güncelle (gerekiyorsa).

### Kısıtlar

- Bu milestone dışına taşma; ilgisiz iyileştirme ekleme.
- `AGENTS.md`'deki mimari sınırları (api→application→domain, adapters→ports) koru.
- Onaylanmamış yeni bağımlılık/altyapı ekleme.

### Bitti sayılması için

- İlgili testler geçiyor (çalıştırıldı, iddia edilmedi).
- Failure path testli.
- `docs/17-traceability-risk-dod.md` DoD listesine uyuyor.

### Oturum sonu

`prompts/08-session-report.md` kuralına göre rapor yaz ve `SESSION_LOG.md`'ye satır ekle.
