import { useEffect, useRef, useState } from 'react'
import { Header } from './components/Header'
import { Sidebar } from './components/Sidebar'
import { SettingsPanel } from './components/SettingsPanel'
import { ChatMessage, type ChatTurn } from './components/ChatMessage'
import { ChatComposer } from './components/ChatComposer'
import { createInvestigation, listSessionInvestigations } from './api/client'
import {
  listSessions,
  createSession,
  renameSession,
  recordQuestion,
  getRecordedQuestion,
  type SessionMeta,
} from './lib/sessionStore'
import { toUserMessage } from './lib/errors'
import type { InvestigationRequest } from './api/types'

export default function App() {
  const [sessions, setSessions] = useState<SessionMeta[]>([])
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null)
  const [turns, setTurns] = useState<ChatTurn[]>([])
  const [modelId, setModelId] = useState<string | null>(null)
  const [mode, setMode] = useState<'quick' | 'thorough'>('thorough')
  const [settingsOpen, setSettingsOpen] = useState(false)
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    scrollRef.current?.scrollTo?.({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [turns])

  function handleNewChat() {
    const session = createSession()
    setSessions(listSessions())
    setActiveSessionId(session.sessionId)
    setTurns([])
  }

  async function selectSession(sessionId: string) {
    setActiveSessionId(sessionId)
    try {
      const investigations = await listSessionInvestigations(sessionId)
      setTurns(
        investigations.map((investigation) => ({
          id: investigation.investigationId,
          question: getRecordedQuestion(sessionId, investigation.investigationId) ?? '—',
          status: 'done' as const,
          investigation,
        }))
      )
    } catch {
      setTurns([])
    }
  }

  async function handleSubmit(question: string, timeWindow?: { startAt: string; endAt: string }) {
    if (!activeSessionId) return
    const turnId = crypto.randomUUID()
    setTurns((prev) => [...prev, { id: turnId, question, status: 'pending' }])
    setBusy(true)

    if (turns.length === 0) {
      renameSession(activeSessionId, question)
      setSessions(listSessions())
    }

    const req: InvestigationRequest = {
      question,
      timeWindow,
      sessionId: activeSessionId,
      modelId: modelId ?? undefined,
      mode,
    }

    try {
      const investigation = await createInvestigation(req)
      recordQuestion(activeSessionId, investigation.investigationId, question)
      setTurns((prev) =>
        prev.map((t) =>
          t.id === turnId ? { ...t, id: investigation.investigationId, status: 'done', investigation } : t
        )
      )
    } catch (err) {
      setTurns((prev) =>
        prev.map((t) => (t.id === turnId ? { ...t, status: 'error', errorMessage: toUserMessage(err) } : t))
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="h-screen bg-paper text-ink font-body flex flex-col">
      <Header onOpenSettings={() => setSettingsOpen(true)} />
      <div className="flex flex-1 min-h-0">
        <Sidebar
          sessions={sessions}
          activeSessionId={activeSessionId}
          onSelect={selectSession}
          onNewChat={handleNewChat}
        />
        <main className="flex-1 flex flex-col min-h-0">
          <div ref={scrollRef} className="flex-1 overflow-y-auto px-6 py-6 max-w-[880px] w-full mx-auto">
            {turns.map((turn) => (
              <div key={turn.id} className="mb-6">
                <ChatMessage turn={turn} />
              </div>
            ))}
          </div>
          <div className="px-6 pb-6 max-w-[880px] w-full mx-auto shrink-0">
            <ChatComposer disabled={busy} onSubmit={handleSubmit} />
          </div>
        </main>
      </div>
      {settingsOpen && (
        <SettingsPanel
          modelId={modelId}
          onModelChange={setModelId}
          mode={mode}
          onModeChange={setMode}
          onClose={() => setSettingsOpen(false)}
        />
      )}
    </div>
  )
}
