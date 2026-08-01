import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createInvestigation, ApiError } from './client'

describe('createInvestigation', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('returns parsed investigation on 200', async () => {
    const body = { investigationId: 'inv-1', status: 'ANOMALY_CONFIRMED' }
    ;(fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => body,
    })

    const result = await createInvestigation({ question: 'Why did OTP delivery drop?' })

    expect(result).toEqual(body)
    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/investigations',
      expect.objectContaining({ method: 'POST' })
    )
  })

  it('throws ApiError with parsed problem-details on non-2xx', async () => {
    const problem = {
      type: 'https://errors.example.local/investigation-timeout',
      title: 'Investigation timed out',
      status: 504,
      detail: 'The investigation exceeded the configured deadline.',
      instance: '/api/v1/investigations',
      correlationId: 'corr-ec3c',
      errorCode: 'INVESTIGATION_TIMEOUT',
    }
    ;(fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: false,
      status: 504,
      json: async () => problem,
    })

    await expect(createInvestigation({ question: 'x' })).rejects.toBeInstanceOf(ApiError)
    try {
      await createInvestigation({ question: 'x' })
      throw new Error('expected rejection')
    } catch (err) {
      expect((err as ApiError).problemDetails.errorCode).toBe('INVESTIGATION_TIMEOUT')
    }
  })
})
