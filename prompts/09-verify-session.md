## Görev: Doğrulama oturumu — {ID}

Bu ayrı bir oturum. Önceki oturumun tüm context'i yok — sadece şunları oku:

1. `SESSION_LOG.md` son satır(lar)ı.
2. `prompts/handoff/{ID}-report.md`.
3. Rapordaki "Karşılanan requirement/AC" alanında geçen `docs/11-acceptance-criteria.md` maddeleri.

### Yap

1. Raporun iddia ettiği test komutunu **kendin tekrar çalıştır**. Raporun yapıştırdığı çıktıya güvenme, doğrula.
2. Değişen dosyaları oku, rapordaki açıklamayla eşleşiyor mu kontrol et.
3. İlgili AC/FR gerçekten karşılanmış mı kontrol et (test var mı, failure path test edilmiş mi).
4. Scope taşması var mı bak (rapor dışında ilgisiz değişiklik).
5. `docs/17-traceability-risk-dod.md` DoD listesiyle karşılaştır.

### Karar

- **VERIFIED**: her şey tutarlı, testler gerçekten geçiyor → `SESSION_LOG.md`'ye `VERIFIED` satırı ekle, kullanıcıya kısa onay ver.
- **REJECTED**: tutarsızlık/eksik test/scope taşması var → `SESSION_LOG.md`'ye `REJECTED` satırı + net sebep ekle, düzeltme için yeni scoped task tanımla (`01-milestone-task.md` veya `05-bugfix.md` ile).

### Kısıtlar

- Kendi başına kod yazma/düzeltme yapma — sadece doğrula ve rapor et. Düzeltme ayrı oturumda, ayrı scoped task.
- Rapor yoksa veya test çıktısı yoksa otomatik REJECTED.
