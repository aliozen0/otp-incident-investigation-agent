## Görev: Validation kuralı — `{KURAL_ADI}`

Kaynak: `docs/07-agent-tool-spec.md` (Validation pipeline) ve `docs/09-security-governance.md`.

### Kural

- Ne kontrol ediyor: {KONTROL_TANIMI}
- Hangi pipeline adımına ekleniyor: {PIPELINE_ADIMI, örn: numeric claim source check, forbidden auto-action check}
- Başarısızlık kodu: {FAILURE_CODE, örn: UNSUPPORTED_NUMERIC_CLAIM, UNKNOWN_EVIDENCE_REFERENCE, FORBIDDEN_AUTOMATIC_ACTION}
- İlgili ATDD senaryosu: `docs/12-atdd-gherkin.md` → {SENARYO_ADI}

### Kısıtlar

- Bu kural deterministik Java kodda yaşar; framework guardrail'ine bırakılmaz.
- Kural başarısız olursa: {SONUC, örn: FAILED / analysis yayımlanmaz / repair tetiklenir}.
- Kuralın kendisi PII/secret loglamaz.

### Sırayla

1. ATDD senaryosunu (veya yenisini) failing test olarak yaz.
2. Validator'a kuralı ekle.
3. Validation report'a yeni failure code'u ekle.
4. Negatif + pozitif case test et.

### Bitti sayılması için

- İlgili Gherkin senaryosu geçiyor.
- Yanlış pozitif/negatif olmadığı ayrı test ile gösterildi.

### Oturum sonu

`prompts/08-session-report.md` kuralına göre rapor yaz ve `SESSION_LOG.md`'ye satır ekle.
