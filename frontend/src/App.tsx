export default function App() {
  return (
    <div className="min-h-screen bg-paper text-ink font-body">
      <header className="border-b border-line px-6 py-4 flex items-center justify-between">
        <span className="font-display font-semibold text-lg tracking-tight">OTP Sentinel</span>
        <span className="font-mono text-xs uppercase tracking-wide text-ink-muted border border-line rounded px-2 py-1">
          Mock / PoC demo
        </span>
      </header>
      <main className="max-w-[880px] mx-auto px-6 py-10">
        <p className="text-ink-muted">Loading investigation console…</p>
      </main>
    </div>
  )
}
