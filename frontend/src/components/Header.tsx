interface Props {
  onOpenSettings: () => void
  activeModel?: string
  mode?: 'quick' | 'thorough'
  sessionTitle?: string
  onNewChat?: () => void
  interactionMode?: 'AUTO' | 'CHAT' | 'INVESTIGATION'
}

export function Header({ onOpenSettings, activeModel, mode = 'thorough', sessionTitle, onNewChat, interactionMode = 'AUTO' }: Props) {
  return (
    <header className="app-header">
      <div className="min-w-0">
        <p className="truncate font-display text-[15px] font-semibold text-ink">
          {sessionTitle || 'Yeni inceleme'}
        </p>
        <p className="mt-0.5 hidden text-[11px] text-ink-subtle sm:block">
          Kanıta dayalı OTP olay araştırması
        </p>
      </div>
      <div className="ml-auto flex items-center gap-2">
        {onNewChat && (
          <button type="button" onClick={onNewChat} className="header-action lg:hidden" aria-label="Yeni sohbet">
            <span aria-hidden="true">＋</span>
          </button>
        )}
        {activeModel && (
          <span className="header-chip hidden md:inline-flex">
            <span className="status-dot bg-confirm" /> {activeModel}
          </span>
        )}
        <span className="header-chip">{interactionMode === 'CHAT' ? 'Sohbet' : interactionMode === 'INVESTIGATION' ? 'İnceleme' : 'Otomatik'} · {mode === 'quick' ? 'Hızlı' : 'Detaylı'}</span>
        <button type="button" onClick={onOpenSettings} className="header-action" aria-label="Bilgi tabanı ve ayarlar">
          <span aria-hidden="true">☰</span>
          <span className="hidden sm:inline">Bilgi tabanı</span>
        </button>
      </div>
    </header>
  )
}
