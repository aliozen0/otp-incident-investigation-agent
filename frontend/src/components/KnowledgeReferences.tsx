import type { KnowledgeReference } from '../api/types'
import { UI_TEXT } from '../lib/labels'
import { SimilarityBar } from './charts/SimilarityBar'

export function KnowledgeReferences({ refs }: { refs: KnowledgeReference[] }) {
  if (refs.length === 0) {
    return <p className="text-sm text-ink-muted">{UI_TEXT.noKnowledgeRefs}</p>
  }

  return (
    <ul className="space-y-2">
      {refs.map((r, i) => (
        <li key={`${r.documentId}-${r.chunkId ?? i}`} className="rounded-md border border-line bg-surface px-3 py-2.5 text-sm text-ink-muted">
          <div className="flex flex-wrap items-start justify-between gap-2">
            <div>
              <strong className="block text-xs font-medium text-ink">{r.title ?? r.documentId}</strong>
              <code className="mt-1 block font-mono text-[9px] text-ink-subtle">
                {r.documentId}{r.version ? ` · v${r.version}` : ''}{r.chunkId ? ` · ${r.chunkId}` : ''}
              </code>
            </div>
            <SimilarityBar reference={r} />
          </div>
        </li>
      ))}
    </ul>
  )
}
