import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { SourceTrail } from './SourceTrail'
import type { ChatTurn, Investigation } from '../api/types'

const INVESTIGATION: Investigation = {
  investigationId: 'inv-1',
  status: 'ANOMALY_CONFIRMED',
  severity: 'HIGH',
  summary: 'OTP başarı oranı düştü.',
  timeWindow: { startAt: '2026-08-04T11:15:00Z', endAt: '2026-08-04T11:30:00Z' },
  evidence: [
    {
      id: 'ev-otp-success-rate-current',
      sourceType: 'TOOL_RESULT',
      sourceReference: 'getOtpMetrics',
      observation: 'OTP success rate for current window is 72.24%',
      observedAt: '2026-08-04T11:30:00Z',
      metricName: 'otp_success_rate',
      metricValue: 72.24,
      metricUnit: 'percent',
    },
    {
      id: 'ev-queue-health',
      sourceType: 'TOOL_RESULT',
      sourceReference: 'getQueueHealth',
      observation: 'Queue status is HEALTHY',
      observedAt: '2026-08-04T11:30:00Z',
      metricName: null,
      metricValue: null,
      metricUnit: null,
    },
  ],
  hypotheses: [],
  recommendedActions: [],
  knowledgeReferences: [
    {
      documentId: 'PB-OPERATOR-B-001',
      version: '1',
      chunkId: 'PB-OPERATOR-B-001#v1#c0',
      title: 'Operator B timeout playbook',
      similarityScore: 0.74,
    },
  ],
  confidence: 0.8,
  approvalRequired: false,
  validation: { status: 'PASSED', warnings: [] },
  visualizations: [],
}

const INVESTIGATION_TURN: ChatTurn = {
  kind: 'investigation',
  id: 'inv-1',
  question: 'neden düştü?',
  assistantMessage: 'OTP başarı oranı düştü.',
  investigation: INVESTIGATION,
}

describe('SourceTrail', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({
            documentId: 'PB-OPERATOR-B-001',
            version: '1',
            title: 'Operator B timeout playbook',
            documentType: 'PROVIDER_PLAYBOOK',
            effectiveFrom: '2026-01-01',
            sanitizedContent: 'tam metin',
            chunks: [
              {
                chunkId: 'PB-OPERATOR-B-001#v1#c0',
                sectionTitle: 'Özet',
                content: 'Circuit breaker HALF_OPEN durumunda bekleyin.',
                tokenCount: 12,
                embeddingModel: 'nvidia/nv-embedqa-e5-v5',
              },
            ],
          }),
        })
      )
    )
  })
  afterEach(() => vi.unstubAllGlobals())

  it('names every table the answer was read from and expands the rows behind it', () => {
    render(<SourceTrail turn={INVESTIGATION_TURN} />)

    fireEvent.click(screen.getByText(/Veritabanı · 2 sorgu, 2 kanıt/))
    expect(screen.getByText('OTP teslim metrikleri')).toBeInTheDocument()
    fireEvent.click(screen.getByText('OTP teslim metrikleri'))
    expect(screen.getByText('ev-otp-success-rate-current')).toBeInTheDocument()
  })

  it('shows the retrieved chunk text so the reader sees what the model was given', async () => {
    render(<SourceTrail turn={INVESTIGATION_TURN} />)

    fireEvent.click(screen.getByText(/RAG · 1 belge parçası/))
    fireEvent.click(screen.getByText('Operator B timeout playbook'))

    await waitFor(() =>
      expect(screen.getByText('Circuit breaker HALF_OPEN durumunda bekleyin.')).toBeInTheDocument()
    )
    expect(screen.getByText(/nvidia\/nv-embedqa-e5-v5/)).toBeInTheDocument()
  })

  it('states plainly that a tool-free chat reply read nothing', () => {
    render(
      <SourceTrail
        turn={{ kind: 'chat', id: 'm-1', question: 'merhaba', assistantMessage: 'merhaba' }}
      />
    )

    expect(screen.getByText(/Kaynak yok · toolsuz sohbet/)).toBeInTheDocument()
  })

  it('does not claim RAG was used when nothing matched', () => {
    render(
      <SourceTrail
        turn={{ ...INVESTIGATION_TURN, investigation: { ...INVESTIGATION, knowledgeReferences: [] } }}
      />
    )

    expect(screen.getByText(/RAG · eşleşme yok/)).toBeInTheDocument()
  })
})
