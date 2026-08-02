import { useState, type KeyboardEvent } from 'react'
import { UI_TEXT } from '../lib/labels'

interface Props {
  disabled: boolean
  onSubmit: (question: string, timeWindow?: { startAt: string; endAt: string }) => void
}

export function ChatComposer({ disabled, onSubmit }: Props) {
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
    <div className="border border-line rounded-lg p-3 bg-white/40">
      <div className="flex items-center gap-2">
        <input
          id="useTimeWindow"
          type="checkbox"
          checked={useTimeWindow}
          onChange={(e) => setUseTimeWindow(e.target.checked)}
          className="accent-signal"
        />
        <label htmlFor="useTimeWindow" className="text-xs text-ink-muted">
          {UI_TEXT.timeWindowToggle}
        </label>
      </div>

      {useTimeWindow && (
        <div className="mt-2 grid grid-cols-2 gap-2">
          <div>
            <label htmlFor="startAt" className="block text-xs text-ink-muted mb-1">
              {UI_TEXT.timeWindowStart} (UTC)
            </label>
            <input
              id="startAt"
              type="datetime-local"
              value={startAt}
              onChange={(e) => setStartAt(e.target.value)}
              className="w-full border border-line-strong rounded-md p-1.5 font-mono text-xs text-ink bg-paper focus:outline-none focus:ring-2 focus:ring-signal"
            />
          </div>
          <div>
            <label htmlFor="endAt" className="block text-xs text-ink-muted mb-1">
              {UI_TEXT.timeWindowEnd} (UTC)
            </label>
            <input
              id="endAt"
              type="datetime-local"
              value={endAt}
              onChange={(e) => setEndAt(e.target.value)}
              className="w-full border border-line-strong rounded-md p-1.5 font-mono text-xs text-ink bg-paper focus:outline-none focus:ring-2 focus:ring-signal"
            />
          </div>
        </div>
      )}

      <textarea
        value={question}
        onChange={(e) => setQuestion(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={UI_TEXT.composerPlaceholder}
        rows={2}
        disabled={disabled}
        className="mt-2 w-full resize-none border-0 bg-transparent p-1 text-sm text-ink focus:outline-none"
      />

      <div className="mt-2 flex justify-end">
        <button
          type="button"
          onClick={submit}
          disabled={disabled}
          className="bg-signal text-white font-display text-sm px-4 py-1.5 rounded-md hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {disabled ? UI_TEXT.investigating : UI_TEXT.send}
        </button>
      </div>
    </div>
  )
}
