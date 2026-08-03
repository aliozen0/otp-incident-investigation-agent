import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import type { InteractionMode, ModelOption } from '../api/types'
import { INTERACTION_MODE_LABEL_TR, MODE_LABEL_TR, UI_TEXT } from '../lib/labels'
import { IconAlert, IconClock, IconSend, IconSparkles } from './icons'

interface Props {
  disabled: boolean
  onSubmit: (question: string, timeWindow?: { startAt: string; endAt: string }) => void
  models?: ModelOption[]
  modelId?: string | null
  onModelChange?: (modelId: string) => void
  mode?: 'quick' | 'thorough'
  onModeChange?: (mode: 'quick' | 'thorough') => void
  interactionMode?: InteractionMode
  onInteractionModeChange?: (mode: InteractionMode) => void
}

const MAX_TEXTAREA_HEIGHT = 220

export function ChatComposer({
  disabled,
  onSubmit,
  models = [],
  modelId = null,
  onModelChange,
  mode = 'thorough',
  onModeChange,
  interactionMode = 'AUTO',
  onInteractionModeChange,
}: Props) {
  const [question, setQuestion] = useState('')
  const [useTimeWindow, setUseTimeWindow] = useState(false)
  const [startAt, setStartAt] = useState('')
  const [endAt, setEndAt] = useState('')
  const [validationMessage, setValidationMessage] = useState<string | null>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  // Grow with the text instead of scrolling inside three fixed rows.
  useEffect(() => {
    const node = textareaRef.current
    if (!node) return
    node.style.height = 'auto'
    node.style.height = `${Math.min(node.scrollHeight, MAX_TEXTAREA_HEIGHT)}px`
  }, [question])

  function submit() {
    const trimmed = question.trim()
    if (trimmed.length === 0 || disabled) return
    if (interactionMode !== 'CHAT' && useTimeWindow && (!startAt || !endAt)) {
      setValidationMessage(UI_TEXT.incompleteTimeWindow)
      return
    }
    const timeWindow =
      interactionMode !== 'CHAT' && useTimeWindow && startAt && endAt
        ? { startAt: new Date(startAt).toISOString(), endAt: new Date(endAt).toISOString() }
        : undefined
    setValidationMessage(null)
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
      {interactionMode !== 'CHAT' && useTimeWindow && (
        <div className="animate-fade grid grid-cols-1 gap-3 border-b border-line bg-surface-sunken px-4 py-3 sm:grid-cols-2">
          <div>
            <label htmlFor="startAt" className="field-label">
              {UI_TEXT.timeWindowStart} · {UI_TEXT.localTime}
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
              {UI_TEXT.timeWindowEnd} · {UI_TEXT.localTime}
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
        ref={textareaRef}
        value={question}
        onChange={(e) => {
          setQuestion(e.target.value)
          if (validationMessage) setValidationMessage(null)
        }}
        onKeyDown={handleKeyDown}
        placeholder={UI_TEXT.composerPlaceholder}
        rows={2}
        disabled={disabled}
        className="scroll-thin w-full resize-none border-0 bg-transparent px-4 pb-3 pt-4 text-[15px] leading-6 text-ink outline-none placeholder:text-ink-subtle disabled:cursor-wait"
      />

      {validationMessage && (
        <p role="alert" className="mx-4 mb-3 flex items-center gap-2 rounded-lg bg-alert-soft px-3 py-2 text-xs text-alert">
          <IconAlert size={14} />
          {validationMessage}
        </p>
      )}

      <div className="flex flex-wrap items-center gap-1.5 border-t border-line-soft px-2.5 py-2">
        {models.length > 0 && (
          <div className="composer-select-wrap">
            <span className="status-dot status-dot-live bg-confirm" aria-hidden="true" />
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
          <IconSparkles size={13} />
          <label htmlFor="composer-interaction-mode" className="sr-only">
            Etkileşim modu
          </label>
          <select
            id="composer-interaction-mode"
            aria-label="Etkileşim modu"
            value={interactionMode}
            onChange={(e) => onInteractionModeChange?.(e.target.value as InteractionMode)}
            className="composer-select"
          >
            {(Object.keys(INTERACTION_MODE_LABEL_TR) as InteractionMode[]).map((value) => (
              <option key={value} value={value}>{INTERACTION_MODE_LABEL_TR[value]}</option>
            ))}
          </select>
        </div>

        {interactionMode !== 'CHAT' && (
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
        )}

        {interactionMode !== 'CHAT' && (
          <label className="composer-tool" title={UI_TEXT.timeWindowToggle}>
            <input
              id="useTimeWindow"
              type="checkbox"
              checked={useTimeWindow}
              onChange={(e) => setUseTimeWindow(e.target.checked)}
              className="sr-only"
            />
            <IconClock size={14} />
            <span className="hidden sm:inline" aria-hidden="true">Zaman aralığı</span>
            <span className="sr-only">{UI_TEXT.timeWindowToggle}</span>
          </label>
        )}

        <div className="ml-auto flex items-center gap-2.5">
          <span className="hidden items-center gap-1.5 text-[11px] text-ink-subtle sm:flex">
            <span className="kbd">Enter</span> gönderir
          </span>
          <button
            type="button"
            onClick={submit}
            disabled={disabled || question.trim().length === 0}
            className="send-button"
            aria-label={disabled ? UI_TEXT.investigating : UI_TEXT.send}
          >
            {disabled ? (
              <span className="flex items-center gap-[3px]">
                <span className="typing-dot" />
                <span className="typing-dot" style={{ animationDelay: '.18s' }} />
                <span className="typing-dot" style={{ animationDelay: '.36s' }} />
              </span>
            ) : (
              <IconSend size={17} />
            )}
          </button>
        </div>
      </div>
    </div>
  )
}
