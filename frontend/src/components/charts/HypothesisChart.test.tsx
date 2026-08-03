import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { HypothesisChart } from './HypothesisChart'
import type { Hypothesis } from '../../api/types'

const HYPOTHESES: Hypothesis[] = [
  {
    rank: 1,
    possibleCause: 'Gateway bağlantı havuzu',
    probability: 'HIGH',
    supportingEvidenceIds: [],
    verificationSteps: [],
  },
  {
    rank: 2,
    possibleCause: 'Operatör B gecikmesi',
    probability: 'LOW',
    supportingEvidenceIds: [],
    verificationSteps: [],
  },
]

describe('HypothesisChart', () => {
  it('renders one labeled row per hypothesis', () => {
    render(<HypothesisChart hypotheses={HYPOTHESES} />)

    expect(screen.getByText(/Gateway bağlantı havuzu/)).toBeInTheDocument()
    expect(screen.getByText(/Operatör B gecikmesi/)).toBeInTheDocument()
  })

  it('renders nothing when there are no hypotheses', () => {
    const { container } = render(<HypothesisChart hypotheses={[]} />)
    expect(container).toBeEmptyDOMElement()
  })
})
