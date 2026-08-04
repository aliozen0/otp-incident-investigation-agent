import { useState } from 'react'
import { previewIncidentDraft, submitIncidentDecision } from '../api/client'
import { generateIdempotencyKey } from '../lib/idempotency'
import { toUserMessage } from '../lib/errors'
import type { IncidentDraftPreview, IncidentDecisionResponse } from '../api/types'
import { SEVERITY_LABEL_TR } from '../lib/labels'
import { IconAlert, IconCheck, IconClose, IconLayers, IconRefresh } from './icons'

type Stage = 'none' | 'previewing' | 'previewed' | 'deciding' | 'decided'

export function IncidentDecisionPanel({ investigationId }: { investigationId: string }) {
  const [stage, setStage] = useState<Stage>('none')
  const [preview, setPreview] = useState<IncidentDraftPreview | null>(null)
  const [decision, setDecision] = useState<IncidentDecisionResponse | null>(null)
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isResubmitting, setIsResubmitting] = useState(false)
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

  async function handleResubmit() {
    setIsResubmitting(true)
    setError(null)
    try {
      const result = await submitIncidentDecision(
        investigationId,
        { decision: decision!.status === 'CREATED' ? 'APPROVE' : 'REJECT', reason },
        idempotencyKey
      )
      setDecision(result)
    } catch (err) {
      setError(toUserMessage(err))
    } finally {
      setIsResubmitting(false)
    }
  }

  return (
    <div>
      <h2 className="mb-2 flex items-center gap-2 font-display text-sm uppercase tracking-wide text-ink-muted">
        <IconLayers size={14} /> Olay taslağı
      </h2>

      {stage === 'none' && (
        <button onClick={handlePreview} className="secondary-button border-signal/40 text-signal">
          <IconAlert size={14} /> Olay taslağını önizle
        </button>
      )}

      {stage === 'previewing' && <p className="text-sm text-ink-muted">Önizleme yükleniyor…</p>}

      {(stage === 'previewed' || stage === 'deciding') && preview && (
        <div className="rounded-xl border border-line bg-surface p-4 shadow-soft">
          <p className="text-xs text-ink-muted mb-2">
            Henüz bir olay oluşturulmadı &mdash; bu yalnızca bir önizleme.
          </p>
          <p className="font-display text-sm text-ink">{preview.title}</p>
          <p className="text-sm text-ink mt-1">{preview.summary}</p>
          <p className="text-xs text-ink-muted mt-1 font-mono">
            {preview.evidenceCount} kanıt öğesi &middot; önem: {SEVERITY_LABEL_TR[preview.severity]}
          </p>
          {preview.recommendedChecks.length > 0 && (
            <ul className="list-disc list-inside text-sm text-ink-muted mt-2">
              {preview.recommendedChecks.map((c, i) => (
                <li key={i}>{c}</li>
              ))}
            </ul>
          )}

          <label htmlFor="reason" className="block text-xs text-ink-muted mt-4 mb-1">
            Gerekçe (denetim kaydına işlenir)
          </label>
          <input
            id="reason"
            type="text"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            className="field-control"
          />

          <div className="mt-3 flex gap-2">
            <button
              onClick={() => handleDecision('APPROVE')}
              disabled={stage === 'deciding'}
              className="primary-button border-confirm bg-confirm hover:bg-confirm/90 disabled:opacity-50"
            >
              <IconCheck size={14} /> Onayla
            </button>
            <button
              onClick={() => handleDecision('REJECT')}
              disabled={stage === 'deciding'}
              className="secondary-button border-danger/40 text-danger hover:bg-danger-soft disabled:opacity-50"
            >
              <IconClose size={14} /> Reddet
            </button>
          </div>
        </div>
      )}

      {stage === 'decided' && decision && (
        <div className="rounded-xl border border-confirm/30 bg-confirm-soft p-4">
          <p className="flex items-center gap-2 font-display text-sm text-confirm">
            <IconCheck size={14} />
            {decision.externalIncidentId ? `Olay ${decision.externalIncidentId} oluşturuldu` : 'Karar kaydedildi'}
          </p>
          <p className="text-xs text-ink-muted mt-1 font-mono">
            incidentDraftId: {decision.incidentDraftId}
          </p>
          {decision.idempotentReplay && (
            <p className="text-xs text-signal mt-2 border border-signal bg-signal-soft rounded px-2 py-1 inline-block">
              Aynı istek tekrar edildi — tekrar kaydedilmedi, kopya oluşturulmadı.
            </p>
          )}
          <button
            onClick={handleResubmit}
            disabled={isResubmitting}
            className="mt-3 inline-flex items-center gap-1.5 text-xs text-ink-muted underline disabled:cursor-not-allowed disabled:opacity-50"
          >
            <IconRefresh size={12} /> Aynı idempotency key ile yeniden gönder (tekrar davranışını gösterir)
          </button>
        </div>
      )}

      {error && <p className="text-danger text-sm mt-2">{error}</p>}
    </div>
  )
}
