import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ResultCard } from './ResultCard'
import type { Investigation } from '../api/types'

const INVESTIGATION: Investigation = {
  investigationId: 'inv-1',
  status: 'ANOMALY_CONFIRMED',
  severity: 'HIGH',
  summary: 'ANOMALY_CONFIRMED',
  timeWindow: { startAt: '2026-07-30T11:15:00Z', endAt: '2026-07-30T11:30:00Z' },
  evidence: [],
  hypotheses: [
    {
      rank: 1,
      possibleCause: 'Gateway bağlantı havuzu',
      probability: 'HIGH',
      supportingEvidenceIds: [],
      verificationSteps: [],
    },
  ],
  recommendedActions: [],
  knowledgeReferences: [],
  confidence: 0.87,
  approvalRequired: false,
  validation: { status: 'PASSED', warnings: [] },
}

describe('ResultCard', () => {
  it('never renders the raw enum summary field', () => {
    render(<ResultCard investigation={INVESTIGATION} />)
    expect(screen.queryByText('ANOMALY_CONFIRMED')).not.toBeInTheDocument()
  })

  it('renders the synthesized Turkish summary instead', () => {
    render(<ResultCard investigation={INVESTIGATION} />)
    expect(screen.getByText(/Anomali doğrulandı/, { selector: 'p' })).toBeInTheDocument()
  })
})
