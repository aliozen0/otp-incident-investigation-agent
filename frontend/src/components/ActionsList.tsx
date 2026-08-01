import type { RecommendedAction } from '../api/types'

export function ActionsList({ actions }: { actions: RecommendedAction[] }) {
  if (actions.length === 0) {
    return <p className="text-sm text-ink-muted">No actions were recommended.</p>
  }

  return (
    <ul className="space-y-2">
      {actions.map((a, i) => (
        <li
          key={i}
          className={`border rounded-md p-3 flex items-start justify-between gap-3 ${
            a.requiresApproval ? 'border-alert bg-alert-soft' : 'border-line'
          }`}
        >
          <div>
            <span className="font-mono text-xs uppercase text-ink-muted">{a.actionType}</span>
            <p className="text-sm text-ink mt-0.5">{a.description}</p>
          </div>
          <div className="text-right shrink-0">
            <span className="block font-mono text-xs text-ink-muted">risk: {a.risk}</span>
            {a.requiresApproval && (
              <span className="block font-display text-xs text-alert mt-0.5">requires approval</span>
            )}
          </div>
        </li>
      ))}
    </ul>
  )
}
