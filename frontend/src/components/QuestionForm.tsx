import { useState, type FormEvent } from 'react'
import type { InvestigationRequest } from '../api/types'

const DEFAULT_QUESTION = 'Why did the OTP delivery rate drop in the last 15 minutes?'
const DEFAULT_START = '2026-07-30T11:15:00Z'
const DEFAULT_END = '2026-07-30T11:30:00Z'

interface Props {
  disabled: boolean
  onSubmit: (req: InvestigationRequest) => void
}

export function QuestionForm({ disabled, onSubmit }: Props) {
  const [question, setQuestion] = useState(DEFAULT_QUESTION)
  const [startAt, setStartAt] = useState(DEFAULT_START)
  const [endAt, setEndAt] = useState(DEFAULT_END)
  const [useTimeWindow, setUseTimeWindow] = useState(true)

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    onSubmit({
      question,
      timeWindow: useTimeWindow ? { startAt, endAt } : undefined,
    })
  }

  return (
    <form onSubmit={handleSubmit} className="border border-line rounded-lg p-6 bg-white/40">
      <label htmlFor="question" className="block font-display text-sm text-ink mb-2">
        What do you want investigated?
      </label>
      <textarea
        id="question"
        value={question}
        onChange={(e) => setQuestion(e.target.value)}
        rows={3}
        minLength={10}
        maxLength={1000}
        required
        className="w-full border border-line-strong rounded-md p-3 font-mono text-sm text-ink bg-paper focus:outline-none focus:ring-2 focus:ring-signal"
      />

      <div className="mt-4 flex items-center gap-2">
        <input
          id="useTimeWindow"
          type="checkbox"
          checked={useTimeWindow}
          onChange={(e) => setUseTimeWindow(e.target.checked)}
          className="accent-signal"
        />
        <label htmlFor="useTimeWindow" className="text-sm text-ink-muted">
          Specify a time window (otherwise resolved from the question)
        </label>
      </div>

      {useTimeWindow && (
        <div className="mt-3 grid grid-cols-2 gap-3">
          <div>
            <label htmlFor="startAt" className="block text-xs text-ink-muted mb-1">
              Start (UTC)
            </label>
            <input
              id="startAt"
              type="text"
              value={startAt}
              onChange={(e) => setStartAt(e.target.value)}
              className="w-full border border-line-strong rounded-md p-2 font-mono text-xs text-ink bg-paper focus:outline-none focus:ring-2 focus:ring-signal"
            />
          </div>
          <div>
            <label htmlFor="endAt" className="block text-xs text-ink-muted mb-1">
              End (UTC)
            </label>
            <input
              id="endAt"
              type="text"
              value={endAt}
              onChange={(e) => setEndAt(e.target.value)}
              className="w-full border border-line-strong rounded-md p-2 font-mono text-xs text-ink bg-paper focus:outline-none focus:ring-2 focus:ring-signal"
            />
          </div>
        </div>
      )}

      <button
        type="submit"
        disabled={disabled}
        className="mt-5 bg-signal text-white font-display text-sm px-5 py-2.5 rounded-md hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {disabled ? 'Investigating…' : 'Start investigation'}
      </button>
    </form>
  )
}
