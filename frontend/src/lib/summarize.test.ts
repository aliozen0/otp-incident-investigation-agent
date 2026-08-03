import { describe, it, expect } from 'vitest'
import { synthesizeSummary } from './summarize'
import type { Investigation } from '../api/types'

const BASE: Investigation = {
  investigationId: 'inv-1',
  status: 'ANOMALY_CONFIRMED',
  severity: 'CRITICAL',
  summary: 'ANOMALY_CONFIRMED',
  timeWindow: { startAt: '2026-07-30T11:15:00Z', endAt: '2026-07-30T11:30:00Z' },
  evidence: [],
  hypotheses: [
    {
      rank: 1,
      possibleCause: 'Gateway bağlantı havuzunda kapasite problemi',
      probability: 'HIGH',
      supportingEvidenceIds: [],
      verificationSteps: [],
    },
    {
      rank: 2,
      possibleCause: 'İkinci olası neden',
      probability: 'LOW',
      supportingEvidenceIds: [],
      verificationSteps: [],
    },
  ],
  recommendedActions: [],
  knowledgeReferences: [],
  confidence: 0.87,
  approvalRequired: true,
  validation: { status: 'PASSED', warnings: [] },
  visualizations: [],
}

describe('synthesizeSummary', () => {
  it('combines Turkish status, severity and the top-ranked hypothesis', () => {
    const result = synthesizeSummary(BASE)
    expect(result).toContain('Anomali doğrulandı')
    expect(result).toContain('Kritik önem') // CRITICAL severity label — see labels.ts
    expect(result).toContain('Gateway bağlantı havuzunda kapasite problemi')
    expect(result).not.toContain('İkinci olası neden')
  })

  it('handles NO_ANOMALY with no hypotheses and null severity', () => {
    const result = synthesizeSummary({
      ...BASE,
      status: 'NO_ANOMALY',
      severity: null,
      hypotheses: [],
    })
    expect(result).toContain('Anomali tespit edilmedi')
    expect(result).not.toContain('null')
    expect(result).not.toContain('undefined')
  })

  it('handles FAILED status', () => {
    const result = synthesizeSummary({ ...BASE, status: 'FAILED', hypotheses: [] })
    expect(result).toContain('Analiz başarısız oldu')
  })
})
