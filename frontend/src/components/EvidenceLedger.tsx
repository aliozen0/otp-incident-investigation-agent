import type { Evidence } from '../api/types'
import { UI_TEXT, formatDateTime, SOURCE_TYPE_LABEL_TR } from '../lib/labels'

export function EvidenceLedger({ evidence }: { evidence: Evidence[] }) {
  if (evidence.length === 0) {
    return <p className="text-sm text-ink-muted">{UI_TEXT.noEvidence}</p>
  }

  return (
    <ul className="space-y-3">
      {evidence.map((item) => (
        <li key={item.id} className="border border-line rounded-md p-3">
          <div className="flex items-center gap-2 flex-wrap">
            <code className="bg-signal-soft text-signal font-mono text-xs px-1.5 py-0.5 rounded">
              {item.id}
            </code>
            <span className="text-xs text-ink-muted uppercase tracking-wide">
              {SOURCE_TYPE_LABEL_TR[item.sourceType] ?? item.sourceType}
            </span>
            <span className="text-xs text-ink-muted font-mono">{item.sourceReference}</span>
          </div>
          <p className="mt-1.5 text-sm text-ink">{item.observation}</p>
          <p className="mt-1 text-xs text-ink-muted font-mono">{formatDateTime(item.observedAt)}</p>
        </li>
      ))}
    </ul>
  )
}
