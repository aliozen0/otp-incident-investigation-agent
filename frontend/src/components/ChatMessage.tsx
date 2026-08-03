import type { ChatTurn } from '../api/types'
import { ResultCard } from './ResultCard'
import { ErrorPanel } from './ErrorPanel'
import { TypingIndicator } from './TypingIndicator'

export type { ChatTurn } from '../api/types'

export function ChatMessage({ turn, onSuggestion }: { turn: ChatTurn; onSuggestion?: (text: string) => void }) {
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
          {turn.kind === 'pending' && <TypingIndicator />}
          {turn.kind === 'error' && (
            <ErrorPanel message={turn.errorMessage} />
          )}
          {turn.kind === 'investigation' && (
            <ResultCard investigation={turn.investigation} assistantMessage={turn.assistantMessage} />
          )}
          {(turn.kind === 'chat' || turn.kind === 'clarification') && (
            <div className={turn.kind === 'clarification' ? 'assistant-bubble clarification-bubble' : 'assistant-bubble'}>
              <p>{turn.assistantMessage}</p>
              {turn.suggestions && turn.suggestions.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-2" aria-label="Önerilen devam soruları">
                  {turn.suggestions.map((suggestion) => (
                    <button key={suggestion} type="button" className="suggestion-chip" onClick={() => onSuggestion?.(suggestion)}>
                      {suggestion}
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
