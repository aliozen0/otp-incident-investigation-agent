import { UI_TEXT } from '../lib/labels'

export function TypingIndicator() {
  return (
    <div className="assistant-bubble flex max-w-md items-center gap-3">
      <span className="flex items-center gap-1 text-signal">
        <span className="typing-dot" />
        <span className="typing-dot" style={{ animationDelay: '.18s' }} />
        <span className="typing-dot" style={{ animationDelay: '.36s' }} />
      </span>
      <span className="text-sm text-ink-muted">{UI_TEXT.investigating}</span>
    </div>
  )
}
