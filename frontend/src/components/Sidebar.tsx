import type { SessionMeta } from '../lib/sessionStore'
import { UI_TEXT } from '../lib/labels'

interface Props {
  sessions: SessionMeta[]
  activeSessionId: string | null
  onSelect: (sessionId: string) => void
  onNewChat: () => void
}

export function Sidebar({ sessions, activeSessionId, onSelect, onNewChat }: Props) {
  return (
    <aside className="w-64 shrink-0 border-r border-line flex flex-col h-full">
      <div className="p-3 border-b border-line">
        <button
          type="button"
          onClick={onNewChat}
          className="w-full border border-signal text-signal font-display text-sm px-3 py-2 rounded-md hover:bg-signal-soft"
        >
          {UI_TEXT.newChat}
        </button>
      </div>
      <nav className="flex-1 overflow-y-auto p-2 space-y-1">
        {sessions.length === 0 && (
          <p className="text-xs text-ink-muted px-2 py-4 text-center">{UI_TEXT.emptyThreadList}</p>
        )}
        {sessions.map((s) => (
          <button
            key={s.sessionId}
            type="button"
            aria-current={s.sessionId === activeSessionId}
            onClick={() => onSelect(s.sessionId)}
            className={`w-full text-left truncate text-sm px-3 py-2 rounded-md ${
              s.sessionId === activeSessionId
                ? 'bg-signal-soft text-signal'
                : 'text-ink hover:bg-line/40'
            }`}
          >
            {s.title}
          </button>
        ))}
      </nav>
    </aside>
  )
}
