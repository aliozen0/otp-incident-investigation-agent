export interface SessionMeta {
  sessionId: string
  title: string
  createdAt: string
}

const SESSIONS_KEY = 'otp-sentinel:sessions'
const questionKey = (sessionId: string) => `otp-sentinel:questions:${sessionId}`

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
  return readSessions().sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
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

export function recordQuestion(sessionId: string, investigationId: string, question: string): void {
  const key = questionKey(sessionId)
  const raw = localStorage.getItem(key)
  const map: Record<string, string> = raw ? JSON.parse(raw) : {}
  map[investigationId] = question
  localStorage.setItem(key, JSON.stringify(map))
}

export function getRecordedQuestion(sessionId: string, investigationId: string): string | undefined {
  const raw = localStorage.getItem(questionKey(sessionId))
  if (!raw) return undefined
  const map: Record<string, string> = JSON.parse(raw)
  return map[investigationId]
}
