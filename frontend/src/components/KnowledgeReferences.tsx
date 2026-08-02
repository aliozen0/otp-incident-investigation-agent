import type { KnowledgeReference } from '../api/types'
import { UI_TEXT } from '../lib/labels'
import { SimilarityBar } from './charts/SimilarityBar'

export function KnowledgeReferences({ refs }: { refs: KnowledgeReference[] }) {
  if (refs.length === 0) {
    return <p className="text-sm text-ink-muted">{UI_TEXT.noKnowledgeRefs}</p>
  }

  return (
    <ul className="space-y-1.5">
      {refs.map((r, i) => (
        <li key={`${r.documentId}-${r.chunkId ?? i}`} className="text-sm text-ink-muted">
          <code className="font-mono text-xs text-ink">{r.documentId}{r.version ? ` v${r.version}` : ''}</code>
          {r.title && <>{' — '}{r.title}</>}
          <span className="ml-2"><SimilarityBar reference={r} /></span>
        </li>
      ))}
    </ul>
  )
}
