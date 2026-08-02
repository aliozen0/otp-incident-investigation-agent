import { UI_TEXT } from '../lib/labels'

export function ErrorPanel({ message }: { message: string }) {
  return (
    <div className="border border-danger bg-danger-soft rounded-lg p-4">
      <p className="font-display text-sm text-danger mb-1">{UI_TEXT.errorTitle}</p>
      <p className="text-sm text-ink">{message}</p>
    </div>
  )
}
