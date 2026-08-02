import { describe, it, expect, beforeEach } from 'vitest'
import {
  listSessions,
  createSession,
  renameSession,
  recordQuestion,
  getRecordedQuestion,
} from './sessionStore'

describe('sessionStore', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('createSession persists a new thread and listSessions returns it newest-first', () => {
    const first = createSession()
    const second = createSession()

    const sessions = listSessions()

    expect(sessions[0].sessionId).toBe(second.sessionId)
    expect(sessions[1].sessionId).toBe(first.sessionId)
    expect(sessions[0].title).toBe('Yeni sohbet')
  })

  it('renameSession updates the title and truncates to 60 chars', () => {
    const session = createSession()
    renameSession(session.sessionId, 'x'.repeat(100))

    const sessions = listSessions()

    expect(sessions[0].title).toHaveLength(60)
  })

  it('records and retrieves a question by investigationId, scoped to sessionId', () => {
    const session = createSession()
    recordQuestion(session.sessionId, 'inv-1', 'Neden düştü?')

    expect(getRecordedQuestion(session.sessionId, 'inv-1')).toBe('Neden düştü?')
    expect(getRecordedQuestion(session.sessionId, 'inv-does-not-exist')).toBeUndefined()
    expect(getRecordedQuestion('other-session', 'inv-1')).toBeUndefined()
  })
})
