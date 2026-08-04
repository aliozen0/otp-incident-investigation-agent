import { IconActivity, IconChevronRight, IconLayers, IconShield, IconSparkles } from './icons'

const PROMPTS = [
  { text: 'Son 15 dakikada OTP başarı oranı neden düştü?', icon: IconActivity },
  { text: 'Operatör B üzerindeki timeout artışını kanıtlarıyla incele.', icon: IconLayers },
  { text: 'Kuyruk sağlıklıysa en olası alternatif neden nedir?', icon: IconSparkles },
]

export function EmptyState({ onPrompt }: { onPrompt: (prompt: string) => void }) {
  return (
    <div className="animate-turn mx-auto flex min-h-full max-w-2xl flex-col justify-center py-12">
      <div className="mb-6 flex h-12 w-12 items-center justify-center rounded-2xl border border-signal-tint bg-signal-soft text-signal">
        <IconShield size={22} />
      </div>
      <p className="eyebrow">OTP OPERASYON ASİSTANI</p>
      <h1 className="mt-3 max-w-xl font-display text-3xl font-semibold tracking-[-0.035em] text-ink sm:text-[38px] sm:leading-[1.15]">
        Bir olayı kanıtlarıyla birlikte inceleyelim.
      </h1>
      <p className="mt-4 max-w-xl text-[15px] leading-7 text-ink-muted">
        Canlı operasyon araçlarını, geçmiş incident kayıtlarını ve runbook’ları güvenli bir araştırma
        akışında bir araya getirir.
      </p>
      <div className="mt-8 grid gap-2.5 sm:grid-cols-3">
        {PROMPTS.map((prompt) => (
          <button key={prompt.text} type="button" onClick={() => onPrompt(prompt.text)} className="prompt-card">
            <span className="prompt-card-icon"><prompt.icon size={15} /></span>
            <span>{prompt.text}</span>
            <span className="flex items-center gap-1 text-[11px] font-medium text-signal">
              Başlat <IconChevronRight size={12} />
            </span>
          </button>
        ))}
      </div>
      <p className="mt-7 flex items-center gap-2 text-xs text-ink-subtle">
        <span className="status-dot status-dot-live bg-confirm" />
        Salt-okunur araştırma · Aksiyonlar açık onay gerektirir
      </p>
    </div>
  )
}
