import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Header } from './components/Header'
import { Sidebar } from './components/Sidebar'
import { SettingsPanel } from './components/SettingsPanel'
import { ChatMessage, type ChatTurn } from './components/ChatMessage'
import { ChatComposer } from './components/ChatComposer'
import { EmptyState } from './components/EmptyState'
import { DataExplorer } from './components/DataExplorer'
import { IconArrowDown, IconCheck } from './components/icons'
import { getModelCatalog, listSessionInvestigations, sendChatMessage } from './api/client'
import {
  listSessions,
  createSession,
  renameSession,
  deleteSession,
  recordQuestion,
  getRecordedQuestion,
  loadTurns,
  saveTurns,
  type SessionMeta,
} from './lib/sessionStore'
import { toUserMessage } from './lib/errors'
import type { ChatMessageRequest, InteractionMode, ModelOption } from './api/types'

const SIDEBAR_KEY = 'otp-sentinel:sidebar-collapsed'

export default function App() {
  const [sessions, setSessions] = useState<SessionMeta[]>([])
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null)
  const [turns, setTurns] = useState<ChatTurn[]>([])
  const [models, setModels] = useState<ModelOption[]>([])
  const [modelId, setModelId] = useState<string | null>(null)
  const [mode, setMode] = useState<'quick' | 'thorough'>('thorough')
  const [interactionMode, setInteractionMode] = useState<InteractionMode>('AUTO')
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [dataOpen, setDataOpen] = useState(false)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(
    () => localStorage.getItem(SIDEBAR_KEY) === '1'
  )
  const [atBottom, setAtBottom] = useState(true)
  const [toast, setToast] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const existing = listSessions()
    if (existing.length > 0) {
      setSessions(existing)
      selectSession(existing[0].sessionId)
    } else {
      handleNewChat()
    }
    getModelCatalog()
      .then((catalog) => {
        setModels(catalog.options.filter((option) => option.verified))
        setModelId((current) => current ?? catalog.defaultModelId)
      })
      .catch(() => {
        setModels([])
      })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    scrollRef.current?.scrollTo?.({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [turns])

  useEffect(() => {
    localStorage.setItem(SIDEBAR_KEY, sidebarCollapsed ? '1' : '0')
  }, [sidebarCollapsed])

  useEffect(() => {
    if (!toast) return
    const timer = setTimeout(() => setToast(null), 2600)
    return () => clearTimeout(timer)
  }, [toast])

  const activeSession = useMemo(
    () => sessions.find((session) => session.sessionId === activeSessionId),
    [sessions, activeSessionId]
  )
  const activeModel = models.find((model) => model.id === modelId)

  function handleNewChat() {
    const session = createSession()
    setSessions(listSessions())
    setActiveSessionId(session.sessionId)
    setTurns([])
  }

  const selectSession = useCallback(async (sessionId: string) => {
    setActiveSessionId(sessionId)
    const localTurns = loadTurns(sessionId)
    if (localTurns.length > 0) {
      setTurns(localTurns)
      return
    }
    try {
      const investigations = await listSessionInvestigations(sessionId)
      const restored: ChatTurn[] = investigations.map((investigation) => ({
        id: investigation.investigationId,
        question: getRecordedQuestion(sessionId, investigation.investigationId) ?? 'Önceki inceleme',
        kind: 'investigation' as const,
        assistantMessage: investigation.summary,
        investigation,
      }))
      setTurns(restored)
      saveTurns(sessionId, restored)
    } catch {
      setTurns([])
    }
  }, [])

  function handleRenameSession(sessionId: string, title: string) {
    renameSession(sessionId, title)
    setSessions(listSessions())
  }

  function handleDeleteSession(sessionId: string) {
    deleteSession(sessionId)
    const remaining = listSessions()
    setSessions(remaining)
    setToast('Sohbet silindi')
    if (sessionId !== activeSessionId) return
    if (remaining.length > 0) void selectSession(remaining[0].sessionId)
    else handleNewChat()
  }

  // Global shortcuts keep the console usable without leaving the keyboard.
  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      const meta = event.ctrlKey || event.metaKey
      if (!meta) return
      if (event.key.toLowerCase() === 'k') {
        event.preventDefault()
        setSidebarCollapsed(false)
        setTimeout(() => document.getElementById('thread-search')?.focus(), 60)
      } else if (event.shiftKey && event.key.toLowerCase() === 'o') {
        event.preventDefault()
        handleNewChat()
      } else if (event.key.toLowerCase() === 'b') {
        event.preventDefault()
        setSidebarCollapsed((value) => !value)
      } else if (event.key === ',') {
        event.preventDefault()
        setSettingsOpen(true)
      } else if (event.key.toLowerCase() === 'd') {
        event.preventDefault()
        setDataOpen(true)
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])

  function handleScroll() {
    const node = scrollRef.current
    if (!node) return
    setAtBottom(node.scrollHeight - node.scrollTop - node.clientHeight < 120)
  }

  async function handleSubmit(question: string, timeWindow?: { startAt: string; endAt: string }) {
    if (!activeSessionId) return
    const turnId = crypto.randomUUID()
    const isFirstTurn = turns.length === 0
    setTurns((previous) => [...previous, { id: turnId, question, kind: 'pending' }])
    setBusy(true)

    if (isFirstTurn) {
      renameSession(activeSessionId, question)
      setSessions(listSessions())
    }

    if (!modelId) {
      setTurns((previous) => previous.map((turn) => turn.id === turnId
        ? { kind: 'error', id: turnId, question, errorMessage: 'Doğrulanmış model yüklenemedi.' }
        : turn))
      setBusy(false)
      return
    }

    const request: ChatMessageRequest = {
      message: question,
      timeWindow,
      sessionId: activeSessionId,
      modelId,
      interactionMode,
      investigationMode: mode === 'quick' ? 'QUICK' : 'THOROUGH',
      locale: 'tr-TR',
    }

    try {
      const response = await sendChatMessage(request)
      if (response.investigation) {
        recordQuestion(activeSessionId, response.investigation.investigationId, question)
      }
      setTurns((previous) => {
        const next = previous.map((turn): ChatTurn => {
          if (turn.id !== turnId) return turn
          if (response.responseType === 'INVESTIGATION' && response.investigation) {
            return { kind: 'investigation', id: response.investigation.investigationId, question,
              assistantMessage: response.assistantMessage, suggestions: response.suggestions,
              investigation: response.investigation }
          }
          return { kind: response.responseType === 'CLARIFICATION' ? 'clarification' : 'chat',
            id: response.messageId, question, assistantMessage: response.assistantMessage,
            suggestions: response.suggestions }
        })
        saveTurns(activeSessionId, next)
        return next
      })
    } catch (error) {
      setTurns((previous) => {
        const next = previous.map((turn): ChatTurn => turn.id === turnId
          ? { kind: 'error', id: turnId, question, errorMessage: toUserMessage(error) }
          : turn)
        saveTurns(activeSessionId, next)
        return next
      })
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="app-shell">
      <Sidebar
        sessions={sessions}
        activeSessionId={activeSessionId}
        onSelect={(sessionId) => {
          void selectSession(sessionId)
          if (window.innerWidth < 1024) setSidebarCollapsed(true)
        }}
        onNewChat={handleNewChat}
        onRename={handleRenameSession}
        onDelete={handleDeleteSession}
        collapsed={sidebarCollapsed}
        onToggleCollapse={() => setSidebarCollapsed((value) => !value)}
      />

      {!sidebarCollapsed && (
        <div
          className="sidebar-backdrop lg:hidden"
          onClick={() => setSidebarCollapsed(true)}
          aria-hidden="true"
        />
      )}

      <section className="flex min-w-0 flex-1 flex-col bg-paper">
        <Header
          onOpenSettings={() => setSettingsOpen(true)}
          onOpenData={() => setDataOpen(true)}
          onNewChat={handleNewChat}
          activeModel={activeModel?.label}
          mode={mode}
          interactionMode={interactionMode}
          sessionTitle={activeSession?.title}
          sidebarCollapsed={sidebarCollapsed}
          onExpandSidebar={() => setSidebarCollapsed((value) => !value)}
        />

        <main ref={scrollRef} onScroll={handleScroll} className="conversation-scroll scroll-thin">
          <div className="mx-auto min-h-full w-full max-w-[940px] px-4 py-8 sm:px-8 lg:px-10">
            {turns.length === 0 ? (
              <EmptyState onPrompt={(prompt) => handleSubmit(prompt)} />
            ) : (
              <div className="space-y-10 pb-8">
                {turns.map((turn) => (
                  <ChatMessage key={turn.id} turn={turn} onSuggestion={(text) => handleSubmit(text)} />
                ))}
              </div>
            )}
          </div>

          {!atBottom && turns.length > 0 && (
            <button
              type="button"
              className="scroll-bottom-button animate-pop"
              onClick={() =>
                scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
              }
            >
              <IconArrowDown size={13} /> En alta git
            </button>
          )}
        </main>

        <div className="composer-dock">
          <div className="mx-auto w-full max-w-[940px] px-4 pb-4 sm:px-8 lg:px-10">
            <ChatComposer
              disabled={busy || !modelId}
              onSubmit={handleSubmit}
              models={models}
              modelId={modelId}
              onModelChange={setModelId}
              mode={mode}
              onModeChange={setMode}
              interactionMode={interactionMode}
              onInteractionModeChange={setInteractionMode}
            />
            <p className="mt-2 text-center text-[10.5px] tracking-wide text-ink-subtle">
              Çıktılar operasyonel kanıtlara dayanır. Değişiklik ve incident işlemleri açık onay gerektirir.
            </p>
          </div>
        </div>
      </section>

      {settingsOpen && (
        <SettingsPanel
          modelId={modelId}
          onModelChange={setModelId}
          mode={mode}
          onModeChange={setMode}
          onClose={() => setSettingsOpen(false)}
          interactionMode={interactionMode}
        />
      )}

      {dataOpen && <DataExplorer onClose={() => setDataOpen(false)} />}

      {toast && (
        <div className="toast-stack">
          <div className="toast is-success" role="status">
            <IconCheck size={15} /> {toast}
          </div>
        </div>
      )}
    </div>
  )
}
