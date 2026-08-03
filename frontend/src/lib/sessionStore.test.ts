import { describe, it, expect, beforeEach, vi } from 'vitest'
import {
  listSessions,
  createSession,
  renameSession,
  recordQuestion,
  getRecordedQuestion,
  saveTurns,
  loadTurns,
  deleteSession,
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

  it('listSessions breaks a tied createdAt by insertion order (later-created first)', () => {
    vi.useFakeTimers()
    try {
      vi.setSystemTime(new Date('2026-08-02T12:00:00.000Z'))
      const first = createSession()
      const second = createSession() // same timestamp as `first` — no clock advance

      const sessions = listSessions()

      expect(sessions[0].sessionId).toBe(second.sessionId)
      expect(sessions[1].sessionId).toBe(first.sessionId)
    } finally {
      vi.useRealTimers()
    }
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

  it('handles corrupt JSON in localStorage gracefully', () => {
    const sessionId = 'test-session-id'

    // Seed corrupt data directly into localStorage
    localStorage.setItem(`otp-sentinel:questions:${sessionId}`, 'not valid json {')

    // getRecordedQuestion should return undefined instead of throwing
    expect(getRecordedQuestion(sessionId, 'inv-1')).toBeUndefined()

    // recordQuestion should not throw and should overwrite the corrupt data
    expect(() => recordQuestion(sessionId, 'inv-1', 'new question')).not.toThrow()

    // After recording, the question should be retrievable
    expect(getRecordedQuestion(sessionId, 'inv-1')).toBe('new question')
  })

  it('stores versioned mixed chat turns and falls back safely from old data', () => {
    const session = createSession()
    const turns = [
      { kind: 'chat' as const, id: 'm-1', question: 'Merhaba', assistantMessage: 'Merhaba!' },
      { kind: 'clarification' as const, id: 'm-2', question: 'Operatör?', assistantMessage: 'Hangisi?' },
    ]
    saveTurns(session.sessionId, turns)

    expect(loadTurns(session.sessionId)).toEqual(turns)
    localStorage.setItem(`otp-sentinel:turns:${session.sessionId}`, JSON.stringify({ version: 1, turns: 'bad' }))
    expect(loadTurns(session.sessionId)).toEqual([])
  })

  it('deletes a session together with its turns and recorded questions', () => {
    const kept = createSession()
    const removed = createSession()
    saveTurns(removed.sessionId, [
      { kind: 'chat' as const, id: 'm-1', question: 'Merhaba', assistantMessage: 'Merhaba!' },
    ])
    recordQuestion(removed.sessionId, 'inv-1', 'Merhaba')

    deleteSession(removed.sessionId)

    expect(listSessions().map((s) => s.sessionId)).toEqual([kept.sessionId])
    expect(loadTurns(removed.sessionId)).toEqual([])
    expect(getRecordedQuestion(removed.sessionId, 'inv-1')).toBeUndefined()
  })
})
