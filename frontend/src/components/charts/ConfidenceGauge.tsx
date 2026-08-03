import { formatNumber } from '../../lib/labels'

export function ConfidenceGauge({ confidence }: { confidence: number | null }) {
  if (confidence === null) return null
  const pct = Math.round(confidence * 100)

  return (
    <div className="inline-flex items-center gap-2 border border-line rounded-md px-3 py-1.5">
      <div className="w-16 h-1.5 rounded-full bg-line overflow-hidden">
        <div className="h-full bg-signal rounded-full" style={{ width: `${pct}%` }} />
      </div>
      <span className="font-mono text-xs text-ink-muted">{formatNumber(confidence, 2)}</span>
    </div>
  )
}
