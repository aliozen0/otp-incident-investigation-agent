import type { InvestigationStatus, Severity } from '../api/types'
import { STATUS_LABEL_TR, SEVERITY_LABEL_TR } from '../lib/labels'

const STATUS_STYLE: Record<InvestigationStatus, string> = {
  ANOMALY_CONFIRMED: 'bg-alert-soft text-alert border-alert',
  NO_ANOMALY: 'bg-confirm-soft text-confirm border-confirm',
  PARTIAL_ANALYSIS: 'bg-signal-soft text-signal border-signal',
  FAILED: 'bg-danger-soft text-danger border-danger',
}

export function StatusBadge({
  status,
  severity,
}: {
  status: InvestigationStatus
  severity: Severity | null
}) {
  return (
    <div className="flex items-center gap-2">
      <span
        className={`font-display text-sm px-3 py-1 rounded-full border ${STATUS_STYLE[status]}`}
      >
        {STATUS_LABEL_TR[status]}
      </span>
      {severity && (
        <span className="font-mono text-xs uppercase tracking-wide text-ink-muted border border-line rounded px-2 py-1">
          {SEVERITY_LABEL_TR[severity]}
        </span>
      )}
    </div>
  )
}
