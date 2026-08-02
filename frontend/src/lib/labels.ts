import type { InvestigationStatus, Severity, Hypothesis } from '../api/types'

export const STATUS_LABEL_TR: Record<InvestigationStatus, string> = {
  ANOMALY_CONFIRMED: 'Anomali doğrulandı',
  NO_ANOMALY: 'Anomali tespit edilmedi',
  PARTIAL_ANALYSIS: 'Kısmi analiz',
  FAILED: 'Analiz başarısız oldu',
}

export const SEVERITY_LABEL_TR: Record<Severity, string> = {
  LOW: 'Düşük önem',
  MEDIUM: 'Orta önem',
  HIGH: 'Kritik önem',
  CRITICAL: 'Acil önem',
}

export const PROBABILITY_LABEL_TR: Record<Hypothesis['probability'], string> = {
  HIGH: 'Yüksek olasılık',
  MEDIUM: 'Orta olasılık',
  LOW: 'Düşük olasılık',
}

export const MODE_LABEL_TR: Record<'quick' | 'thorough', string> = {
  quick: 'Hızlı',
  thorough: 'Detaylı',
}

export const DOCUMENT_TYPE_LABEL_TR: Record<string, string> = {
  INCIDENT_POSTMORTEM: 'Olay sonrası analiz (postmortem)',
  RUNBOOK: 'Runbook',
  ERROR_REFERENCE: 'Hata referansı',
  PROVIDER_PLAYBOOK: 'Operatör oyun kitabı',
  CHANGE_POLICY: 'Değişiklik politikası',
}

export const UI_TEXT = {
  appName: 'OTP Sentinel',
  appTagline: 'olay inceleme konsolu',
  newChat: 'Yeni sohbet',
  emptyThreadList: 'Henüz sohbet yok',
  composerPlaceholder: 'Ne araştırmak istersiniz? (Enter ile gönder, Shift+Enter yeni satır)',
  send: 'Gönder',
  investigating: 'İnceleniyor…',
  investigatingDetail:
    'Canlı metrik, hata, kuyruk ve operatör verileri toplanıyor, ardından geçmiş olaylarla karşılaştırılıyor. Gerçek bir analiz bir dakikaya kadar sürebilir — bu önbelleğe alınmış bir sonuç değildir.',
  settings: 'Ayarlar',
  closeSettings: 'Kapat',
  modelLabel: 'Model',
  modeLabel: 'Mod',
  knowledgeSectionTitle: 'Bilgi tabanı',
  knowledgeListEmpty: 'Henüz yüklenmiş belge yok.',
  uploadTitle: 'Belge yükle',
  uploadTitleField: 'Başlık',
  uploadTypeField: 'Tür',
  uploadProviderField: 'Operatör (opsiyonel)',
  uploadTagsField: 'Etiketler (virgülle ayırın, opsiyonel)',
  uploadEffectiveFromField: 'Geçerlilik başlangıcı',
  uploadEffectiveToField: 'Geçerlilik bitişi (opsiyonel)',
  uploadLanguageField: 'Dil (opsiyonel, varsayılan tr)',
  uploadContentField: 'İçerik',
  uploadSubmit: 'Yükle',
  uploadSuccess: 'Belge yüklendi',
  timeWindowToggle: 'Zaman aralığı belirt (aksi halde sorudan çözülür)',
  timeWindowStart: 'Başlangıç',
  timeWindowEnd: 'Bitiş',
  evidenceSection: 'Kanıtlar',
  hypothesesSection: 'Hipotezler',
  actionsSection: 'Önerilen aksiyonlar',
  knowledgeRefsSection: 'İlgili geçmiş olaylar',
  confidenceLabel: 'Güven',
  noEvidence: 'Kanıt toplanmadı.',
  noHypotheses: 'Hipotez üretilmedi.',
  noActions: 'Aksiyon önerilmedi.',
  noKnowledgeRefs: 'Benzer geçmiş olay bulunamadı.',
  approvalRequired: 'onay gerekli',
  riskLabel: 'risk',
  errorTitle: 'İnceleme başlatılamadı',
} as const

export function formatNumber(n: number, fractionDigits = 2): string {
  return new Intl.NumberFormat('tr-TR', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(n)
}

export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat('tr-TR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(iso))
}
