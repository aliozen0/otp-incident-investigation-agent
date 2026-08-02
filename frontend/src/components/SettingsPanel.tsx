import { useEffect, useState } from 'react'
import {
  listModels,
  listKnowledgeDocuments,
  uploadKnowledgeDocument,
} from '../api/client'
import { toUserMessage } from '../lib/errors'
import { MODE_LABEL_TR, DOCUMENT_TYPE_LABEL_TR, UI_TEXT } from '../lib/labels'
import type { KnowledgeDocumentSummary } from '../api/types'

const DOCUMENT_TYPES = Object.keys(DOCUMENT_TYPE_LABEL_TR)

interface Props {
  modelId: string | null
  onModelChange: (modelId: string) => void
  mode: 'quick' | 'thorough'
  onModeChange: (mode: 'quick' | 'thorough') => void
  onClose: () => void
}

export function SettingsPanel({ modelId, onModelChange, mode, onModeChange, onClose }: Props) {
  const [models, setModels] = useState<string[]>([])
  const [documents, setDocuments] = useState<KnowledgeDocumentSummary[]>([])
  const [uploadError, setUploadError] = useState<string | null>(null)
  const [uploadOk, setUploadOk] = useState(false)
  const [title, setTitle] = useState('')
  const [documentType, setDocumentType] = useState(DOCUMENT_TYPES[0])
  const [effectiveFrom, setEffectiveFrom] = useState('')
  const [content, setContent] = useState('')

  useEffect(() => {
    listModels().then(setModels).catch(() => setModels([]))
    refreshDocuments()
  }, [])

  function refreshDocuments() {
    listKnowledgeDocuments().then(setDocuments).catch(() => setDocuments([]))
  }

  async function handleUpload(e: React.FormEvent) {
    e.preventDefault()
    setUploadError(null)
    setUploadOk(false)
    try {
      await uploadKnowledgeDocument({ title, documentType, effectiveFrom, content })
      setUploadOk(true)
      setTitle('')
      setContent('')
      refreshDocuments()
    } catch (err) {
      setUploadError(toUserMessage(err))
    }
  }

  return (
    <div className="fixed inset-0 bg-ink/20 flex justify-end z-10">
      <div className="w-96 bg-paper h-full border-l border-line overflow-y-auto p-5">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-display text-lg">{UI_TEXT.settings}</h2>
          <button type="button" onClick={onClose} className="text-ink-muted text-sm">
            {UI_TEXT.closeSettings}
          </button>
        </div>

        <section className="mb-6">
          <label htmlFor="model" className="block font-display text-sm mb-2">
            {UI_TEXT.modelLabel}
          </label>
          <select
            id="model"
            value={modelId ?? ''}
            onChange={(e) => onModelChange(e.target.value)}
            className="w-full border border-line-strong rounded-md p-2 text-sm bg-paper"
          >
            {models.map((m) => (
              <option key={m} value={m}>
                {m}
              </option>
            ))}
          </select>
        </section>

        <section className="mb-6">
          <p className="font-display text-sm mb-2">{UI_TEXT.modeLabel}</p>
          <div className="flex gap-4">
            {(['quick', 'thorough'] as const).map((m) => (
              <label key={m} className="flex items-center gap-1.5 text-sm">
                <input
                  type="radio"
                  name="mode"
                  checked={mode === m}
                  onChange={() => onModeChange(m)}
                  className="accent-signal"
                />
                {MODE_LABEL_TR[m]}
              </label>
            ))}
          </div>
        </section>

        <section>
          <p className="font-display text-sm mb-2">{UI_TEXT.knowledgeSectionTitle}</p>
          {documents.length === 0 && (
            <p className="text-xs text-ink-muted mb-3">{UI_TEXT.knowledgeListEmpty}</p>
          )}
          <ul className="text-xs text-ink-muted mb-4 space-y-1">
            {documents.map((d) => (
              <li key={`${d.documentId}-${d.version}`}>
                {d.title} — {DOCUMENT_TYPE_LABEL_TR[d.documentType] ?? d.documentType}
              </li>
            ))}
          </ul>

          <form onSubmit={handleUpload} className="border-t border-line pt-3 space-y-2">
            <p className="font-display text-xs uppercase text-ink-muted">{UI_TEXT.uploadTitle}</p>
            <input
              required
              placeholder={UI_TEXT.uploadTitleField}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full border border-line-strong rounded-md p-2 text-sm bg-paper"
            />
            <select
              value={documentType}
              onChange={(e) => setDocumentType(e.target.value)}
              className="w-full border border-line-strong rounded-md p-2 text-sm bg-paper"
            >
              {DOCUMENT_TYPES.map((t) => (
                <option key={t} value={t}>
                  {DOCUMENT_TYPE_LABEL_TR[t]}
                </option>
              ))}
            </select>
            <input
              required
              type="date"
              value={effectiveFrom}
              onChange={(e) => setEffectiveFrom(e.target.value)}
              className="w-full border border-line-strong rounded-md p-2 text-sm bg-paper"
            />
            <textarea
              required
              placeholder={UI_TEXT.uploadContentField}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              rows={4}
              className="w-full border border-line-strong rounded-md p-2 text-sm bg-paper"
            />
            <button
              type="submit"
              className="w-full bg-signal text-white font-display text-sm px-3 py-2 rounded-md hover:opacity-90"
            >
              {UI_TEXT.uploadSubmit}
            </button>
            {uploadOk && <p className="text-xs text-confirm">{UI_TEXT.uploadSuccess}</p>}
            {uploadError && <p className="text-xs text-danger">{uploadError}</p>}
          </form>
        </section>
      </div>
    </div>
  )
}
