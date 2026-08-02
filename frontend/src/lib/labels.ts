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
  HIGH: 'Yüksek önem',
  CRITICAL: 'Kritik önem',
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

export const SOURCE_TYPE_LABEL_TR: Record<string, string> = {
  TOOL_RESULT: 'Araç sonucu',
  PROVIDER_HEALTH: 'Operatör sağlığı',
}

export const ACTION_TYPE_LABEL_TR: Record<string, string> = {
  MANUAL_CHECK: 'Manuel kontrol',
  CHANGE_PROPOSAL: 'Değişiklik önerisi',
  RESTART: 'Yeniden başlatma',
  ROLLBACK: 'Geri alma',
  CONFIG_CHANGE: 'Yapılandırma değişikliği',
}

export const ERROR_MESSAGE_TR: Record<string, string> = {
  INVALID_TIME_WINDOW:
    'Bu zaman aralığı geçerli değil — 1 dakika ile 24 saat arasında olmalı ve gelecekte olamaz.',
  INVALID_REQUEST: 'İstek işlenemedi. Soruyu kontrol edip tekrar deneyin.',
  QUESTION_NOT_ACTIONABLE:
    'Bu soru inceleme için yeterli bilgi içermiyor. Metrik ve zaman aralığı konusunda daha spesifik olmayı deneyin.',
  INVESTIGATION_RATE_LIMITED:
    'Kısa sürede çok fazla inceleme başlatıldı. Biraz bekleyip tekrar deneyin.',
  MODEL_PROVIDER_ERROR:
    'Analiz modeli şu anda kullanılamıyor. Bu canlı bir bağımlılık sorunu, hata değil — kısa süre sonra tekrar deneyin.',
  INVESTIGATION_TIMEOUT: 'İnceleme çok uzun sürdü ve zaman aşımına uğradı.',
  INVESTIGATION_NOT_ACTIONABLE:
    'Bu inceleme doğrulamadan geçemedi, bu yüzden henüz üzerinde bir olay aksiyonu alınamaz.',
  INVESTIGATION_NOT_FOUND:
    'Bu inceleme bulunamadı. Süresi dolmuş veya bağlantı hatalı olabilir.',
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
