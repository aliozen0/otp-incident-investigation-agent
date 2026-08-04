import { UI_TEXT } from '../lib/labels'
import { IconAlert } from './icons'

export function ErrorPanel({ message }: { message: string }) {
  return (
    <div className="flex max-w-2xl gap-3 rounded-xl border border-danger/30 bg-danger-soft p-4">
      <span className="mt-0.5 text-danger"><IconAlert size={16} /></span>
      <div>
        <p className="font-display text-sm font-semibold text-danger">{UI_TEXT.errorTitle}</p>
        <p className="mt-1 text-sm leading-6 text-ink">{message}</p>
      </div>
    </div>
  )
}
