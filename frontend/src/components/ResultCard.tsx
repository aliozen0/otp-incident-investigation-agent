import type { Investigation } from '../api/types'
import { StatusBadge } from './StatusBadge'
import { EvidenceLedger } from './EvidenceLedger'
import { HypothesisList } from './HypothesisList'
import { ActionsList } from './ActionsList'
import { KnowledgeReferences } from './KnowledgeReferences'

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
      <p className="mt-3 text-ink">{inv.summary}</p>

      {inv.validation.warnings.length > 0 && (
        <div className="mt-3 border border-alert bg-alert-soft rounded-md p-3">
          <p className="font-display text-xs uppercase text-alert mb-1">Validation warnings</p>
          <ul className="list-disc list-inside text-sm text-ink">
            {inv.validation.warnings.map((w, i) => (
              <li key={i}>{w}</li>
            ))}
          </ul>
        </div>
      )}

      {inv.confidence !== null && (
        <p className="mt-3 font-mono text-xs text-ink-muted">
          confidence: {inv.confidence.toFixed(2)}
        </p>
      )}

      <Section title="Evidence">
        <EvidenceLedger evidence={inv.evidence} />
      </Section>

      <Section title="Hypotheses">
        <HypothesisList hypotheses={inv.hypotheses} />
      </Section>

      <Section title="Recommended actions">
        <ActionsList actions={inv.recommendedActions} />
      </Section>

      <Section title="Related incidents">
        <KnowledgeReferences refs={inv.knowledgeReferences} />
      </Section>
    </div>
  )
}
