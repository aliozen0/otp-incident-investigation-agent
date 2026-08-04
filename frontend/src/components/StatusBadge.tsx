import type { InvestigationStatus, Severity } from '../api/types'
import { STATUS_LABEL_TR, SEVERITY_LABEL_TR } from '../lib/labels'
import { IconActivity, IconAlert, IconCheck, IconClose } from './icons'

const STATUS_STYLE: Record<InvestigationStatus, string> = {
  ANOMALY_CONFIRMED: 'bg-alert-soft text-alert border-alert/30',
  NO_ANOMALY: 'bg-confirm-soft text-confirm border-confirm/30',
  PARTIAL_ANALYSIS: 'bg-signal-soft text-signal border-signal/30',
  FAILED: 'bg-danger-soft text-danger border-danger/30',
}

const STATUS_ICON: Record<InvestigationStatus, typeof IconCheck> = {
  ANOMALY_CONFIRMED: IconAlert,
  NO_ANOMALY: IconCheck,
  PARTIAL_ANALYSIS: IconActivity,
  FAILED: IconClose,
}

export function StatusBadge({
  status,
  severity,
}: {
  status: InvestigationStatus
  severity: Severity | null
}) {
  const Glyph = STATUS_ICON[status]
  return (
    <div className="flex items-center gap-2">
      <span
        className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 font-display text-[13px] font-medium ${STATUS_STYLE[status]}`}
      >
        <Glyph size={13} />
        {STATUS_LABEL_TR[status]}
      </span>
      {severity && (
        <span className="rounded-full border border-line px-2.5 py-1 font-mono text-[10.5px] uppercase tracking-wide text-ink-muted">
          {SEVERITY_LABEL_TR[severity]}
        </span>
      )}
    </div>
  )
}
