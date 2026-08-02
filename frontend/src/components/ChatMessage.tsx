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
    <div className="space-y-3">
      <div className="flex justify-end">
        <div className="max-w-[75%] bg-signal text-white rounded-lg rounded-br-sm px-4 py-2.5 text-sm">
          {turn.question}
        </div>
      </div>

      <div className="flex justify-start">
        <div className="max-w-[85%] w-full">
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
