import { useEffect, useMemo, useRef, useState } from 'react'
import { Header } from './components/Header'
import { Sidebar } from './components/Sidebar'
import { SettingsPanel } from './components/SettingsPanel'
import { ChatMessage, type ChatTurn } from './components/ChatMessage'
import { ChatComposer } from './components/ChatComposer'
import { EmptyState } from './components/EmptyState'
import { createInvestigation, getModelCatalog, listSessionInvestigations } from './api/client'
import {
  listSessions,
  createSession,
  renameSession,
  recordQuestion,
  getRecordedQuestion,
  type SessionMeta,
} from './lib/sessionStore'
import { toUserMessage } from './lib/errors'
import type { InvestigationRequest, ModelOption } from './api/types'

export default function App() {
  const [sessions, setSessions] = useState<SessionMeta[]>([])
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null)
  const [turns, setTurns] = useState<ChatTurn[]>([])
  const [models, setModels] = useState<ModelOption[]>([])
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

  async function selectSession(sessionId: string) {
    setActiveSessionId(sessionId)
    try {
      const investigations = await listSessionInvestigations(sessionId)
      setTurns(
        investigations.map((investigation) => ({
          id: investigation.investigationId,
          question: getRecordedQuestion(sessionId, investigation.investigationId) ?? 'Önceki inceleme',
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
    const isFirstTurn = turns.length === 0
    setTurns((previous) => [...previous, { id: turnId, question, status: 'pending' }])
    setBusy(true)

    if (isFirstTurn) {
      renameSession(activeSessionId, question)
      setSessions(listSessions())
    }

    const request: InvestigationRequest = {
      question,
      timeWindow,
      sessionId: activeSessionId,
      modelId: modelId ?? undefined,
      mode,
      locale: 'tr-TR',
    }

    try {
      const investigation = await createInvestigation(request)
      recordQuestion(activeSessionId, investigation.investigationId, question)
      setTurns((previous) =>
        previous.map((turn) =>
          turn.id === turnId
            ? { ...turn, id: investigation.investigationId, status: 'done', investigation }
            : turn
        )
      )
    } catch (error) {
      setTurns((previous) =>
        previous.map((turn) =>
          turn.id === turnId
            ? { ...turn, status: 'error', errorMessage: toUserMessage(error) }
            : turn
        )
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="app-shell">
      <Sidebar
        sessions={sessions}
        activeSessionId={activeSessionId}
        onSelect={selectSession}
        onNewChat={handleNewChat}
        className="hidden lg:flex"
      />

      <section className="flex min-w-0 flex-1 flex-col bg-paper">
        <Header
          onOpenSettings={() => setSettingsOpen(true)}
          onNewChat={handleNewChat}
          activeModel={activeModel?.label}
          mode={mode}
          sessionTitle={activeSession?.title}
        />

        <main ref={scrollRef} className="conversation-scroll">
          <div className="mx-auto min-h-full w-full max-w-[940px] px-4 py-8 sm:px-8 lg:px-10">
            {turns.length === 0 ? (
              <EmptyState onPrompt={(prompt) => handleSubmit(prompt)} />
            ) : (
              <div className="space-y-10 pb-8">
                {turns.map((turn) => (
                  <ChatMessage key={turn.id} turn={turn} />
                ))}
              </div>
            )}
          </div>
        </main>

        <div className="composer-dock">
          <div className="mx-auto w-full max-w-[940px] px-4 pb-4 sm:px-8 lg:px-10">
            <ChatComposer
              disabled={busy}
              onSubmit={handleSubmit}
              models={models}
              modelId={modelId}
              onModelChange={setModelId}
              mode={mode}
              onModeChange={setMode}
            />
            <p className="mt-2 text-center text-[10px] tracking-wide text-ink-subtle">
              Çıktılar operasyonel kanıtlara dayanır. Değişiklik ve incident işlemleri açık onay gerektirir.
            </p>
          </div>
        </div>
      </section>

      <SettingsPanel
        modelId={modelId}
        onModelChange={setModelId}
        mode={mode}
        onModeChange={setMode}
        onClose={() => setSettingsOpen(false)}
        embedded
      />

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
