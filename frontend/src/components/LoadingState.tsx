export function LoadingState() {
  return (
    <div className="border border-line rounded-lg p-8 flex flex-col items-center gap-3 text-center">
      <div className="h-8 w-8 border-2 border-line-strong border-t-signal rounded-full animate-spin" />
      <p className="font-display text-ink">Investigating&hellip;</p>
      <p className="text-sm text-ink-muted max-w-sm">
        Collecting live metrics, error, queue and provider evidence, then checking historical
        incidents. A real analysis run can take up to a minute &mdash; this isn&apos;t a cached result.
      </p>
    </div>
  )
}
