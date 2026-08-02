import { useEffect, useMemo, useRef, useState } from 'react'
import { Header } from './components/Header'
import { Sidebar } from './components/Sidebar'
import { SettingsPanel } from './components/SettingsPanel'
import { ChatMessage, type ChatTurn } from './components/ChatMessage'
import { ChatComposer } from './components/ChatComposer'
import { EmptyState } from './components/EmptyState'
import { getModelCatalog, listSessionInvestigations, sendChatMessage } from './api/client'
import {
  listSessions,
  createSession,
  renameSession,
  recordQuestion,
  getRecordedQuestion,
  loadTurns,
  saveTurns,
  type SessionMeta,
} from './lib/sessionStore'
import { toUserMessage } from './lib/errors'
import type { ChatMessageRequest, InteractionMode, ModelOption } from './api/types'

export default function App() {
  const [sessions, setSessions] = useState<SessionMeta[]>([])
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null)
  const [turns, setTurns] = useState<ChatTurn[]>([])
  const [models, setModels] = useState<ModelOption[]>([])
  const [modelId, setModelId] = useState<string | null>(null)
  const [mode, setMode] = useState<'quick' | 'thorough'>('thorough')
  const [interactionMode, setInteractionMode] = useState<InteractionMode>('AUTO')
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
      setTurns((previous) =>
        {
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
        }
      )
    } catch (error) {
      setTurns((previous) =>
        {
          const next = previous.map((turn): ChatTurn => turn.id === turnId
            ? { kind: 'error', id: turnId, question, errorMessage: toUserMessage(error) }
            : turn)
          saveTurns(activeSessionId, next)
          return next
        }
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
          interactionMode={interactionMode}
          sessionTitle={activeSession?.title}
        />

        <main ref={scrollRef} className="conversation-scroll">
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
        interactionMode={interactionMode}
      />

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
    </div>
  )
}
