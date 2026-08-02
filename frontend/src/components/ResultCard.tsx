import type { Investigation } from '../api/types'
import { StatusBadge } from './StatusBadge'
import { EvidenceLedger } from './EvidenceLedger'
import { HypothesisList } from './HypothesisList'
import { ActionsList } from './ActionsList'
import { KnowledgeReferences } from './KnowledgeReferences'
import { IncidentDecisionPanel } from './IncidentDecisionPanel'
import { HypothesisChart } from './charts/HypothesisChart'
import { ConfidenceGauge } from './charts/ConfidenceGauge'
import { synthesizeSummary } from '../lib/summarize'
import { UI_TEXT } from '../lib/labels'

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mt-6">
      <h2 className="font-display text-sm uppercase tracking-wide text-ink-muted mb-2">{title}</h2>
      {children}
    </section>
  )
}

export function ResultCard({ investigation }: { investigation: Investigation }) {
  const inv = investigation
  return (
    <div className="border border-line rounded-lg p-6">
      <StatusBadge status={inv.status} severity={inv.severity} />
      <p className="mt-3 text-ink">{synthesizeSummary(inv)}</p>

      {inv.validation.warnings.length > 0 && (
        <div className="mt-3 border border-alert bg-alert-soft rounded-md p-3">
          <p className="font-display text-xs uppercase text-alert mb-1">Doğrulama uyarıları</p>
          <ul className="list-disc list-inside text-sm text-ink">
            {inv.validation.warnings.map((w, i) => (
              <li key={i}>{w}</li>
            ))}
          </ul>
        </div>
      )}

      {inv.confidence !== null && (
        <div className="mt-3">
          <p className="font-display text-xs uppercase text-ink-muted mb-1">{UI_TEXT.confidenceLabel}</p>
          <ConfidenceGauge confidence={inv.confidence} />
        </div>
      )}

      <Section title={UI_TEXT.evidenceSection}>
        <EvidenceLedger evidence={inv.evidence} />
      </Section>

      <Section title={UI_TEXT.hypothesesSection}>
        <HypothesisChart hypotheses={inv.hypotheses} />
        <div className="mt-4">
          <HypothesisList hypotheses={inv.hypotheses} />
        </div>
      </Section>

      <Section title={UI_TEXT.actionsSection}>
        <ActionsList actions={inv.recommendedActions} />
      </Section>

      <Section title={UI_TEXT.knowledgeRefsSection}>
        <KnowledgeReferences refs={inv.knowledgeReferences} />
      </Section>

      {inv.status !== 'FAILED' && (
        <IncidentDecisionPanel investigationId={inv.investigationId} />
      )}
    </div>
  )
}
