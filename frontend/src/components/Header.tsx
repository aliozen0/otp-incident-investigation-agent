export function Header() {
  return (
    <header className="border-b border-line px-6 py-4 flex items-center justify-between">
      <div>
        <span className="font-display font-semibold text-lg tracking-tight">OTP Sentinel</span>
        <span className="ml-2 text-ink-muted text-sm">incident investigation console</span>
      </div>
      <span className="font-mono text-xs uppercase tracking-wide text-ink-muted border border-line rounded px-2 py-1">
        Mock / PoC demo
      </span>
    </header>
  )
}
