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
            json: async () => ({
              models: ['meta/llama-3.1-8b-instruct'],
              defaultModelId: 'meta/llama-3.1-8b-instruct',
              options: [
                {
                  id: 'meta/llama-3.1-8b-instruct',
                  label: 'Llama 3.1 8B',
                  provider: 'Meta / NVIDIA NIM',
                  profile: 'FAST',
                  description: 'Hızlı',
                  verified: true,
                },
              ],
            }),
          })
        }
        if (url.includes('/knowledge/documents')) {
          return Promise.resolve({ ok: true, status: 200, json: async () => [] })
        }
        if (url.includes('/chat/messages') && init?.method === 'POST') {
          const body = JSON.parse(init.body as string)
          const isChat = body.message === 'Merhaba'
          return Promise.resolve({
            ok: true,
            status: 200,
            json: async () => ({
              messageId: `msg-${body.message.length}`,
              sessionId: body.sessionId,
              responseType: isChat ? 'CHAT' : 'INVESTIGATION',
              assistantMessage: isChat
                ? 'Merhaba, OTP operasyonlarında yardımcı olurum.'
                : 'OTP başarısı düştü ve Operatör B üzerinde yoğunlaştı.',
              route: { intent: isChat ? 'CHAT' : 'INVESTIGATION', confidence: 0.95, modelId: body.modelId },
              suggestions: [],
              investigation: isChat ? null : {
                investigationId: `inv-${body.message.length}`,
                status: 'ANOMALY_CONFIRMED', severity: 'HIGH',
                summary: 'OTP başarısı düştü ve Operatör B üzerinde yoğunlaştı.',
                timeWindow: { startAt: '2026-07-30T11:15:00Z', endAt: '2026-07-30T11:30:00Z' },
                evidence: [], hypotheses: [], recommendedActions: [], knowledgeReferences: [],
                confidence: 0.9, approvalRequired: false,
                validation: { status: 'PASSED', warnings: [] }, visualizations: [],
              },
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

  it('keeps chat then investigation in one thread and reuses the sessionId', async () => {
    render(<App />)

    const textarea = await screen.findByPlaceholderText(/Ne araştırmak istersiniz/)
    expect(screen.getByLabelText('Analiz modeli')).toHaveValue('meta/llama-3.1-8b-instruct')
    fireEvent.change(textarea, { target: { value: 'Merhaba' } })
    fireEvent.keyDown(textarea, { key: 'Enter', code: 'Enter' })

    await waitFor(() => expect(screen.getAllByText('Merhaba').length).toBeGreaterThan(0))
    await waitFor(() => expect(screen.getByText(/OTP operasyonlarında yardımcı/)).toBeInTheDocument())

    fireEvent.change(textarea, { target: { value: 'OTP oranı neden düştü?' } })
    fireEvent.keyDown(textarea, { key: 'Enter', code: 'Enter' })

    await waitFor(() => expect(screen.getByText('OTP oranı neden düştü?')).toBeInTheDocument())
    await waitFor(() => expect(screen.getAllByText(/Anomali doğrulandı/).length).toBeGreaterThan(0))

    const postCalls = (fetch as unknown as ReturnType<typeof vi.fn>).mock.calls.filter((call) => {
      const [url, init] = call as [string, RequestInit]
      return url.includes('/chat/messages') && init?.method === 'POST'
    })
    expect(postCalls).toHaveLength(2)
    const firstSessionId = JSON.parse(postCalls[0][1].body as string).sessionId
    const secondSessionId = JSON.parse(postCalls[1][1].body as string).sessionId
    expect(firstSessionId).toBeTruthy()
    expect(firstSessionId).toBe(secondSessionId)
    expect(JSON.parse(postCalls[0][1].body as string).modelId)
      .toBe('meta/llama-3.1-8b-instruct')
    expect(JSON.parse(postCalls[0][1].body as string).interactionMode).toBe('AUTO')
    expect(screen.getAllByText('OTP başarısı düştü ve Operatör B üzerinde yoğunlaştı.').length)
      .toBeGreaterThan(0)
  })
})
