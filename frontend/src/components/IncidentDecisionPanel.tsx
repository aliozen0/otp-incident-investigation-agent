import { useState } from 'react'
import { previewIncidentDraft, submitIncidentDecision } from '../api/client'
import { generateIdempotencyKey } from '../lib/idempotency'
import { toUserMessage } from '../lib/errors'
import type { IncidentDraftPreview, IncidentDecisionResponse } from '../api/types'

type Stage = 'none' | 'previewing' | 'previewed' | 'deciding' | 'decided'

export function IncidentDecisionPanel({ investigationId }: { investigationId: string }) {
  const [stage, setStage] = useState<Stage>('none')
  const [preview, setPreview] = useState<IncidentDraftPreview | null>(null)
  const [decision, setDecision] = useState<IncidentDecisionResponse | null>(null)
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [idempotencyKey] = useState(() => generateIdempotencyKey())

  async function handlePreview() {
    setStage('previewing')
    setError(null)
    try {
      const result = await previewIncidentDraft(investigationId)
      setPreview(result)
      setStage('previewed')
    } catch (err) {
      setError(toUserMessage(err))
      setStage('none')
    }
  }

  async function handleDecision(kind: 'APPROVE' | 'REJECT') {
    setStage('deciding')
    setError(null)
    try {
      const result = await submitIncidentDecision(
        investigationId,
        { decision: kind, reason },
        idempotencyKey
      )
      setDecision(result)
      setStage('decided')
    } catch (err) {
      setError(toUserMessage(err))
      setStage('previewed')
    }
  }

  return (
    <div className="mt-6 border-t border-line pt-6">
      <h2 className="font-display text-sm uppercase tracking-wide text-ink-muted mb-2">
        Incident draft
      </h2>

      {stage === 'none' && (
        <button
          onClick={handlePreview}
          className="border border-signal text-signal font-display text-sm px-4 py-2 rounded-md hover:bg-signal-soft"
        >
          Preview incident draft
        </button>
      )}

      {stage === 'previewing' && <p className="text-sm text-ink-muted">Loading preview…</p>}

      {(stage === 'previewed' || stage === 'deciding') && preview && (
        <div className="border border-line rounded-md p-4">
          <p className="text-xs text-ink-muted mb-2">
            No incident exists yet &mdash; this is a preview only.
          </p>
          <p className="font-display text-sm text-ink">{preview.title}</p>
          <p className="text-sm text-ink mt-1">{preview.summary}</p>
          <p className="text-xs text-ink-muted mt-1 font-mono">
            {preview.evidenceCount} evidence item(s) &middot; severity {preview.severity}
          </p>
          {preview.recommendedChecks.length > 0 && (
            <ul className="list-disc list-inside text-sm text-ink-muted mt-2">
              {preview.recommendedChecks.map((c, i) => (
                <li key={i}>{c}</li>
              ))}
            </ul>
          )}

          <label htmlFor="reason" className="block text-xs text-ink-muted mt-4 mb-1">
            Reason (recorded in the audit trail)
          </label>
          <input
            id="reason"
            type="text"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            className="w-full border border-line-strong rounded-md p-2 text-sm bg-paper focus:outline-none focus:ring-2 focus:ring-signal"
          />

          <div className="mt-3 flex gap-2">
            <button
              onClick={() => handleDecision('APPROVE')}
              disabled={stage === 'deciding'}
              className="bg-confirm text-white font-display text-sm px-4 py-2 rounded-md hover:opacity-90 disabled:opacity-50"
            >
              Approve
            </button>
            <button
              onClick={() => handleDecision('REJECT')}
              disabled={stage === 'deciding'}
              className="border border-danger text-danger font-display text-sm px-4 py-2 rounded-md hover:bg-danger-soft disabled:opacity-50"
            >
              Reject
            </button>
          </div>
        </div>
      )}

      {stage === 'decided' && decision && (
        <div className="border border-confirm bg-confirm-soft rounded-md p-4">
          <p className="font-display text-sm text-confirm">
            {decision.externalIncidentId ? `Incident ${decision.externalIncidentId} created` : 'Decision recorded'}
          </p>
          <p className="text-xs text-ink-muted mt-1 font-mono">
            incidentDraftId: {decision.incidentDraftId}
          </p>
          {decision.idempotentReplay && (
            <p className="text-xs text-signal mt-2 border border-signal bg-signal-soft rounded px-2 py-1 inline-block">
              Idempotent replay &mdash; the same decision was already recorded, no duplicate was created.
            </p>
          )}
          <button
            onClick={() => handleDecision(decision.status === 'CREATED' ? 'APPROVE' : 'REJECT')}
            className="block mt-3 text-xs text-ink-muted underline"
          >
            Resubmit with the same idempotency key (demonstrates replay)
          </button>
        </div>
      )}

      {error && <p className="text-danger text-sm mt-2">{error}</p>}
    </div>
  )
}
