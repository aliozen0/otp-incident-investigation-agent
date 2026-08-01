import { describe, it, expect } from 'vitest'
import { toUserMessage } from './errors'
import { ApiError } from '../api/client'

function problem(errorCode: string, detail = 'x') {
  return {
    type: 'https://errors.example.local/x',
    title: 'x',
    status: 400,
    detail,
    instance: '/api/v1/investigations',
    correlationId: 'corr-1',
    errorCode,
  }
}

describe('toUserMessage', () => {
  it('maps INVESTIGATION_TIMEOUT to an honest, non-alarming message', () => {
    const msg = toUserMessage(new ApiError(problem('INVESTIGATION_TIMEOUT')))
    expect(msg).toMatch(/took too long|timed out/i)
  })

  it('maps QUESTION_NOT_ACTIONABLE to a guidance message', () => {
    const msg = toUserMessage(new ApiError(problem('QUESTION_NOT_ACTIONABLE')))
    expect(msg).toMatch(/rephrase|specific/i)
  })

  it('falls back to the problem detail for unknown error codes', () => {
    const msg = toUserMessage(new ApiError(problem('SOME_NEW_CODE', 'Something specific happened.')))
    expect(msg).toBe('Something specific happened.')
  })

  it('handles non-ApiError unknowns with a generic message', () => {
    const msg = toUserMessage(new Error('network down'))
    expect(msg).toMatch(/unexpected|failed/i)
  })
})
