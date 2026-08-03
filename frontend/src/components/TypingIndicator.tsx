import { UI_TEXT } from '../lib/labels'

export function TypingIndicator() {
  return (
    <div className="border border-line rounded-lg p-4 flex items-center gap-3 max-w-md">
      <div className="flex gap-1">
        <span className="w-1.5 h-1.5 rounded-full bg-ink-muted animate-bounce [animation-delay:-0.3s]" />
        <span className="w-1.5 h-1.5 rounded-full bg-ink-muted animate-bounce [animation-delay:-0.15s]" />
        <span className="w-1.5 h-1.5 rounded-full bg-ink-muted animate-bounce" />
      </div>
      <span className="text-sm text-ink-muted">{UI_TEXT.investigating}</span>
    </div>
  )
}
