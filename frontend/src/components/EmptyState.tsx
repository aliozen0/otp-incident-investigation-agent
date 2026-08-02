const PROMPTS = [
  'Son 15 dakikada OTP başarı oranı neden düştü?',
  'Operatör B üzerindeki timeout artışını kanıtlarıyla incele.',
  'Kuyruk sağlıklıysa en olası alternatif neden nedir?',
]

export function EmptyState({ onPrompt }: { onPrompt: (prompt: string) => void }) {
  return (
    <div className="mx-auto flex min-h-full max-w-2xl flex-col justify-center py-12">
      <div className="mb-6 flex h-12 w-12 items-center justify-center rounded-xl border border-signal/20 bg-signal-soft font-display text-sm font-bold text-signal">OS</div>
      <p className="eyebrow">OTP OPERASYON ASİSTANI</p>
      <h1 className="mt-3 max-w-xl font-display text-3xl font-semibold tracking-[-0.035em] text-ink sm:text-4xl">
        Bir olayı kanıtlarıyla birlikte inceleyelim.
      </h1>
      <p className="mt-4 max-w-xl text-[15px] leading-7 text-ink-muted">
        Canlı operasyon araçlarını, geçmiş incident kayıtlarını ve runbook’ları güvenli bir araştırma akışında bir araya getirir.
      </p>
      <div className="mt-8 grid gap-2 sm:grid-cols-3">
        {PROMPTS.map((prompt) => (
          <button key={prompt} type="button" onClick={() => onPrompt(prompt)} className="prompt-card">
            <span>{prompt}</span>
            <span aria-hidden="true">↗</span>
          </button>
        ))}
      </div>
      <p className="mt-7 flex items-center gap-2 text-xs text-ink-subtle">
        <span className="status-dot bg-confirm" /> Salt-okunur araştırma · Aksiyonlar açık onay gerektirir
      </p>
    </div>
  )
}
