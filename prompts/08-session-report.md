## Oturum sonu raporu — ZORUNLU

Task bitince (veya bloklanınca), oturumu kapatmadan önce:

1. `prompts/handoff/{ID}-report.md` dosyasını aşağıdaki şablonla yaz.
2. `SESSION_LOG.md`'ye tek satır ekle (append, üzerine yazma).

Rapor doğrulanmadan "tamamlandı" sayılmaz — bir sonraki oturum bunu okuyup gerçekten test çalıştırarak doğrulayacak.

### Rapor şablonu (`prompts/handoff/{ID}-report.md`)

```markdown
# {ID} — {BASLIK}

## Durum
DONE / BLOCKED / FAILED

## Kapsam
{ne yapıldı, tek paragraf}

## Değişen dosyalar
- {path} — {ne değişti}

## Testler (gerçek komut + gerçek çıktı, iddia değil)
Komut: `{örn: mvn -pl ... test}`
Çıktı özeti:
```
{gerçek test çıktısından kopyala-yapıştır, en az pass/fail sayısı}
```

## Karşılanan requirement/AC
- {FR/AC id} — {nasıl karşılandı}

## Karşılanmayan / ertelenen
- {varsa, neden}

## Spec çelişkisi/belirsizlik (varsa)
{07-spec-conflict.md ile raporlandıysa link/özet}

## Sonraki oturum için not
{tek cümle, ör: "M2'ye devam edilebilir" / "şu AC bloklu, onay bekliyor"}
```

### Kural

- "Testler geçti" yazıp gerçek çıktı yapıştırmamak yasak (bkz. `AGENTS.md`: "Do not claim tests passed unless they were executed.").
- Rapor eksikse bir sonraki oturum bu görevi VERIFIED sayamaz.
- Git: `docs/20-git-workflow.md`'deki branch adı (`milestone/{ID}-...` veya `fix/...`) ve commit convention'a uy. `main`'e doğrudan commit/push yok.

