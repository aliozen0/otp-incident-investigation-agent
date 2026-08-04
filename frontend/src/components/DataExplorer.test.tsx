import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { DataExplorer } from './DataExplorer'

const OVERVIEW = {
  startAt: '2026-08-04T11:00:00Z',
  endAt: '2026-08-04T12:00:00Z',
  totals: {
    attempted: 12480, delivered: 8998, failed: 3482, retries: 2100,
    successRate: 72.1, averageDeliverySeconds: 8.7, p95DeliverySeconds: 19.4,
  },
  series: [
    {
      bucketAt: '2026-08-04T11:59:00Z', attempted: 832, delivered: 600, failed: 232, retries: 140,
      successRate: 72.12, averageDeliverySeconds: 8.69, p95DeliverySeconds: 18.2,
    },
  ],
  providers: [
    {
      provider: 'OPERATOR_B', attempted: 6930, delivered: 3585, failed: 3345, successRate: 51.73,
      status: 'DEGRADED', averageResponseSeconds: 13.9, timeoutRate: 0.31,
      circuitBreakerState: 'HALF_OPEN', activeConnections: 48, maxConnections: 50,
    },
  ],
  errors: [{ errorCode: 'PROVIDER_TIMEOUT', failures: 2228, share: 63.99 }],
  queue: {
    bucketAt: '2026-08-04T11:59:00Z', pendingMessages: 184, normalPendingThreshold: 1000,
    oldestMessageAgeSeconds: 4, activeConsumers: 8, expectedConsumers: 8, deadLetterCount: 3,
    processingRateStatus: 'NORMAL', status: 'HEALTHY',
  },
  changes: [
    {
      changeId: 'chg-102', occurredAt: '2026-08-04T11:12:00Z', type: 'DEPLOY',
      component: 'OTP_GATEWAY', description: 'Gateway v2.4 deployed', version: 'v2.4', approved: true,
    },
  ],
}

const SAMPLES = [
  {
    bucketAt: '2026-08-04T11:59:00Z', provider: 'OPERATOR_B', attempted: 462, delivered: 239,
    failed: 223, retries: 150, averageDeliverySeconds: 13.9, p95DeliverySeconds: 26.1,
    providerStatus: 'DEGRADED', timeoutRate: 0.31,
    errors: 'PROVIDER_TIMEOUT=142, RATE_LIMITED=40',
  },
]

describe('DataExplorer', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        if (url.includes('/operations/overview')) {
          return Promise.resolve({ ok: true, status: 200, json: async () => OVERVIEW })
        }
        if (url.includes('/operations/samples')) {
          return Promise.resolve({ ok: true, status: 200, json: async () => SAMPLES })
        }
        return Promise.reject(new Error(`unexpected fetch ${url}`))
      })
    )
  })
  afterEach(() => vi.unstubAllGlobals())

  it('shows the aggregates, the degraded provider and the raw rows behind them', async () => {
    render(<DataExplorer onClose={vi.fn()} />)

    await waitFor(() => expect(screen.getByText('72,10%')).toBeInTheDocument())
    expect(screen.getByText(/OPERATOR_B bozulmuş durumda/)).toBeInTheDocument()
    // The raw minute row the aggregate is computed from must be visible, not just the summary.
    expect(screen.getByText('PROVIDER_TIMEOUT=142, RATE_LIMITED=40')).toBeInTheDocument()
    expect(screen.getByText('Gateway v2.4 deployed')).toBeInTheDocument()
  })

  it('requests the window from the selected range preset', async () => {
    render(<DataExplorer onClose={vi.fn()} />)

    await waitFor(() => expect(fetch).toHaveBeenCalled())
    const call = (fetch as unknown as ReturnType<typeof vi.fn>).mock.calls.find((entry) =>
      String(entry[0]).includes('/operations/overview')
    )
    const url = new URL(String(call?.[0]), 'http://localhost')
    const startAt = new Date(url.searchParams.get('startAt') as string).getTime()
    const endAt = new Date(url.searchParams.get('endAt') as string).getTime()
    expect(endAt - startAt).toBe(60 * 60_000)
  })
})
