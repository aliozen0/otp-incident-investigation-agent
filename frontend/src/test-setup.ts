import '@testing-library/jest-dom'
import { configure } from '@testing-library/dom'

// Exclude recharts' hidden off-screen text-measurement span from query
// matches — it duplicates every tick label's text into the DOM.
configure({ defaultIgnore: 'script, style, #recharts_measurement_span' })

// jsdom has no ResizeObserver; recharts' ResponsiveContainer needs one to mount.
// ponytail: no-op stub, not a real observer — fine since jsdom never actually resizes.
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver ??= ResizeObserverStub as unknown as typeof ResizeObserver

// jsdom elements report a zero-size box, so recharts' ResponsiveContainer
// (which sizes off getBoundingClientRect) never renders its children. Give
// every element a fixed non-zero box — except recharts' own off-screen text
// measurement span, which needs a size proportional to its text so recharts
// doesn't think every tick label is 600px wide and force-wrap it word by word.
// ponytail: this is a GLOBAL fixed-size stub (600x400 for every element, in
// every test file, present and future) needed only for recharts' ResponsiveContainer
// under jsdom. Ceiling: no test in this suite can rely on real element geometry
// to catch a collapsed/hidden-element bug. If that's ever needed, scope this to
// chart test files only — e.g. a per-file `beforeEach` import instead of global
// `setupFiles`, or vitest's `environmentMatchGlobs` — rather than leaving it global.
Object.defineProperty(HTMLElement.prototype, 'offsetWidth', { configurable: true, value: 600 })
Object.defineProperty(HTMLElement.prototype, 'offsetHeight', { configurable: true, value: 400 })
HTMLElement.prototype.getBoundingClientRect = function (this: HTMLElement) {
  const width = this.id === 'recharts_measurement_span' ? (this.textContent?.length ?? 0) * 7 : 600
  const height = this.id === 'recharts_measurement_span' ? 14 : 400
  return { width, height, top: 0, left: 0, right: width, bottom: height, x: 0, y: 0, toJSON() {} } as DOMRect
}
