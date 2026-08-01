import type { InvestigationStatus, Severity } from '../api/types'

const STATUS_STYLE: Record<InvestigationStatus, string> = {
  ANOMALY_CONFIRMED: 'bg-alert-soft text-alert border-alert',
  NO_ANOMALY: 'bg-confirm-soft text-confirm border-confirm',
  PARTIAL_ANALYSIS: 'bg-signal-soft text-signal border-signal',
  FAILED: 'bg-danger-soft text-danger border-danger',
}

const STATUS_LABEL: Record<InvestigationStatus, string> = {
  ANOMALY_CONFIRMED: 'Anomaly confirmed',
  NO_ANOMALY: 'No anomaly',
  PARTIAL_ANALYSIS: 'Partial analysis',
  FAILED: 'Failed',
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
        {STATUS_LABEL[status]}
      </span>
      {severity && (
        <span className="font-mono text-xs uppercase tracking-wide text-ink-muted border border-line rounded px-2 py-1">
          {severity}
        </span>
      )}
    </div>
  )
}
