import { useState, type KeyboardEvent } from 'react'
import type { ModelOption } from '../api/types'
import { MODE_LABEL_TR, UI_TEXT } from '../lib/labels'

interface Props {
  disabled: boolean
  onSubmit: (question: string, timeWindow?: { startAt: string; endAt: string }) => void
  models?: ModelOption[]
  modelId?: string | null
  onModelChange?: (modelId: string) => void
  mode?: 'quick' | 'thorough'
  onModeChange?: (mode: 'quick' | 'thorough') => void
}

export function ChatComposer({
  disabled,
  onSubmit,
  models = [],
  modelId = null,
  onModelChange,
  mode = 'thorough',
  onModeChange,
}: Props) {
  const [question, setQuestion] = useState('')
  const [useTimeWindow, setUseTimeWindow] = useState(false)
  const [startAt, setStartAt] = useState('')
  const [endAt, setEndAt] = useState('')

  function submit() {
    const trimmed = question.trim()
    if (trimmed.length === 0 || disabled) return
    const timeWindow =
      useTimeWindow && startAt && endAt
        ? { startAt: `${startAt}:00Z`, endAt: `${endAt}:00Z` }
        : undefined
    onSubmit(trimmed, timeWindow)
    setQuestion('')
  }

  function handleKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      submit()
    }
  }

  return (
    <div className="composer-shell">
      {useTimeWindow && (
        <div className="grid grid-cols-1 gap-3 border-b border-line bg-surface-muted px-4 py-3 sm:grid-cols-2">
          <div>
            <label htmlFor="startAt" className="field-label">
              {UI_TEXT.timeWindowStart} · UTC
            </label>
            <input
              id="startAt"
              type="datetime-local"
              value={startAt}
              onChange={(e) => setStartAt(e.target.value)}
              className="field-control font-mono text-xs"
            />
          </div>
          <div>
            <label htmlFor="endAt" className="field-label">
              {UI_TEXT.timeWindowEnd} · UTC
            </label>
            <input
              id="endAt"
              type="datetime-local"
              value={endAt}
              onChange={(e) => setEndAt(e.target.value)}
              className="field-control font-mono text-xs"
            />
          </div>
        </div>
      )}

      <textarea
        value={question}
        onChange={(e) => setQuestion(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={UI_TEXT.composerPlaceholder}
        rows={3}
        disabled={disabled}
        className="w-full resize-none border-0 bg-transparent px-4 pb-3 pt-4 text-[15px] leading-6 text-ink outline-none placeholder:text-ink-subtle disabled:cursor-wait"
      />

      <div className="flex flex-wrap items-center gap-2 border-t border-line px-3 py-2.5">
        {models.length > 0 && (
          <div className="composer-select-wrap">
            <span className="status-dot bg-confirm" aria-hidden="true" />
            <label htmlFor="composer-model" className="sr-only">
              Analiz modeli
            </label>
            <select
              id="composer-model"
              aria-label="Analiz modeli"
              value={modelId ?? models[0]?.id ?? ''}
              onChange={(e) => onModelChange?.(e.target.value)}
              className="composer-select"
            >
              {models.map((model) => (
                <option key={model.id} value={model.id}>
                  {model.label}
                </option>
              ))}
            </select>
          </div>
        )}

        <div className="composer-select-wrap">
          <label htmlFor="composer-mode" className="sr-only">
            Analiz modu
          </label>
          <select
            id="composer-mode"
            aria-label="Analiz modu"
            value={mode}
            onChange={(e) => onModeChange?.(e.target.value as 'quick' | 'thorough')}
            className="composer-select"
          >
            <option value="quick">{MODE_LABEL_TR.quick}</option>
            <option value="thorough">{MODE_LABEL_TR.thorough}</option>
          </select>
        </div>

        <label className="composer-tool" title={UI_TEXT.timeWindowToggle}>
          <input
            id="useTimeWindow"
            type="checkbox"
            checked={useTimeWindow}
            onChange={(e) => setUseTimeWindow(e.target.checked)}
            className="sr-only"
          />
          <span aria-hidden="true">◷</span>
          <span>{UI_TEXT.timeWindowToggle}</span>
        </label>

        <div className="ml-auto flex items-center gap-3">
          <span className="hidden text-[11px] text-ink-subtle sm:inline">Enter gönderir</span>
          <button
            type="button"
            onClick={submit}
            disabled={disabled || question.trim().length === 0}
            className="send-button"
            aria-label={disabled ? UI_TEXT.investigating : UI_TEXT.send}
          >
            {disabled ? <span className="animate-pulse">•••</span> : <span aria-hidden="true">↑</span>}
          </button>
        </div>
      </div>
    </div>
  )
}
