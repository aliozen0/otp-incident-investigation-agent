import { UI_TEXT } from '../lib/labels'

export function Header({ onOpenSettings }: { onOpenSettings: () => void }) {
  return (
    <header className="border-b border-line px-6 py-3 flex items-center justify-between shrink-0">
      <div>
        <span className="font-display font-semibold text-lg tracking-tight">{UI_TEXT.appName}</span>
        <span className="ml-2 text-ink-muted text-sm">{UI_TEXT.appTagline}</span>
      </div>
      <button
        type="button"
        onClick={onOpenSettings}
        className="font-mono text-xs uppercase tracking-wide text-ink-muted border border-line rounded px-3 py-1.5 hover:bg-line/30"
      >
        {UI_TEXT.settings}
      </button>
    </header>
  )
}
