import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import App from './App'

describe('App chat flow', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string, init?: RequestInit) => {
        if (url.includes('/models')) {
          return Promise.resolve({
            ok: true,
            status: 200,
            json: async () => ({ models: ['meta/llama-3.1-8b-instruct'] }),
          })
        }
        if (url.includes('/knowledge/documents')) {
          return Promise.resolve({ ok: true, status: 200, json: async () => [] })
        }
        if (url.includes('/investigations') && init?.method === 'POST') {
          const body = JSON.parse(init.body as string)
          return Promise.resolve({
            ok: true,
            status: 200,
            json: async () => ({
              investigationId: `inv-${body.question.length}`,
              status: 'ANOMALY_CONFIRMED',
              severity: 'HIGH',
              summary: 'ANOMALY_CONFIRMED',
              timeWindow: { startAt: '2026-07-30T11:15:00Z', endAt: '2026-07-30T11:30:00Z' },
              evidence: [],
              hypotheses: [],
              recommendedActions: [],
              knowledgeReferences: [],
              confidence: 0.9,
              approvalRequired: false,
              validation: { status: 'PASSED', warnings: [] },
            }),
          })
        }
        return Promise.reject(new Error(`unexpected fetch ${url}`))
      })
    )
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('sends a question, then a follow-up, reusing the same sessionId both times', async () => {
    render(<App />)

    const textarea = await screen.findByPlaceholderText(/Ne araştırmak istersiniz/)
    fireEvent.change(textarea, { target: { value: 'OTP oranı neden düştü?' } })
    fireEvent.keyDown(textarea, { key: 'Enter', code: 'Enter' })

    await waitFor(() => expect(screen.getAllByText('OTP oranı neden düştü?').length).toBeGreaterThan(0))
    await waitFor(() => expect(screen.getAllByText(/Anomali doğrulandı/).length).toBeGreaterThan(0))

    fireEvent.change(textarea, { target: { value: 'Peki ya operatör B?' } })
    fireEvent.keyDown(textarea, { key: 'Enter', code: 'Enter' })

    await waitFor(() => expect(screen.getByText('Peki ya operatör B?')).toBeInTheDocument())

    const postCalls = (fetch as unknown as ReturnType<typeof vi.fn>).mock.calls.filter((call) => {
      const [url, init] = call as [string, RequestInit]
      return url.includes('/investigations') && init?.method === 'POST'
    })
    expect(postCalls).toHaveLength(2)
    const firstSessionId = JSON.parse(postCalls[0][1].body as string).sessionId
    const secondSessionId = JSON.parse(postCalls[1][1].body as string).sessionId
    expect(firstSessionId).toBeTruthy()
    expect(firstSessionId).toBe(secondSessionId)
  })
})
