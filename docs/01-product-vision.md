# 01 — Product Vision

## Vizyon

> Telekom operasyon ekiplerinin yalnızca alarm görmesini değil, alarmın nedenini kanıta dayalı biçimde araştırmasını sağlayan güvenli bir AI operasyon yardımcısı.

## Değer önerisi

OTP Sentinel:

- farklı operasyon sinyallerini tek araştırmada birleştirir,
- geçmiş incident bilgisini kurumsal hafızaya dönüştürür,
- kanıt ile tahmini ayırır,
- bir sonraki kontrolü önerir,
- kritik aksiyonları insan kontrolünde tutar.

## Ürün konumlandırması

Bu ürün:

- metric dashboard değildir,
- log arama motoru değildir,
- basit chatbot değildir,
- tam otonom SRE değildir.

Mevcut operasyon sistemlerinin üzerinde çalışan **agentic investigation layer**'dır.

## Ana kullanıcı sonucu

Kullanıcı:

> “Son 15 dakikada OTP teslimat oranı neden düştü?”

diye sorduğunda sistem:

1. olayı doğrular,
2. etkiyi sayısallaştırır,
3. provider yoğunlaşmasını gösterir,
4. normal bileşenleri de kanıt olarak sunar,
5. en fazla üç hipotez sıralar,
6. her hipotezi evidence ile bağlar,
7. güvenli kontrol adımları önerir,
8. incident taslağı için onay ister.

## Ürün ilkeleri

### Evidence first

Her operasyonel gerçek bir source reference taşır.

### Hypothesis, not certainty

Kök neden doğrulanmadıysa “kesin neden” dili kullanılmaz.

### Human-controlled action

MVP restart, rollback veya config değişikliği yapmaz.

### Hybrid intelligence

Belirsiz yorum ve tool seçimi AI'a; politika, onay ve write işlemleri deterministik koda aittir.

### Enterprise compatibility

Sistem bağımsız REST API'dir. Mevcut Java veya PHP sistemlerinin yeniden yazılmasını gerektirmez.

### Observable AI

Tool çağrıları, kaynaklar, model sürümü, token kullanımı ve validation sonucu izlenebilir olmalıdır.

## MVP ve gelecek ayrımı

### MVP

- Tek olay: OTP teslimat düşüşü
- Tek write: onaylı incident taslağı

### Gelecek vizyonu

- Toplu SMS gecikme inceleme
- Provider routing önerisi
- Çağrı merkezi kuyruk analizi
- Deployment risk korelasyonu
- Onaylı remediation playbook'ları

## North-star metrik

> Kanıta dayalı ilk incident değerlendirmesinin hazırlanma süresi.

Demo hedefi: 30 saniye altında yapılandırılmış ilk değerlendirme.
