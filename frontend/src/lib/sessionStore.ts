export interface SessionMeta {
  sessionId: string
  title: string
  createdAt: string
}

import type { ChatTurn } from '../api/types'

const SESSIONS_KEY = 'otp-sentinel:sessions'
const questionKey = (sessionId: string) => `otp-sentinel:questions:${sessionId}`
const turnsKey = (sessionId: string) => `otp-sentinel:turns:${sessionId}`
const TURN_SCHEMA_VERSION = 2

function readSessions(): SessionMeta[] {
  const raw = localStorage.getItem(SESSIONS_KEY)
  if (!raw) return []
  try {
    return JSON.parse(raw) as SessionMeta[]
  } catch {
    return []
  }
}

function writeSessions(sessions: SessionMeta[]): void {
  localStorage.setItem(SESSIONS_KEY, JSON.stringify(sessions))
}

export function listSessions(): SessionMeta[] {
  const sessions = readSessions()
  const indexed = sessions.map((s, i) => ({ session: s, index: i }))
  return indexed
    .sort((a, b) => {
      const timeDiff = new Date(b.session.createdAt).getTime() - new Date(a.session.createdAt).getTime()
      return timeDiff !== 0 ? timeDiff : b.index - a.index
    })
    .map(x => x.session)
}

export function createSession(): SessionMeta {
  const session: SessionMeta = {
    sessionId: crypto.randomUUID(),
    title: 'Yeni sohbet',
    createdAt: new Date().toISOString(),
  }
  writeSessions([...readSessions(), session])
  return session
}

export function renameSession(sessionId: string, title: string): void {
  const truncated = title.slice(0, 60)
  writeSessions(
    readSessions().map((s) => (s.sessionId === sessionId ? { ...s, title: truncated } : s))
  )
}

export function deleteSession(sessionId: string): void {
  writeSessions(readSessions().filter((s) => s.sessionId !== sessionId))
  localStorage.removeItem(turnsKey(sessionId))
  localStorage.removeItem(questionKey(sessionId))
}

export function recordQuestion(sessionId: string, investigationId: string, question: string): void {
  const key = questionKey(sessionId)
  const raw = localStorage.getItem(key)
  let map: Record<string, string> = {}
  if (raw) {
    try {
      map = JSON.parse(raw) as Record<string, string>
    } catch {
      map = {}
    }
  }
  map[investigationId] = question
  localStorage.setItem(key, JSON.stringify(map))
}

export function getRecordedQuestion(sessionId: string, investigationId: string): string | undefined {
  const raw = localStorage.getItem(questionKey(sessionId))
  if (!raw) return undefined
  try {
    const map: Record<string, string> = JSON.parse(raw) as Record<string, string>
    return map[investigationId]
  } catch {
    return undefined
  }
}

export function saveTurns(sessionId: string, turns: ChatTurn[]): void {
  const stableTurns = turns.filter((turn) => turn.kind !== 'pending')
  localStorage.setItem(
    turnsKey(sessionId),
    JSON.stringify({ version: TURN_SCHEMA_VERSION, turns: stableTurns })
  )
}

export function loadTurns(sessionId: string): ChatTurn[] {
  const raw = localStorage.getItem(turnsKey(sessionId))
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw) as { version?: unknown; turns?: unknown }
    if (parsed.version !== TURN_SCHEMA_VERSION || !Array.isArray(parsed.turns)) return []
    return parsed.turns.filter(isStoredTurn)
  } catch {
    return []
  }
}

function isStoredTurn(value: unknown): value is ChatTurn {
  if (!value || typeof value !== 'object') return false
  const turn = value as Record<string, unknown>
  if (typeof turn.id !== 'string' || typeof turn.question !== 'string') return false
  if (turn.kind === 'chat' || turn.kind === 'clarification') {
    return typeof turn.assistantMessage === 'string'
  }
  if (turn.kind === 'investigation') {
    return typeof turn.assistantMessage === 'string' && !!turn.investigation
  }
  if (turn.kind === 'error') return typeof turn.errorMessage === 'string'
  return false
}
