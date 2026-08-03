import type { KnowledgeReference } from '../../api/types'
import { formatNumber } from '../../lib/labels'

export function SimilarityBar({ reference }: { reference: KnowledgeReference }) {
  if (typeof reference.similarityScore !== 'number') return null
  const pct = Math.round(reference.similarityScore * 100)

  return (
    <div className="inline-flex items-center gap-1.5">
      <div
        data-testid="similarity-bar-track"
        className="w-12 h-1.5 rounded-full bg-line overflow-hidden"
      >
        <div className="h-full bg-signal rounded-full" style={{ width: `${pct}%` }} />
      </div>
      <span className="font-mono text-xs text-ink-muted">{formatNumber(reference.similarityScore, 2)}</span>
    </div>
  )
}
