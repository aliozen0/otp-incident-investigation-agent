import type { Hypothesis } from '../api/types'
import { PROBABILITY_LABEL_TR, UI_TEXT } from '../lib/labels'

const PROBABILITY_STYLE: Record<Hypothesis['probability'], string> = {
  HIGH: 'text-alert',
  MEDIUM: 'text-signal',
  LOW: 'text-ink-muted',
}

export function HypothesisList({ hypotheses }: { hypotheses: Hypothesis[] }) {
  if (hypotheses.length === 0) {
    return <p className="text-sm text-ink-muted">{UI_TEXT.noHypotheses}</p>
  }

  return (
    <ol className="space-y-4">
      {hypotheses.map((h) => (
        <li key={h.rank} className="border border-line rounded-md p-4">
          <div className="flex items-center justify-between">
            <span className="font-display text-sm text-ink">#{h.rank} {h.possibleCause}</span>
            <span className={`font-mono text-xs uppercase ${PROBABILITY_STYLE[h.probability]}`}>
              {PROBABILITY_LABEL_TR[h.probability]}
            </span>
          </div>
          <div className="mt-2 flex flex-wrap gap-1.5">
            {h.supportingEvidenceIds.map((id) => (
              <code key={id} className="bg-signal-soft text-signal font-mono text-xs px-1.5 py-0.5 rounded">
                {id}
              </code>
            ))}
          </div>
          {h.verificationSteps.length > 0 && (
            <ul className="mt-2 list-disc list-inside text-sm text-ink-muted">
              {h.verificationSteps.map((step, i) => (
                <li key={i}>{step}</li>
              ))}
            </ul>
          )}
        </li>
      ))}
    </ol>
  )
}
