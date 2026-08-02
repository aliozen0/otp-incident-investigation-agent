import type { Investigation } from '../api/types'
import { ResultCard } from './ResultCard'
import { ErrorPanel } from './ErrorPanel'
import { TypingIndicator } from './TypingIndicator'

export interface ChatTurn {
  id: string
  question: string
  status: 'pending' | 'done' | 'error'
  investigation?: Investigation
  errorMessage?: string
}

export function ChatMessage({ turn }: { turn: ChatTurn }) {
  return (
    <div className="chat-turn">
      <div className="flex justify-end pl-10 sm:pl-20">
        <div className="user-message">{turn.question}</div>
      </div>

      <div className="mt-7 grid grid-cols-[34px_minmax(0,1fr)] gap-3 sm:grid-cols-[40px_minmax(0,1fr)] sm:gap-4">
        <div className="agent-avatar" aria-hidden="true">
          OS
        </div>
        <div className="min-w-0">
          <div className="mb-2 flex items-center gap-2">
            <span className="font-display text-sm font-semibold text-ink">OTP Sentinel</span>
            <span className="text-[10px] uppercase tracking-[0.14em] text-ink-subtle">Agent</span>
          </div>
          {turn.status === 'pending' && <TypingIndicator />}
          {turn.status === 'error' && turn.errorMessage && (
            <ErrorPanel message={turn.errorMessage} />
          )}
          {turn.status === 'done' && turn.investigation && (
            <ResultCard investigation={turn.investigation} />
          )}
        </div>
      </div>
    </div>
  )
}
