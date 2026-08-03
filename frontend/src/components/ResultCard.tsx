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
import { formatDateTime, UI_TEXT } from '../lib/labels'
import { VisualizationRenderer } from './charts/VisualizationRenderer'

function DetailSection({
  title,
  count,
  children,
  open = false,
}: {
  title: string
  count?: number
  children: React.ReactNode
  open?: boolean
}) {
  return (
    <details className="result-detail" open={open}>
      <summary>
        <span>{title}</span>
        {count !== undefined && <span className="detail-count">{count}</span>}
      </summary>
      <div className="px-4 pb-4 pt-2">{children}</div>
    </details>
  )
}

export function ResultCard({ investigation, assistantMessage }: { investigation: Investigation; assistantMessage?: string }) {
  const inv = investigation
  const naturalSummary =
    assistantMessage || (inv.summary && inv.summary !== inv.status ? inv.summary : synthesizeSummary(inv))

  return (
    <article className="assistant-result">
      <div className="flex flex-wrap items-center gap-2">
        <StatusBadge status={inv.status} severity={inv.severity} />
        <span className="ml-auto font-mono text-[10px] uppercase tracking-[0.16em] text-ink-subtle">
          {formatDateTime(inv.timeWindow.endAt)}
        </span>
      </div>

      <p className="mt-4 text-[15px] leading-7 text-ink">{naturalSummary}</p>

      <div className="result-stats">
        <div>
          <span>Güven skoru</span>
          {inv.confidence !== null ? (
            <ConfidenceGauge confidence={inv.confidence} />
          ) : (
            <strong>—</strong>
          )}
        </div>
        <div>
          <span>Kanıt</span>
          <strong>{inv.evidence.length}</strong>
        </div>
        <div>
          <span>Hipotez</span>
          <strong>{inv.hypotheses.length}</strong>
        </div>
        <div>
          <span>RAG kaynağı</span>
          <strong>{inv.knowledgeReferences.length}</strong>
        </div>
      </div>

      {inv.validation.warnings.length > 0 && (
        <div className="mt-4 border-l-2 border-alert bg-alert-soft px-4 py-3">
          <p className="font-display text-xs font-semibold uppercase tracking-wide text-alert">
            Doğrulama notu
          </p>
          <ul className="mt-1 space-y-1 text-sm leading-5 text-ink">
            {inv.validation.warnings.map((warning) => (
              <li key={warning}>{warning}</li>
            ))}
          </ul>
        </div>
      )}

      {(inv.visualizations ?? []).length > 0 && (
        <section className="mt-5" aria-label="Analiz görselleri">
          <h3 className="mb-3 font-display text-xs font-semibold uppercase tracking-wide text-ink-muted">Analiz görselleri</h3>
          <div className="grid gap-4 xl:grid-cols-2">
            {(inv.visualizations ?? []).map((visualization) => (
              <VisualizationRenderer key={visualization.id} visualization={visualization} />
            ))}
          </div>
        </section>
      )}

      <div className="mt-5 overflow-hidden rounded-lg border border-line">
        <DetailSection title={UI_TEXT.hypothesesSection} count={inv.hypotheses.length} open>
          <HypothesisChart hypotheses={inv.hypotheses} />
          <div className="mt-4">
            <HypothesisList hypotheses={inv.hypotheses} />
          </div>
        </DetailSection>
        <DetailSection title={UI_TEXT.evidenceSection} count={inv.evidence.length}>
          <EvidenceLedger evidence={inv.evidence} />
        </DetailSection>
        <DetailSection title={UI_TEXT.actionsSection} count={inv.recommendedActions.length}>
          <ActionsList actions={inv.recommendedActions} />
        </DetailSection>
        <DetailSection title={UI_TEXT.knowledgeRefsSection} count={inv.knowledgeReferences.length}>
          <KnowledgeReferences refs={inv.knowledgeReferences} />
        </DetailSection>
      </div>

      {inv.status !== 'FAILED' && (
        <div className="mt-5 border-t border-line pt-5">
          <IncidentDecisionPanel investigationId={inv.investigationId} />
        </div>
      )}
    </article>
  )
}
