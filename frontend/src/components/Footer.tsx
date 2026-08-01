export function Footer() {
  return (
    <footer className="border-t border-line mt-16 px-6 py-6 text-sm text-ink-muted">
      <p className="max-w-[880px] mx-auto">
        This is a proof-of-concept built on mock fixture data. It has no access to and makes no
        claim about any real company&apos;s internal systems or architecture. Analysis is produced by a
        live LLM (or a deterministic stub, depending on configuration) and is not a confirmed root
        cause &mdash; every incident record requires explicit human approval before it is created.
      </p>
    </footer>
  )
}
