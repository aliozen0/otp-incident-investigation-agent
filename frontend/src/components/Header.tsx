import { INTERACTION_MODE_LABEL_TR, MODE_LABEL_TR } from '../lib/labels'
import type { InteractionMode } from '../api/types'
import { IconActivity, IconBook, IconPlus, IconSidebarExpand, IconSparkles } from './icons'

interface Props {
  onOpenSettings: () => void
  onOpenData?: () => void
  activeModel?: string
  mode?: 'quick' | 'thorough'
  sessionTitle?: string
  onNewChat?: () => void
  interactionMode?: InteractionMode
  sidebarCollapsed?: boolean
  onExpandSidebar?: () => void
}

export function Header({
  onOpenSettings,
  onOpenData,
  activeModel,
  mode = 'thorough',
  sessionTitle,
  onNewChat,
  interactionMode = 'AUTO',
  sidebarCollapsed = false,
  onExpandSidebar,
}: Props) {
  return (
    <header className="app-header">
      {onExpandSidebar && (
        <button
          type="button"
          onClick={onExpandSidebar}
          className={`icon-button ${sidebarCollapsed ? '' : 'lg:hidden'}`}
          aria-label="Paneli aç"
          title="Paneli aç (Ctrl+B)"
        >
          <IconSidebarExpand size={17} />
        </button>
      )}

      <div className="min-w-0">
        <p className="truncate font-display text-[15px] font-semibold tracking-tight text-ink">
          {sessionTitle || 'Yeni inceleme'}
        </p>
        <p className="mt-0.5 hidden truncate text-[11px] text-ink-subtle sm:block">
          Kanıta dayalı OTP olay araştırması
        </p>
      </div>

      <div className="ml-auto flex items-center gap-2">
        {onNewChat && (
          <button type="button" onClick={onNewChat} className="icon-button is-bordered lg:hidden" aria-label="Yeni sohbet">
            <IconPlus size={16} />
          </button>
        )}
        {activeModel && (
          <span className="header-chip hidden md:inline-flex" title="Aktif analiz modeli">
            <span className="status-dot status-dot-live bg-confirm" />
            <strong>{activeModel}</strong>
          </span>
        )}
        <span className="header-chip hidden sm:inline-flex" title="Etkileşim ve analiz modu">
          <IconSparkles size={13} />
          {INTERACTION_MODE_LABEL_TR[interactionMode]}
          {interactionMode !== 'CHAT' && ` · ${MODE_LABEL_TR[mode]}`}
        </span>
        {onOpenData && (
          <button type="button" onClick={onOpenData} className="header-action" aria-label="Operasyon verileri" title="Veri gezgini (Ctrl+D)">
            <IconActivity size={15} />
            <span className="hidden sm:inline">Veri</span>
          </button>
        )}
        <button type="button" onClick={onOpenSettings} className="header-action" aria-label="Bilgi tabanı ve ayarlar" title="Ayarlar (Ctrl+,)">
          <IconBook size={15} />
          <span className="hidden sm:inline">Bilgi tabanı</span>
        </button>
      </div>
    </header>
  )
}
