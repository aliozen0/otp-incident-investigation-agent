import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { VisualizationRenderer } from './VisualizationRenderer'

describe('VisualizationRenderer', () => {
  it('renders a validated bar chart with accessible title', () => {
    render(
      <VisualizationRenderer visualization={{
        id: 'success', type: 'BAR', title: 'Başarı karşılaştırması', unit: 'PERCENT',
        series: [{ key: 'success', label: 'Başarı' }],
        points: [{ label: 'Mevcut', seriesKey: 'success', value: 72.1, evidenceId: 'ev-current' }],
      }} />
    )
    expect(screen.getByText('Başarı karşılaştırması')).toBeInTheDocument()
  })

  it('fails closed for an unknown runtime chart type', () => {
    render(
      <VisualizationRenderer visualization={{
        id: 'bad', type: 'SCRIPT' as 'BAR', title: 'Bad', unit: 'NONE',
        series: [], points: [],
      }} />
    )
    expect(screen.getByText(/gösterilemiyor/i)).toBeInTheDocument()
  })
})
