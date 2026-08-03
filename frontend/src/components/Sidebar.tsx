import { useMemo, useState } from 'react'
import type { SessionMeta } from '../lib/sessionStore'
import { UI_TEXT } from '../lib/labels'
import {
  IconCheck,
  IconMessage,
  IconPencil,
  IconPlus,
  IconSearch,
  IconShield,
  IconSidebarCollapse,
  IconTrash,
} from './icons'

interface Props {
  sessions: SessionMeta[]
  activeSessionId: string | null
  onSelect: (sessionId: string) => void
  onNewChat: () => void
  collapsed?: boolean
  onToggleCollapse?: () => void
  onRename?: (sessionId: string, title: string) => void
  onDelete?: (sessionId: string) => void
  className?: string
}

const DAY = 86_400_000

// Thread history reads better bucketed by recency than as one flat list.
function bucketOf(createdAt: string): string {
  const age = Date.now() - new Date(createdAt).getTime()
  if (age < DAY) return 'Bugün'
  if (age < 2 * DAY) return 'Dün'
  if (age < 7 * DAY) return 'Son 7 gün'
  return 'Daha önce'
}

export function Sidebar({
  sessions,
  activeSessionId,
  onSelect,
  onNewChat,
  collapsed = false,
  onToggleCollapse,
  onRename,
  onDelete,
  className = '',
}: Props) {
  const [query, setQuery] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [draftTitle, setDraftTitle] = useState('')

  const groups = useMemo(() => {
    const needle = query.toLocaleLowerCase('tr')
    const visible = sessions.filter((session) => session.title.toLocaleLowerCase('tr').includes(needle))
    const order = ['Bugün', 'Dün', 'Son 7 gün', 'Daha önce']
    const map = new Map<string, SessionMeta[]>()
    for (const session of visible) {
      const bucket = bucketOf(session.createdAt)
      map.set(bucket, [...(map.get(bucket) ?? []), session])
    }
    return order.filter((bucket) => map.has(bucket)).map((bucket) => [bucket, map.get(bucket)!] as const)
  }, [sessions, query])

  const total = groups.reduce((sum, [, items]) => sum + items.length, 0)

  function commitRename(sessionId: string) {
    const trimmed = draftTitle.trim()
    if (trimmed) onRename?.(sessionId, trimmed)
    setEditingId(null)
  }

  return (
    <aside className={`sidebar-shell ${collapsed ? 'is-collapsed' : ''} ${className}`} aria-label="Sohbet paneli">
      <div className="flex items-center gap-2.5 px-4 pb-4 pt-4">
        <div className="brand-mark">OS</div>
        <div className="collapse-hide min-w-0 flex-1">
          <p className="truncate font-display text-[14.5px] font-semibold tracking-tight text-ink">
            {UI_TEXT.appName}
          </p>
          <p className="truncate text-[10px] uppercase tracking-[0.16em] text-ink-subtle">
            {UI_TEXT.appTagline}
          </p>
        </div>
        {onToggleCollapse && (
          <button
            type="button"
            onClick={onToggleCollapse}
            className="icon-button collapse-hide"
            aria-label="Paneli daralt"
            title="Paneli daralt (Ctrl+B)"
          >
            <IconSidebarCollapse size={17} />
          </button>
        )}
      </div>

      <div className="px-3">
        <button
          type="button"
          onClick={onNewChat}
          className="new-chat-button tip"
          data-tip={UI_TEXT.newChat}
          title={`${UI_TEXT.newChat} (Ctrl+Shift+O)`}
        >
          <IconPlus size={16} />
          <span className="collapse-hide">{UI_TEXT.newChat}</span>
        </button>
      </div>

      <div className="collapse-hide px-3 pt-3">
        <label htmlFor="thread-search" className="sr-only">Sohbetlerde ara</label>
        <div className="thread-search">
          <IconSearch size={14} />
          <input
            id="thread-search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Sohbetlerde ara"
          />
          <span className="kbd" aria-hidden="true">Ctrl K</span>
        </div>
      </div>

      <nav className="scroll-thin flex-1 overflow-y-auto px-3 pb-3" aria-label="Sohbet geçmişi">
        {total === 0 && (
          <p className="collapse-hide px-2 py-10 text-center text-xs leading-5 text-ink-subtle">
            {query ? 'Eşleşen sohbet yok' : UI_TEXT.emptyThreadList}
          </p>
        )}

        {groups.map(([bucket, items]) => (
          <div key={bucket}>
            <p className="group-label collapse-hide">{bucket}</p>
            <div className="space-y-0.5">
              {items.map((session) => {
                const isActive = session.sessionId === activeSessionId
                if (editingId === session.sessionId) {
                  return (
                    <div key={session.sessionId} className="px-1 py-1">
                      <input
                        autoFocus
                        value={draftTitle}
                        onChange={(event) => setDraftTitle(event.target.value)}
                        onBlur={() => commitRename(session.sessionId)}
                        onKeyDown={(event) => {
                          if (event.key === 'Enter') commitRename(session.sessionId)
                          if (event.key === 'Escape') setEditingId(null)
                        }}
                        className="thread-rename-input"
                        aria-label="Sohbet adını düzenle"
                      />
                    </div>
                  )
                }
                return (
                  <div key={session.sessionId} className={`thread-row ${isActive ? 'is-active' : ''}`}>
                    <button
                      type="button"
                      aria-current={isActive}
                      onClick={() => onSelect(session.sessionId)}
                      onDoubleClick={() => {
                        setDraftTitle(session.title)
                        setEditingId(session.sessionId)
                      }}
                      className="thread-item tip"
                      data-tip={session.title}
                      title={session.title}
                    >
                      <span className="thread-icon"><IconMessage size={15} /></span>
                      <span className="collapse-hide min-w-0 flex-1 truncate">{session.title}</span>
                    </button>
                    <div className="thread-actions collapse-hide">
                      {onRename && (
                        <button
                          type="button"
                          className="thread-action"
                          aria-label={`${session.title} sohbetini yeniden adlandır`}
                          onClick={() => {
                            setDraftTitle(session.title)
                            setEditingId(session.sessionId)
                          }}
                        >
                          <IconPencil size={13} />
                        </button>
                      )}
                      {onDelete && (
                        <button
                          type="button"
                          className="thread-action is-danger"
                          aria-label={`${session.title} sohbetini sil`}
                          onClick={() => onDelete(session.sessionId)}
                        >
                          <IconTrash size={13} />
                        </button>
                      )}
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        ))}
      </nav>

      <div className="border-t border-line px-4 py-3">
        <div className="flex items-center gap-2 text-[11px] text-ink-subtle">
          <span className="status-dot status-dot-live bg-confirm" />
          <span className="collapse-hide flex-1">Salt-okunur araştırma</span>
          <span className="collapse-hide inline-flex items-center gap-1 text-ink-subtle">
            <IconShield size={13} /> v0.1
          </span>
        </div>
      </div>

      {editingId && (
        <span className="sr-only" role="status">
          <IconCheck size={12} /> Yeniden adlandırma açık
        </span>
      )}
    </aside>
  )
}
