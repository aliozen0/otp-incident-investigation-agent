import type { RecommendedAction } from '../api/types'
import { UI_TEXT, SEVERITY_LABEL_TR, ACTION_TYPE_LABEL_TR } from '../lib/labels'

export function ActionsList({ actions }: { actions: RecommendedAction[] }) {
  if (actions.length === 0) {
    return <p className="text-sm text-ink-muted">{UI_TEXT.noActions}</p>
  }

  return (
    <ul className="space-y-2">
      {actions.map((a, i) => (
        <li
          key={i}
          className={`flex items-start justify-between gap-3 rounded-xl border p-3 ${
            a.requiresApproval ? 'border-alert/40 bg-alert-soft' : 'border-line bg-surface'
          }`}
        >
          <div>
            <span className="font-mono text-xs uppercase text-ink-muted">
              {ACTION_TYPE_LABEL_TR[a.actionType] ?? a.actionType}
            </span>
            <p className="text-sm text-ink mt-0.5">{a.description}</p>
          </div>
          <div className="text-right shrink-0">
            <span className="block font-mono text-xs text-ink-muted">{`${UI_TEXT.riskLabel}: ${SEVERITY_LABEL_TR[a.risk]}`}</span>
            {a.requiresApproval && (
              <span className="block font-display text-xs text-alert mt-0.5">{UI_TEXT.approvalRequired}</span>
            )}
          </div>
        </li>
      ))}
    </ul>
  )
}
