import type { KnowledgeReference } from '../api/types'

export function KnowledgeReferences({ refs }: { refs: KnowledgeReference[] }) {
  if (refs.length === 0) {
    return <p className="text-sm text-ink-muted">No similar historical incidents were found.</p>
  }

  return (
    <ul className="space-y-1.5">
      {refs.map((r, i) => (
        <li key={`${r.documentId}-${r.chunkId ?? i}`} className="text-sm text-ink-muted">
          <code className="font-mono text-xs text-ink">{r.documentId}{r.version ? ` v${r.version}` : ''}</code>
          {r.title && <>{' — '}{r.title}</>}
          {typeof r.similarityScore === 'number' && (
            <span className="ml-2 font-mono text-xs">(similarity {r.similarityScore.toFixed(2)})</span>
          )}
        </li>
      ))}
    </ul>
  )
}
