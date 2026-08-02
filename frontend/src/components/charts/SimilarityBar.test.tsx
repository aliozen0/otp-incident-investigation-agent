import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { SimilarityBar } from './SimilarityBar'

describe('SimilarityBar', () => {
  it('renders a bar with the formatted score when similarityScore is present', () => {
    render(
      <SimilarityBar reference={{ documentId: 'INC-2026-041', similarityScore: 0.86 }} />
    )
    expect(screen.getByText('0,86')).toBeInTheDocument()
  })

  it('renders nothing bar-like when similarityScore is absent (real backend today)', () => {
    const { container } = render(<SimilarityBar reference={{ documentId: 'INC-2026-041' }} />)
    expect(container.querySelector('[data-testid="similarity-bar-track"]')).toBeNull()
  })
})
