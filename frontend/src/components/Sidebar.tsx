import { useMemo, useState } from 'react'
import type { SessionMeta } from '../lib/sessionStore'
import { UI_TEXT } from '../lib/labels'

interface Props {
  sessions: SessionMeta[]
  activeSessionId: string | null
  onSelect: (sessionId: string) => void
  onNewChat: () => void
  className?: string
}

export function Sidebar({ sessions, activeSessionId, onSelect, onNewChat, className = '' }: Props) {
  const [query, setQuery] = useState('')
  const visible = useMemo(
    () => sessions.filter((session) => session.title.toLocaleLowerCase('tr').includes(query.toLocaleLowerCase('tr'))),
    [sessions, query]
  )

  return (
    <aside className={`sidebar-shell ${className}`}>
      <div className="border-b border-white/10 px-4 pb-4 pt-5">
        <div className="mb-5 flex items-center gap-3 px-1">
          <div className="brand-mark">OS</div>
          <div>
            <p className="font-display text-[15px] font-semibold tracking-tight text-white">OTP Sentinel</p>
            <p className="text-[10px] uppercase tracking-[0.18em] text-sidebar-muted">Incident intelligence</p>
          </div>
        </div>
        <button type="button" onClick={onNewChat} className="new-chat-button">
          <span className="text-lg leading-none" aria-hidden="true">+</span>
          {UI_TEXT.newChat}
        </button>
      </div>

      <div className="px-3 pt-4">
        <label htmlFor="thread-search" className="sr-only">Sohbetlerde ara</label>
        <div className="thread-search">
          <span aria-hidden="true">⌕</span>
          <input
            id="thread-search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Sohbetlerde ara"
          />
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-5" aria-label="Sohbet geçmişi">
        <p className="mb-2 px-2 text-[10px] font-semibold uppercase tracking-[0.18em] text-sidebar-muted">İncelemeler</p>
        {visible.length === 0 && (
          <p className="px-2 py-8 text-center text-xs leading-5 text-sidebar-muted">{UI_TEXT.emptyThreadList}</p>
        )}
        <div className="space-y-1">
          {visible.map((session) => (
            <button
              key={session.sessionId}
              type="button"
              aria-current={session.sessionId === activeSessionId}
              onClick={() => onSelect(session.sessionId)}
              className={`thread-item ${session.sessionId === activeSessionId ? 'thread-item-active' : ''}`}
            >
              <span className="thread-icon" aria-hidden="true">◇</span>
              <span className="min-w-0 flex-1 truncate">{session.title}</span>
            </button>
          ))}
        </div>
      </nav>

      <div className="border-t border-white/10 p-4">
        <div className="flex items-center justify-between text-[11px] text-sidebar-muted">
          <span className="flex items-center gap-2"><span className="status-dot bg-confirm" /> Sistem hazır</span>
          <span>v0.1</span>
        </div>
      </div>
    </aside>
  )
}
