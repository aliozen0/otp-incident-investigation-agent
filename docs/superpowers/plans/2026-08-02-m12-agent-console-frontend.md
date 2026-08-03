# M12 — Agent console frontend (chat UI) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the M10 single-page form→result frontend with a ChatGPT/Claude-style chat console (Turkish UI, session memory, model/mode selection, document upload, charts) that talks to the M11 backend endpoints.

**Architecture:** React 19 + Tailwind v4 SPA (unchanged stack). A sidebar lists chat threads (client-tracked session ids — the backend has no "list all sessions" endpoint, only `GET /sessions/{id}/investigations`), a center panel renders a scrolling message list of user/agent bubbles, a navbar settings drawer holds model/mode/knowledge controls. Every user turn is `POST /investigations` with the thread's `sessionId`, `modelId`, `mode`. Charts render inside the agent bubble via Recharts, colors derived from the existing `--color-*` tokens and validated with the dataviz skill's palette validator.

**Tech Stack:** React 19, TypeScript, Tailwind v4, Vitest + Testing Library, Recharts (new dependency — the plan's only new dependency), Node's `crypto.randomUUID()`, `localStorage`.

## Global Constraints

- No dark theme.
- No new container/service; `frontend` Docker build stage stays as-is.
- Only new dependency allowed: `recharts` (chart library, per M12 prompt recommendation). No i18n library — one hand-written dictionary file.
- Do not touch backend (`src/main/java/...`) — M11 is frozen. Any backend data gaps found (see Task 1 note on `KnowledgeReferenceDto`/`InvestigationResponseDto`) must be worked around in the frontend, not "fixed" upstream.
- Before the branch is considered done: `mvn spotless:apply` + `mvn verify` (backend must be unchanged/green), `npm run build` + `npm run test` (frontend) must be green, `node scripts/validate_palette.js` (run from `frontend/`) must PASS for the ordinal hypothesis-probability ramp.
- Raw backend `summary` (currently just the `status` enum name, e.g. `"ANOMALY_CONFIRMED"`) must never be rendered — replace with a synthesized Turkish one-liner (status + severity + top hypothesis).
- All static UI text and backend enum values shown to the user must be Turkish; numbers/dates formatted with `tr-TR`.

## Backend response shape reminders (read-only, verified in `src/main/java`)

`InvestigationResponseDto` (`src/main/java/com/example/otpsentinel/api/dto/InvestigationResponseDto.java`) does **not** echo the question, `sessionId`, `modelId`, or `mode` back. `KnowledgeReferenceDto` inside it carries **only** `documentId` (no `version`/`chunkId`/`title`/`similarityScore`, despite the older Swagger example in `InvestigationController` showing richer fields — that example is aspirational, the mapper (`InvestigationDtoMapper.toDto`) only fills `documentId`). `GET /api/v1/models` returns `{"models": ["meta/llama-3.1-8b-instruct", "meta/llama-3.3-70b-instruct"]}`. `GET /api/v1/knowledge/documents` returns a list of `{documentId, version, title, documentType, effectiveFrom}` (date as `"YYYY-MM-DD"` string). `POST /api/v1/knowledge/documents` request body: `{title, documentType, provider, tags, effectiveFrom, effectiveTo, language, content}` — `documentType` must be one of `INCIDENT_POSTMORTEM | RUNBOOK | ERROR_REFERENCE | PROVIDER_PLAYBOOK | CHANGE_POLICY`, response `{documentId, version}`.

Because the response never echoes the question, the frontend must remember it client-side (Task 3) to render the user bubble when a thread is reloaded from `GET /sessions/{id}/investigations`. Because `similarityScore`/`title` are never populated by the real backend, the similarity chart (Task 4) must degrade to plain text when those fields are absent — this is a known, real backend gap, not a frontend bug.

## File Structure

- `frontend/src/api/types.ts` — extend with `ModelsResponse`, `KnowledgeDocumentSummary`, `KnowledgeDocumentUploadRequest`, `KnowledgeDocumentUploadResponse`; extend `InvestigationRequest` with `sessionId`/`modelId`/`mode`.
- `frontend/src/api/client.ts` — add `listSessionInvestigations`, `listModels`, `listKnowledgeDocuments`, `uploadKnowledgeDocument`.
- `frontend/src/lib/labels.ts` — new. Turkish label dictionary for every backend enum + static UI strings + `tr-TR` number/date formatters.
- `frontend/src/lib/summarize.ts` — new. Synthesizes the one-line Turkish summary (status + severity + top hypothesis) that replaces the raw `summary` field.
- `frontend/src/lib/sessionStore.ts` — new. `localStorage`-backed chat-thread list + per-thread question cache (works around the backend not echoing the question).
- `frontend/src/components/charts/HypothesisChart.tsx` — new. Ranked probability bar chart (Recharts).
- `frontend/src/components/charts/ConfidenceGauge.tsx` — new. Compact confidence stat-tile/gauge.
- `frontend/src/components/charts/SimilarityBar.tsx` — new. Per-reference score bar, renders nothing chart-like when `similarityScore` is absent.
- `frontend/src/components/ResultCard.tsx` — modify: drop raw summary render, use `summarize.ts`, wire in the three charts, Turkish section titles via `labels.ts`.
- `frontend/src/components/StatusBadge.tsx`, `HypothesisList.tsx`, `ActionsList.tsx`, `KnowledgeReferences.tsx`, `EvidenceLedger.tsx`, `IncidentDecisionPanel.tsx` — modify: Turkish labels via `labels.ts`, no behavior change otherwise.
- `frontend/src/components/ChatMessage.tsx` — new. User bubble + agent bubble (wraps `ResultCard`/`ErrorPanel`).
- `frontend/src/components/TypingIndicator.tsx` — new. Loading bubble.
- `frontend/src/components/ChatComposer.tsx` — new. Textarea (Enter sends, Shift+Enter newline), optional `datetime-local` time-window row. Replaces `QuestionForm.tsx` (deleted).
- `frontend/src/components/Sidebar.tsx` — new. Thread list + "yeni sohbet" button.
- `frontend/src/components/SettingsPanel.tsx` — new. Model picker, quick/thorough toggle, knowledge document list + upload form.
- `frontend/src/components/Header.tsx` — modify: becomes the top navbar with a settings toggle button.
- `frontend/src/App.tsx` — rewrite: three-pane layout, per-thread turn state, wiring session/model/mode into requests.
- `frontend/scripts/validate_palette.js` — new, copied verbatim from the dataviz skill (`node_modules`-free standalone script).
- `frontend/package.json` — add `recharts` dependency.
- Delete: `frontend/src/components/QuestionForm.tsx`.

## Task 1: API layer — types + client functions for M11 endpoints

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/client.ts`
- Modify: `frontend/src/api/client.test.ts`

**Interfaces:**
- Produces (used by every later task): `InvestigationRequest` gains `sessionId?: string`, `modelId?: string`, `mode?: 'quick' | 'thorough'`. New types `ModelsResponse { models: string[] }`, `KnowledgeDocumentSummary { documentId: string; version: string; title: string; documentType: string; effectiveFrom: string }`, `KnowledgeDocumentUploadRequest { title: string; documentType: string; provider?: string; tags?: string[]; effectiveFrom: string; effectiveTo?: string; language?: string; content: string }`, `KnowledgeDocumentUploadResponse { documentId: string; version: string }`. New client functions `listSessionInvestigations(sessionId: string): Promise<Investigation[]>` (GET `/sessions/{sessionId}/investigations`, maps each item through `normalizeInvestigation`), `listModels(): Promise<string[]>` (GET `/models`, returns `.models`), `listKnowledgeDocuments(): Promise<KnowledgeDocumentSummary[]>` (GET `/knowledge/documents`), `uploadKnowledgeDocument(req: KnowledgeDocumentUploadRequest): Promise<KnowledgeDocumentUploadResponse>` (POST `/knowledge/documents`).

- [ ] **Step 1: Extend `InvestigationRequest` and add the new response/request types**

In `frontend/src/api/types.ts`, change:

```ts
export interface InvestigationRequest {
  question: string
  timeWindow?: {
    startAt: string
    endAt: string
  }
  locale?: string
  sessionId?: string
  modelId?: string
  mode?: 'quick' | 'thorough'
}
```

Append at end of file:

```ts
export interface ModelsResponse {
  models: string[]
}

export interface KnowledgeDocumentSummary {
  documentId: string
  version: string
  title: string
  documentType: string
  effectiveFrom: string
}

export interface KnowledgeDocumentUploadRequest {
  title: string
  documentType: string
  provider?: string
  tags?: string[]
  effectiveFrom: string
  effectiveTo?: string
  language?: string
  content: string
}

export interface KnowledgeDocumentUploadResponse {
  documentId: string
  version: string
}
```

- [ ] **Step 2: Write failing tests for the new client functions**

Append to `frontend/src/api/client.test.ts` (new `describe` blocks, same file, same `vi.stubGlobal('fetch', ...)` pattern already used above):

```ts
import {
  listSessionInvestigations,
  listModels,
  listKnowledgeDocuments,
  uploadKnowledgeDocument,
} from './client'

describe('listSessionInvestigations', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('GETs the session thread and normalizes each investigation', async () => {
    const body = [
      {
        investigationId: 'inv-1',
        status: 'ANOMALY_CONFIRMED',
        validation: null,
      },
    ]
    ;(fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => body,
    })

    const result = await listSessionInvestigations('sess-1')

    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/sessions/sess-1/investigations',
      expect.objectContaining({ method: 'GET' })
    )
    expect(result[0].validation).toEqual({ status: 'PASSED', warnings: [] })
  })
})

describe('listModels', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('GETs /models and returns the model id list', async () => {
    ;(fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ models: ['meta/llama-3.1-8b-instruct', 'meta/llama-3.3-70b-instruct'] }),
    })

    const result = await listModels()

    expect(fetch).toHaveBeenCalledWith('/api/v1/models', expect.objectContaining({ method: 'GET' }))
    expect(result).toEqual(['meta/llama-3.1-8b-instruct', 'meta/llama-3.3-70b-instruct'])
  })
})

describe('knowledge documents', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('lists documents', async () => {
    const body = [
      {
        documentId: 'UPLOAD-ABC123',
        version: '1',
        title: 'Operatör B runbook',
        documentType: 'RUNBOOK',
        effectiveFrom: '2026-01-01',
      },
    ]
    ;(fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => body,
    })

    const result = await listKnowledgeDocuments()

    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/knowledge/documents',
      expect.objectContaining({ method: 'GET' })
    )
    expect(result).toEqual(body)
  })

  it('uploads a document', async () => {
    ;(fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      status: 201,
      json: async () => ({ documentId: 'UPLOAD-XYZ', version: '1' }),
    })

    const result = await uploadKnowledgeDocument({
      title: 'Yeni runbook',
      documentType: 'RUNBOOK',
      effectiveFrom: '2026-08-02',
      content: 'İçerik',
    })

    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/knowledge/documents',
      expect.objectContaining({ method: 'POST' })
    )
    expect(result).toEqual({ documentId: 'UPLOAD-XYZ', version: '1' })
  })
})
```

- [ ] **Step 3: Run tests to verify they fail**

Run (from `frontend/`): `npm run test -- client.test.ts`
Expected: FAIL — `listSessionInvestigations`/`listModels`/`listKnowledgeDocuments`/`uploadKnowledgeDocument` are not exported.

- [ ] **Step 4: Implement the client functions**

Append to `frontend/src/api/client.ts`:

```ts
export async function listSessionInvestigations(sessionId: string): Promise<Investigation[]> {
  const raw = await request<Investigation[]>(`/sessions/${sessionId}/investigations`, {
    method: 'GET',
  })
  return raw.map(normalizeInvestigation)
}

export async function listModels(): Promise<string[]> {
  const raw = await request<ModelsResponse>('/models', { method: 'GET' })
  return raw.models
}

export function listKnowledgeDocuments(): Promise<KnowledgeDocumentSummary[]> {
  return request<KnowledgeDocumentSummary[]>('/knowledge/documents', { method: 'GET' })
}

export function uploadKnowledgeDocument(
  req: KnowledgeDocumentUploadRequest
): Promise<KnowledgeDocumentUploadResponse> {
  return request<KnowledgeDocumentUploadResponse>('/knowledge/documents', {
    method: 'POST',
    body: JSON.stringify(req),
  })
}
```

Add `ModelsResponse`, `KnowledgeDocumentSummary`, `KnowledgeDocumentUploadRequest`, `KnowledgeDocumentUploadResponse` to the existing `import type { ... } from './types'` at the top of the file.

- [ ] **Step 5: Run tests to verify they pass**

Run: `npm run test -- client.test.ts`
Expected: PASS, all suites green.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/api/types.ts frontend/src/api/client.ts frontend/src/api/client.test.ts
git commit -m "feat(frontend): add session/models/knowledge client functions for M12"
```

---

## Task 2: Turkish label dictionary + summary synthesizer

**Files:**
- Create: `frontend/src/lib/labels.ts`
- Create: `frontend/src/lib/summarize.ts`
- Create: `frontend/src/lib/summarize.test.ts`

**Interfaces:**
- Consumes: `Investigation`, `InvestigationStatus`, `Severity`, `Hypothesis` from `frontend/src/api/types.ts` (Task 1, unchanged shapes for these).
- Produces (used by Tasks 5, 6, 7, 8, 9): from `labels.ts` — `STATUS_LABEL_TR: Record<InvestigationStatus, string>`, `SEVERITY_LABEL_TR: Record<Severity, string>`, `PROBABILITY_LABEL_TR: Record<Hypothesis['probability'], string>`, `MODE_LABEL_TR: Record<'quick' | 'thorough', string>`, `DOCUMENT_TYPE_LABEL_TR: Record<string, string>` (keys: the 5 `KnowledgeDocumentType` enum names), `UI_TEXT: Record<string, string>` (static strings — grows as later tasks need entries, see each task), `formatNumber(n: number, fractionDigits?: number): string` (`tr-TR` `Intl.NumberFormat`), `formatDateTime(iso: string): string` (`tr-TR` `Intl.DateTimeFormat`). From `summarize.ts` — `synthesizeSummary(investigation: Investigation): string`.

- [ ] **Step 1: Write the failing test for `synthesizeSummary`**

```ts
// frontend/src/lib/summarize.test.ts
import { describe, it, expect } from 'vitest'
import { synthesizeSummary } from './summarize'
import type { Investigation } from '../api/types'

const BASE: Investigation = {
  investigationId: 'inv-1',
  status: 'ANOMALY_CONFIRMED',
  severity: 'HIGH',
  summary: 'ANOMALY_CONFIRMED',
  timeWindow: { startAt: '2026-07-30T11:15:00Z', endAt: '2026-07-30T11:30:00Z' },
  evidence: [],
  hypotheses: [
    {
      rank: 1,
      possibleCause: 'Gateway bağlantı havuzunda kapasite problemi',
      probability: 'HIGH',
      supportingEvidenceIds: [],
      verificationSteps: [],
    },
    {
      rank: 2,
      possibleCause: 'İkinci olası neden',
      probability: 'LOW',
      supportingEvidenceIds: [],
      verificationSteps: [],
    },
  ],
  recommendedActions: [],
  knowledgeReferences: [],
  confidence: 0.87,
  approvalRequired: true,
  validation: { status: 'PASSED', warnings: [] },
}

describe('synthesizeSummary', () => {
  it('combines Turkish status, severity and the top-ranked hypothesis', () => {
    const result = synthesizeSummary(BASE)
    expect(result).toContain('Anomali doğrulandı')
    expect(result).toContain('Kritik önem') // HIGH severity label — see labels.ts
    expect(result).toContain('Gateway bağlantı havuzunda kapasite problemi')
    expect(result).not.toContain('İkinci olası neden')
  })

  it('handles NO_ANOMALY with no hypotheses and null severity', () => {
    const result = synthesizeSummary({
      ...BASE,
      status: 'NO_ANOMALY',
      severity: null,
      hypotheses: [],
    })
    expect(result).toContain('Anomali tespit edilmedi')
    expect(result).not.toContain('null')
    expect(result).not.toContain('undefined')
  })

  it('handles FAILED status', () => {
    const result = synthesizeSummary({ ...BASE, status: 'FAILED', hypotheses: [] })
    expect(result).toContain('Analiz başarısız oldu')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm run test -- summarize.test.ts`
Expected: FAIL — `frontend/src/lib/summarize.ts` does not exist.

- [ ] **Step 3: Write `labels.ts`**

```ts
// frontend/src/lib/labels.ts
import type { InvestigationStatus, Severity, Hypothesis } from '../api/types'

export const STATUS_LABEL_TR: Record<InvestigationStatus, string> = {
  ANOMALY_CONFIRMED: 'Anomali doğrulandı',
  NO_ANOMALY: 'Anomali tespit edilmedi',
  PARTIAL_ANALYSIS: 'Kısmi analiz',
  FAILED: 'Analiz başarısız oldu',
}

export const SEVERITY_LABEL_TR: Record<Severity, string> = {
  LOW: 'Düşük önem',
  MEDIUM: 'Orta önem',
  HIGH: 'Kritik önem',
  CRITICAL: 'Acil önem',
}

export const PROBABILITY_LABEL_TR: Record<Hypothesis['probability'], string> = {
  HIGH: 'Yüksek olasılık',
  MEDIUM: 'Orta olasılık',
  LOW: 'Düşük olasılık',
}

export const MODE_LABEL_TR: Record<'quick' | 'thorough', string> = {
  quick: 'Hızlı',
  thorough: 'Detaylı',
}

export const DOCUMENT_TYPE_LABEL_TR: Record<string, string> = {
  INCIDENT_POSTMORTEM: 'Olay sonrası analiz (postmortem)',
  RUNBOOK: 'Runbook',
  ERROR_REFERENCE: 'Hata referansı',
  PROVIDER_PLAYBOOK: 'Operatör oyun kitabı',
  CHANGE_POLICY: 'Değişiklik politikası',
}

export const UI_TEXT = {
  appName: 'OTP Sentinel',
  appTagline: 'olay inceleme konsolu',
  newChat: 'Yeni sohbet',
  emptyThreadList: 'Henüz sohbet yok',
  composerPlaceholder: 'Ne araştırmak istersiniz? (Enter ile gönder, Shift+Enter yeni satır)',
  send: 'Gönder',
  investigating: 'İnceleniyor…',
  investigatingDetail:
    'Canlı metrik, hata, kuyruk ve operatör verileri toplanıyor, ardından geçmiş olaylarla karşılaştırılıyor. Gerçek bir analiz bir dakikaya kadar sürebilir — bu önbelleğe alınmış bir sonuç değildir.',
  settings: 'Ayarlar',
  closeSettings: 'Kapat',
  modelLabel: 'Model',
  modeLabel: 'Mod',
  knowledgeSectionTitle: 'Bilgi tabanı',
  knowledgeListEmpty: 'Henüz yüklenmiş belge yok.',
  uploadTitle: 'Belge yükle',
  uploadTitleField: 'Başlık',
  uploadTypeField: 'Tür',
  uploadProviderField: 'Operatör (opsiyonel)',
  uploadTagsField: 'Etiketler (virgülle ayırın, opsiyonel)',
  uploadEffectiveFromField: 'Geçerlilik başlangıcı',
  uploadEffectiveToField: 'Geçerlilik bitişi (opsiyonel)',
  uploadLanguageField: 'Dil (opsiyonel, varsayılan tr)',
  uploadContentField: 'İçerik',
  uploadSubmit: 'Yükle',
  uploadSuccess: 'Belge yüklendi',
  timeWindowToggle: 'Zaman aralığı belirt (aksi halde sorudan çözülür)',
  timeWindowStart: 'Başlangıç',
  timeWindowEnd: 'Bitiş',
  evidenceSection: 'Kanıtlar',
  hypothesesSection: 'Hipotezler',
  actionsSection: 'Önerilen aksiyonlar',
  knowledgeRefsSection: 'İlgili geçmiş olaylar',
  confidenceLabel: 'Güven',
  noEvidence: 'Kanıt toplanmadı.',
  noHypotheses: 'Hipotez üretilmedi.',
  noActions: 'Aksiyon önerilmedi.',
  noKnowledgeRefs: 'Benzer geçmiş olay bulunamadı.',
  approvalRequired: 'onay gerekli',
  riskLabel: 'risk',
  errorTitle: 'İnceleme başlatılamadı',
} as const

export function formatNumber(n: number, fractionDigits = 2): string {
  return new Intl.NumberFormat('tr-TR', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(n)
}

export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat('tr-TR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(iso))
}
```

- [ ] **Step 4: Write `summarize.ts`**

```ts
// frontend/src/lib/summarize.ts
import type { Investigation } from '../api/types'
import { STATUS_LABEL_TR, SEVERITY_LABEL_TR } from './labels'

export function synthesizeSummary(investigation: Investigation): string {
  const parts = [STATUS_LABEL_TR[investigation.status]]

  if (investigation.severity) {
    parts.push(SEVERITY_LABEL_TR[investigation.severity])
  }

  const top = investigation.hypotheses.find((h) => h.rank === 1) ?? investigation.hypotheses[0]
  if (top) {
    parts.push(`en olası neden: ${top.possibleCause}`)
  }

  return parts.join(' — ')
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npm run test -- summarize.test.ts`
Expected: PASS, all 3 cases green.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/lib/labels.ts frontend/src/lib/summarize.ts frontend/src/lib/summarize.test.ts
git commit -m "feat(frontend): Turkish label dictionary and summary synthesizer"
```

---

## Task 3: Client-side session/thread store

**Files:**
- Create: `frontend/src/lib/sessionStore.ts`
- Create: `frontend/src/lib/sessionStore.test.ts`

**Interfaces:**
- Consumes: nothing beyond `crypto.randomUUID()` and `localStorage` (both available in the Vite/jsdom test env already configured — see `frontend/src/api/client.test.ts` for the existing vitest setup pattern).
- Produces (used by Tasks 7, 9): `interface SessionMeta { sessionId: string; title: string; createdAt: string }`, `listSessions(): SessionMeta[]` (newest first), `createSession(): SessionMeta` (generates `crypto.randomUUID()`, title `'Yeni sohbet'`, persists, returns it), `renameSession(sessionId: string, title: string): void` (truncates to 60 chars), `recordQuestion(sessionId: string, investigationId: string, question: string): void`, `getRecordedQuestion(sessionId: string, investigationId: string): string | undefined`.

- [ ] **Step 1: Write the failing tests**

```ts
// frontend/src/lib/sessionStore.test.ts
import { describe, it, expect, beforeEach } from 'vitest'
import {
  listSessions,
  createSession,
  renameSession,
  recordQuestion,
  getRecordedQuestion,
} from './sessionStore'

describe('sessionStore', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('createSession persists a new thread and listSessions returns it newest-first', () => {
    const first = createSession()
    const second = createSession()

    const sessions = listSessions()

    expect(sessions[0].sessionId).toBe(second.sessionId)
    expect(sessions[1].sessionId).toBe(first.sessionId)
    expect(sessions[0].title).toBe('Yeni sohbet')
  })

  it('renameSession updates the title and truncates to 60 chars', () => {
    const session = createSession()
    renameSession(session.sessionId, 'x'.repeat(100))

    const sessions = listSessions()

    expect(sessions[0].title).toHaveLength(60)
  })

  it('records and retrieves a question by investigationId, scoped to sessionId', () => {
    const session = createSession()
    recordQuestion(session.sessionId, 'inv-1', 'Neden düştü?')

    expect(getRecordedQuestion(session.sessionId, 'inv-1')).toBe('Neden düştü?')
    expect(getRecordedQuestion(session.sessionId, 'inv-does-not-exist')).toBeUndefined()
    expect(getRecordedQuestion('other-session', 'inv-1')).toBeUndefined()
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm run test -- sessionStore.test.ts`
Expected: FAIL — module not found.

- [ ] **Step 3: Implement `sessionStore.ts`**

```ts
// frontend/src/lib/sessionStore.ts
export interface SessionMeta {
  sessionId: string
  title: string
  createdAt: string
}

const SESSIONS_KEY = 'otp-sentinel:sessions'
const questionKey = (sessionId: string) => `otp-sentinel:questions:${sessionId}`

function readSessions(): SessionMeta[] {
  const raw = localStorage.getItem(SESSIONS_KEY)
  if (!raw) return []
  try {
    return JSON.parse(raw) as SessionMeta[]
  } catch {
    return []
  }
}

function writeSessions(sessions: SessionMeta[]): void {
  localStorage.setItem(SESSIONS_KEY, JSON.stringify(sessions))
}

export function listSessions(): SessionMeta[] {
  return readSessions().sort((a, b) => b.createdAt.localeCompare(a.createdAt))
}

export function createSession(): SessionMeta {
  const session: SessionMeta = {
    sessionId: crypto.randomUUID(),
    title: 'Yeni sohbet',
    createdAt: new Date().toISOString(),
  }
  writeSessions([...readSessions(), session])
  return session
}

export function renameSession(sessionId: string, title: string): void {
  const truncated = title.slice(0, 60)
  writeSessions(
    readSessions().map((s) => (s.sessionId === sessionId ? { ...s, title: truncated } : s))
  )
}

export function recordQuestion(sessionId: string, investigationId: string, question: string): void {
  const key = questionKey(sessionId)
  const raw = localStorage.getItem(key)
  const map: Record<string, string> = raw ? JSON.parse(raw) : {}
  map[investigationId] = question
  localStorage.setItem(key, JSON.stringify(map))
}

export function getRecordedQuestion(sessionId: string, investigationId: string): string | undefined {
  const raw = localStorage.getItem(questionKey(sessionId))
  if (!raw) return undefined
  const map: Record<string, string> = JSON.parse(raw)
  return map[investigationId]
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm run test -- sessionStore.test.ts`
Expected: PASS, all 3 cases green.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/lib/sessionStore.ts frontend/src/lib/sessionStore.test.ts
git commit -m "feat(frontend): client-side chat thread store (localStorage)"
```

---

## Task 4: Charts (Recharts) — hypothesis bar, confidence gauge, similarity bar

**Files:**
- Modify: `frontend/package.json` (add `recharts`)
- Create: `frontend/scripts/validate_palette.js` (copy verbatim — see step 1)
- Create: `frontend/src/components/charts/HypothesisChart.tsx`
- Create: `frontend/src/components/charts/HypothesisChart.test.tsx`
- Create: `frontend/src/components/charts/ConfidenceGauge.tsx`
- Create: `frontend/src/components/charts/SimilarityBar.tsx`
- Create: `frontend/src/components/charts/SimilarityBar.test.tsx`

**Interfaces:**
- Consumes: `Hypothesis[]`, `KnowledgeReference[]`, `number | null` (confidence) from `frontend/src/api/types.ts`; `PROBABILITY_LABEL_TR`, `formatNumber` from `frontend/src/lib/labels.ts` (Task 2).
- Produces (used by Task 5): `HypothesisChart({ hypotheses }: { hypotheses: Hypothesis[] })`, `ConfidenceGauge({ confidence }: { confidence: number | null })`, `SimilarityBar({ reference }: { reference: KnowledgeReference })` (renders `null`-safe: returns a plain text fallback, not a bar, when `similarityScore` is undefined — the real backend never populates it today, see plan header).

- [ ] **Step 1: Add the `recharts` dependency and copy the palette validator**

```bash
cd frontend && npm install recharts@^2.15.0
```

Copy the validator script from the dataviz skill's bundled `scripts/validate_palette.js` (same skill loaded for this plan) into `frontend/scripts/validate_palette.js` verbatim — it is a standalone, dependency-free Node script (no import from the skill dir at runtime). Read it once from the skill's `scripts/validate_palette.js` and write an identical copy at `frontend/scripts/validate_palette.js`.

- [ ] **Step 2: Derive and validate the ordinal probability ramp**

The three `Hypothesis.probability` values (`LOW`/`MEDIUM`/`HIGH`) are an **ordinal** ramp (order carries meaning — see dataviz `color-formula.md`), one hue (blue, matching `--color-signal: #1D4ED8`), monotone lightness. Candidate steps: `LOW = "#B7CDF3"`, `MEDIUM = "#5B87DE"`, `HIGH = "#1D4ED8"`.

Run from `frontend/`:

```bash
node scripts/validate_palette.js "#B7CDF3,#5B87DE,#1D4ED8" --mode light --ordinal
```

Expected: PASS (monotone L, adjacent ΔL ≥ 0.06, light-end contrast ≥ 2:1 vs the `--color-paper` surface `#F7F7F4`). If it FAILs, apply the skill's snap-to-passing procedure (nudge the failing step's lightness, same hue, re-run) until it passes, then use the passing hex triad in Step 3. Record the final validated triad and the exact PASS output in the task's commit message body.

- [ ] **Step 3: Write the failing test for `HypothesisChart`**

```tsx
// frontend/src/components/charts/HypothesisChart.test.tsx
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { HypothesisChart } from './HypothesisChart'
import type { Hypothesis } from '../../api/types'

const HYPOTHESES: Hypothesis[] = [
  {
    rank: 1,
    possibleCause: 'Gateway bağlantı havuzu',
    probability: 'HIGH',
    supportingEvidenceIds: [],
    verificationSteps: [],
  },
  {
    rank: 2,
    possibleCause: 'Operatör B gecikmesi',
    probability: 'LOW',
    supportingEvidenceIds: [],
    verificationSteps: [],
  },
]

describe('HypothesisChart', () => {
  it('renders one labeled row per hypothesis', () => {
    render(<HypothesisChart hypotheses={HYPOTHESES} />)

    expect(screen.getByText(/Gateway bağlantı havuzu/)).toBeInTheDocument()
    expect(screen.getByText(/Operatör B gecikmesi/)).toBeInTheDocument()
  })

  it('renders nothing when there are no hypotheses', () => {
    const { container } = render(<HypothesisChart hypotheses={[]} />)
    expect(container).toBeEmptyDOMElement()
  })
})
```

- [ ] **Step 4: Run test to verify it fails**

Run: `npm run test -- HypothesisChart.test.tsx`
Expected: FAIL — module not found.

- [ ] **Step 5: Implement `HypothesisChart.tsx`**

Ranked horizontal bar, one bar per hypothesis, length = a fixed value per probability tier (`HIGH=1, MEDIUM=0.66, LOW=0.33`, since the API only exposes a 3-tier enum, not a continuous probability — do not fabricate a fake float), color = the validated ordinal triad from Step 2, direct label = `possibleCause` + `PROBABILITY_LABEL_TR`, thin bars (per dataviz `marks-and-anatomy.md`: 2px gap, rounded ends), no legend needed (single-series, ordinal color already labeled per-row):

```tsx
// frontend/src/components/charts/HypothesisChart.tsx
import { BarChart, Bar, XAxis, YAxis, Cell, ResponsiveContainer, Tooltip } from 'recharts'
import type { Hypothesis } from '../../api/types'
import { PROBABILITY_LABEL_TR } from '../../lib/labels'

const PROBABILITY_VALUE: Record<Hypothesis['probability'], number> = {
  HIGH: 1,
  MEDIUM: 0.66,
  LOW: 0.33,
}

// Ordinal blue ramp validated with scripts/validate_palette.js --ordinal (LOW→HIGH lightness).
const PROBABILITY_COLOR: Record<Hypothesis['probability'], string> = {
  LOW: '#B7CDF3',
  MEDIUM: '#5B87DE',
  HIGH: '#1D4ED8',
}

export function HypothesisChart({ hypotheses }: { hypotheses: Hypothesis[] }) {
  if (hypotheses.length === 0) return null

  const data = [...hypotheses]
    .sort((a, b) => a.rank - b.rank)
    .map((h) => ({
      name: `#${h.rank} ${h.possibleCause}`,
      value: PROBABILITY_VALUE[h.probability],
      probability: h.probability,
    }))

  return (
    <div style={{ width: '100%', height: Math.max(60, data.length * 40) }}>
      <ResponsiveContainer>
        <BarChart data={data} layout="vertical" margin={{ left: 0, right: 16, top: 4, bottom: 4 }}>
          <XAxis type="number" domain={[0, 1]} hide />
          <YAxis
            type="category"
            dataKey="name"
            width={220}
            tick={{ fontSize: 12, fill: 'var(--color-ink)' }}
            axisLine={false}
            tickLine={false}
          />
          <Tooltip
            formatter={(_value, _key, item) =>
              PROBABILITY_LABEL_TR[item.payload.probability as Hypothesis['probability']]
            }
          />
          <Bar dataKey="value" radius={[0, 4, 4, 0]} barSize={14}>
            {data.map((d, i) => (
              <Cell key={i} fill={PROBABILITY_COLOR[d.probability]} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `npm run test -- HypothesisChart.test.tsx`
Expected: PASS.

- [ ] **Step 7: Implement `ConfidenceGauge.tsx` (no separate test — trivial presentational stat-tile, YAGNI on a test for a static render)**

```tsx
// frontend/src/components/charts/ConfidenceGauge.tsx
import { formatNumber } from '../../lib/labels'

export function ConfidenceGauge({ confidence }: { confidence: number | null }) {
  if (confidence === null) return null
  const pct = Math.round(confidence * 100)

  return (
    <div className="inline-flex items-center gap-2 border border-line rounded-md px-3 py-1.5">
      <div className="w-16 h-1.5 rounded-full bg-line overflow-hidden">
        <div className="h-full bg-signal rounded-full" style={{ width: `${pct}%` }} />
      </div>
      <span className="font-mono text-xs text-ink-muted">{formatNumber(confidence, 2)}</span>
    </div>
  )
}
```

- [ ] **Step 8: Write the failing test for `SimilarityBar`**

```tsx
// frontend/src/components/charts/SimilarityBar.test.tsx
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { SimilarityBar } from './SimilarityBar'

describe('SimilarityBar', () => {
  it('renders a bar with the formatted score when similarityScore is present', () => {
    render(
      <SimilarityBar reference={{ documentId: 'INC-2026-041', similarityScore: 0.86 }} />
    )
    expect(screen.getByText('0,86')).toBeInTheDocument()
  })

  it('renders nothing bar-like when similarityScore is absent (real backend today)', () => {
    const { container } = render(<SimilarityBar reference={{ documentId: 'INC-2026-041' }} />)
    expect(container.querySelector('[data-testid="similarity-bar-track"]')).toBeNull()
  })
})
```

- [ ] **Step 9: Run test to verify it fails**

Run: `npm run test -- SimilarityBar.test.tsx`
Expected: FAIL — module not found.

- [ ] **Step 10: Implement `SimilarityBar.tsx`**

```tsx
// frontend/src/components/charts/SimilarityBar.tsx
import type { KnowledgeReference } from '../../api/types'
import { formatNumber } from '../../lib/labels'

export function SimilarityBar({ reference }: { reference: KnowledgeReference }) {
  if (typeof reference.similarityScore !== 'number') return null
  const pct = Math.round(reference.similarityScore * 100)

  return (
    <div className="inline-flex items-center gap-1.5">
      <div
        data-testid="similarity-bar-track"
        className="w-12 h-1.5 rounded-full bg-line overflow-hidden"
      >
        <div className="h-full bg-signal rounded-full" style={{ width: `${pct}%` }} />
      </div>
      <span className="font-mono text-xs text-ink-muted">{formatNumber(reference.similarityScore, 2)}</span>
    </div>
  )
}
```

- [ ] **Step 11: Run test to verify it passes**

Run: `npm run test -- SimilarityBar.test.tsx`
Expected: PASS.

- [ ] **Step 12: Commit**

```bash
git add frontend/package.json frontend/package-lock.json frontend/scripts/validate_palette.js frontend/src/components/charts
git commit -m "feat(frontend): hypothesis/confidence/similarity charts with validated palette"
```

---

## Task 5: Rework `ResultCard` and sub-components for Turkish labels + synthesized summary + charts

**Files:**
- Modify: `frontend/src/components/ResultCard.tsx`
- Modify: `frontend/src/components/StatusBadge.tsx`
- Modify: `frontend/src/components/HypothesisList.tsx`
- Modify: `frontend/src/components/ActionsList.tsx`
- Modify: `frontend/src/components/KnowledgeReferences.tsx`
- Modify: `frontend/src/components/EvidenceLedger.tsx`
- Modify: `frontend/src/components/IncidentDecisionPanel.tsx`
- Create: `frontend/src/components/ResultCard.test.tsx`

**Interfaces:**
- Consumes: `HypothesisChart`, `ConfidenceGauge`, `SimilarityBar` (Task 4); `STATUS_LABEL_TR`, `SEVERITY_LABEL_TR`, `PROBABILITY_LABEL_TR`, `UI_TEXT`, `formatNumber`, `formatDateTime` (Task 2); `synthesizeSummary` (Task 2).
- Produces (used by Task 6): `ResultCard({ investigation }: { investigation: Investigation })` unchanged export signature.

- [ ] **Step 1: Write the failing test for `ResultCard`**

```tsx
// frontend/src/components/ResultCard.test.tsx
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ResultCard } from './ResultCard'
import type { Investigation } from '../api/types'

const INVESTIGATION: Investigation = {
  investigationId: 'inv-1',
  status: 'ANOMALY_CONFIRMED',
  severity: 'HIGH',
  summary: 'ANOMALY_CONFIRMED',
  timeWindow: { startAt: '2026-07-30T11:15:00Z', endAt: '2026-07-30T11:30:00Z' },
  evidence: [],
  hypotheses: [
    {
      rank: 1,
      possibleCause: 'Gateway bağlantı havuzu',
      probability: 'HIGH',
      supportingEvidenceIds: [],
      verificationSteps: [],
    },
  ],
  recommendedActions: [],
  knowledgeReferences: [],
  confidence: 0.87,
  approvalRequired: false,
  validation: { status: 'PASSED', warnings: [] },
}

describe('ResultCard', () => {
  it('never renders the raw enum summary field', () => {
    render(<ResultCard investigation={INVESTIGATION} />)
    expect(screen.queryByText('ANOMALY_CONFIRMED')).not.toBeInTheDocument()
  })

  it('renders the synthesized Turkish summary instead', () => {
    render(<ResultCard investigation={INVESTIGATION} />)
    expect(screen.getByText(/Anomali doğrulandı/)).toBeInTheDocument()
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm run test -- ResultCard.test.tsx`
Expected: FAIL — the raw `"ANOMALY_CONFIRMED"` text currently renders via `<StatusBadge>`'s English label path and `inv.summary`.

- [ ] **Step 3: Update `StatusBadge.tsx`**

Replace `STATUS_LABEL` and the bare `{severity}` with the Turkish dictionary:

```tsx
import type { InvestigationStatus, Severity } from '../api/types'
import { STATUS_LABEL_TR, SEVERITY_LABEL_TR } from '../lib/labels'

const STATUS_STYLE: Record<InvestigationStatus, string> = {
  ANOMALY_CONFIRMED: 'bg-alert-soft text-alert border-alert',
  NO_ANOMALY: 'bg-confirm-soft text-confirm border-confirm',
  PARTIAL_ANALYSIS: 'bg-signal-soft text-signal border-signal',
  FAILED: 'bg-danger-soft text-danger border-danger',
}

export function StatusBadge({
  status,
  severity,
}: {
  status: InvestigationStatus
  severity: Severity | null
}) {
  return (
    <div className="flex items-center gap-2">
      <span
        className={`font-display text-sm px-3 py-1 rounded-full border ${STATUS_STYLE[status]}`}
      >
        {STATUS_LABEL_TR[status]}
      </span>
      {severity && (
        <span className="font-mono text-xs uppercase tracking-wide text-ink-muted border border-line rounded px-2 py-1">
          {SEVERITY_LABEL_TR[severity]}
        </span>
      )}
    </div>
  )
}
```

- [ ] **Step 4: Update `HypothesisList.tsx`** — replace the raw `{h.probability}` badge text with `PROBABILITY_LABEL_TR[h.probability]` (import from `../lib/labels`), keep everything else (the color mapping, evidence-id chips, verification steps) unchanged.

- [ ] **Step 5: Update `ActionsList.tsx`** — replace `risk: {a.risk}` with `` `${UI_TEXT.riskLabel}: ${a.risk}` `` and `requires approval` with `UI_TEXT.approvalRequired`, `No actions were recommended.` with `UI_TEXT.noActions` (import `UI_TEXT` from `../lib/labels`).

- [ ] **Step 6: Update `KnowledgeReferences.tsx`** — replace `No similar historical incidents were found.` with `UI_TEXT.noKnowledgeRefs`, `(similarity {score})` with the new `<SimilarityBar reference={r} />` (import from `./charts/SimilarityBar`) placed where the old inline similarity text was, keep the `documentId`/`title` line as-is (already language-neutral).

- [ ] **Step 7: Update `EvidenceLedger.tsx`** — replace `No evidence was collected.` with `UI_TEXT.noEvidence`, format `item.observedAt` through `formatDateTime` (import from `../lib/labels`) instead of raw ISO string.

- [ ] **Step 8: Update `IncidentDecisionPanel.tsx`** — this component is reachable only after the M12 flow renders a result; translate its static strings (`Incident draft` → `'Olay taslağı'`, `Preview incident draft` → `'Olay taslağını önizle'`, `Loading preview…` → `'Önizleme yükleniyor…'`, `No incident exists yet — this is a preview only.` → `'Henüz bir olay oluşturulmadı — bu yalnızca bir önizleme.'`, `Reason (recorded in the audit trail)` → `'Gerekçe (denetim kaydına işlenir)'`, `Approve`/`Reject` → `'Onayla'`/`'Reddet'`, `Incident {id} created` → `` `Olay ${id} oluşturuldu` ``, `Decision recorded` → `'Karar kaydedildi'`, `Idempotent replay...` → `'Aynı istek tekrar edildi — tekrar kaydedilmedi, kopya oluşturulmadı.'`, `Resubmit with the same idempotency key (demonstrates replay)` → `'Aynı idempotency key ile yeniden gönder (tekrar davranışını gösterir)'`. Keep all logic/state machine untouched — string-only edit.

- [ ] **Step 9: Update `ResultCard.tsx`**

```tsx
import type { Investigation } from '../api/types'
import { StatusBadge } from './StatusBadge'
import { EvidenceLedger } from './EvidenceLedger'
import { HypothesisList } from './HypothesisList'
import { ActionsList } from './ActionsList'
import { KnowledgeReferences } from './KnowledgeReferences'
import { IncidentDecisionPanel } from './IncidentDecisionPanel'
import { HypothesisChart } from './charts/HypothesisChart'
import { ConfidenceGauge } from './charts/ConfidenceGauge'
import { synthesizeSummary } from '../lib/summarize'
import { UI_TEXT } from '../lib/labels'

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mt-6">
      <h2 className="font-display text-sm uppercase tracking-wide text-ink-muted mb-2">{title}</h2>
      {children}
    </section>
  )
}

export function ResultCard({ investigation }: { investigation: Investigation }) {
  const inv = investigation
  return (
    <div className="border border-line rounded-lg p-6">
      <StatusBadge status={inv.status} severity={inv.severity} />
      <p className="mt-3 text-ink">{synthesizeSummary(inv)}</p>

      {inv.validation.warnings.length > 0 && (
        <div className="mt-3 border border-alert bg-alert-soft rounded-md p-3">
          <p className="font-display text-xs uppercase text-alert mb-1">Doğrulama uyarıları</p>
          <ul className="list-disc list-inside text-sm text-ink">
            {inv.validation.warnings.map((w, i) => (
              <li key={i}>{w}</li>
            ))}
          </ul>
        </div>
      )}

      {inv.confidence !== null && (
        <div className="mt-3">
          <p className="font-display text-xs uppercase text-ink-muted mb-1">{UI_TEXT.confidenceLabel}</p>
          <ConfidenceGauge confidence={inv.confidence} />
        </div>
      )}

      <Section title={UI_TEXT.evidenceSection}>
        <EvidenceLedger evidence={inv.evidence} />
      </Section>

      <Section title={UI_TEXT.hypothesesSection}>
        <HypothesisChart hypotheses={inv.hypotheses} />
        <div className="mt-4">
          <HypothesisList hypotheses={inv.hypotheses} />
        </div>
      </Section>

      <Section title={UI_TEXT.actionsSection}>
        <ActionsList actions={inv.recommendedActions} />
      </Section>

      <Section title={UI_TEXT.knowledgeRefsSection}>
        <KnowledgeReferences refs={inv.knowledgeReferences} />
      </Section>

      {inv.status !== 'FAILED' && (
        <IncidentDecisionPanel investigationId={inv.investigationId} />
      )}
    </div>
  )
}
```

- [ ] **Step 10: Run tests to verify they pass**

Run: `npm run test`
Expected: PASS, full suite green (existing `HypothesisList`/etc. have no dedicated test files today, so only `ResultCard.test.tsx` + everything from Tasks 1-4 must be green — confirm no regressions).

- [ ] **Step 11: Commit**

```bash
git add frontend/src/components/ResultCard.tsx frontend/src/components/StatusBadge.tsx frontend/src/components/HypothesisList.tsx frontend/src/components/ActionsList.tsx frontend/src/components/KnowledgeReferences.tsx frontend/src/components/EvidenceLedger.tsx frontend/src/components/IncidentDecisionPanel.tsx frontend/src/components/ResultCard.test.tsx
git commit -m "feat(frontend): Turkish labels, synthesized summary, charts in ResultCard"
```

---

## Task 6: Chat bubbles — `ChatMessage`, `TypingIndicator`, `ChatComposer`

**Files:**
- Create: `frontend/src/components/ChatMessage.tsx`
- Create: `frontend/src/components/TypingIndicator.tsx`
- Create: `frontend/src/components/ChatComposer.tsx`
- Create: `frontend/src/components/ChatComposer.test.tsx`
- Delete: `frontend/src/components/QuestionForm.tsx`

**Interfaces:**
- Consumes: `ResultCard` (Task 5), `ErrorPanel` (existing, unmodified), `UI_TEXT` (Task 2), `InvestigationRequest` type (Task 1).
- Produces (used by Task 9): `export interface ChatTurn { id: string; question: string; status: 'pending' | 'done' | 'error'; investigation?: Investigation; errorMessage?: string }` (declared in `ChatMessage.tsx`, imported by `App.tsx`), `ChatMessage({ turn }: { turn: ChatTurn })`, `TypingIndicator()`, `ChatComposer({ disabled, onSubmit }: { disabled: boolean; onSubmit: (question: string, timeWindow?: { startAt: string; endAt: string }) => void })`.

- [ ] **Step 1: Write the failing tests for `ChatComposer`**

```tsx
// frontend/src/components/ChatComposer.test.tsx
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ChatComposer } from './ChatComposer'

describe('ChatComposer', () => {
  it('submits on Enter and clears the textarea', () => {
    const onSubmit = vi.fn()
    render(<ChatComposer disabled={false} onSubmit={onSubmit} />)

    const textarea = screen.getByPlaceholderText(/Ne araştırmak istersiniz/)
    fireEvent.change(textarea, { target: { value: 'OTP oranı neden düştü?' } })
    fireEvent.keyDown(textarea, { key: 'Enter', code: 'Enter' })

    expect(onSubmit).toHaveBeenCalledWith('OTP oranı neden düştü?', undefined)
    expect((textarea as HTMLTextAreaElement).value).toBe('')
  })

  it('does not submit on Shift+Enter (newline instead)', () => {
    const onSubmit = vi.fn()
    render(<ChatComposer disabled={false} onSubmit={onSubmit} />)

    const textarea = screen.getByPlaceholderText(/Ne araştırmak istersiniz/)
    fireEvent.change(textarea, { target: { value: 'satır 1' } })
    fireEvent.keyDown(textarea, { key: 'Enter', code: 'Enter', shiftKey: true })

    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('does not submit an empty/whitespace-only question', () => {
    const onSubmit = vi.fn()
    render(<ChatComposer disabled={false} onSubmit={onSubmit} />)

    const textarea = screen.getByPlaceholderText(/Ne araştırmak istersiniz/)
    fireEvent.change(textarea, { target: { value: '   ' } })
    fireEvent.keyDown(textarea, { key: 'Enter', code: 'Enter' })

    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('includes the time window when the toggle is checked', () => {
    const onSubmit = vi.fn()
    render(<ChatComposer disabled={false} onSubmit={onSubmit} />)

    fireEvent.click(screen.getByLabelText(/Zaman aralığı belirt/))
    const [startInput, endInput] = screen.getAllByDisplayValue('') as HTMLInputElement[]
    fireEvent.change(startInput, { target: { value: '2026-07-30T11:15' } })
    fireEvent.change(endInput, { target: { value: '2026-07-30T11:30' } })

    const textarea = screen.getByPlaceholderText(/Ne araştırmak istersiniz/)
    fireEvent.change(textarea, { target: { value: 'soru' } })
    fireEvent.keyDown(textarea, { key: 'Enter', code: 'Enter' })

    expect(onSubmit).toHaveBeenCalledWith('soru', {
      startAt: '2026-07-30T11:15:00Z',
      endAt: '2026-07-30T11:30:00Z',
    })
  })
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npm run test -- ChatComposer.test.tsx`
Expected: FAIL — module not found.

- [ ] **Step 3: Implement `ChatComposer.tsx`**

The time-window inputs are `type="datetime-local"` (native, replaces the old checkbox+free-text-ISO pattern from the deleted `QuestionForm`); their `value` (`YYYY-MM-DDTHH:mm`, no timezone) is treated as UTC wall-clock and serialized by appending `:00Z`, consistent with the console's existing "(UTC)"-labeled convention:

```tsx
// frontend/src/components/ChatComposer.tsx
import { useState, type KeyboardEvent } from 'react'
import { UI_TEXT } from '../lib/labels'

interface Props {
  disabled: boolean
  onSubmit: (question: string, timeWindow?: { startAt: string; endAt: string }) => void
}

export function ChatComposer({ disabled, onSubmit }: Props) {
  const [question, setQuestion] = useState('')
  const [useTimeWindow, setUseTimeWindow] = useState(false)
  const [startAt, setStartAt] = useState('')
  const [endAt, setEndAt] = useState('')

  function submit() {
    const trimmed = question.trim()
    if (trimmed.length === 0 || disabled) return
    const timeWindow =
      useTimeWindow && startAt && endAt
        ? { startAt: `${startAt}:00Z`, endAt: `${endAt}:00Z` }
        : undefined
    onSubmit(trimmed, timeWindow)
    setQuestion('')
  }

  function handleKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      submit()
    }
  }

  return (
    <div className="border border-line rounded-lg p-3 bg-white/40">
      <textarea
        value={question}
        onChange={(e) => setQuestion(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={UI_TEXT.composerPlaceholder}
        rows={2}
        disabled={disabled}
        className="w-full resize-none border-0 bg-transparent p-1 text-sm text-ink focus:outline-none"
      />

      <div className="mt-2 flex items-center gap-2">
        <input
          id="useTimeWindow"
          type="checkbox"
          checked={useTimeWindow}
          onChange={(e) => setUseTimeWindow(e.target.checked)}
          className="accent-signal"
        />
        <label htmlFor="useTimeWindow" className="text-xs text-ink-muted">
          {UI_TEXT.timeWindowToggle}
        </label>
      </div>

      {useTimeWindow && (
        <div className="mt-2 grid grid-cols-2 gap-2">
          <div>
            <label htmlFor="startAt" className="block text-xs text-ink-muted mb-1">
              {UI_TEXT.timeWindowStart} (UTC)
            </label>
            <input
              id="startAt"
              type="datetime-local"
              value={startAt}
              onChange={(e) => setStartAt(e.target.value)}
              className="w-full border border-line-strong rounded-md p-1.5 font-mono text-xs text-ink bg-paper focus:outline-none focus:ring-2 focus:ring-signal"
            />
          </div>
          <div>
            <label htmlFor="endAt" className="block text-xs text-ink-muted mb-1">
              {UI_TEXT.timeWindowEnd} (UTC)
            </label>
            <input
              id="endAt"
              type="datetime-local"
              value={endAt}
              onChange={(e) => setEndAt(e.target.value)}
              className="w-full border border-line-strong rounded-md p-1.5 font-mono text-xs text-ink bg-paper focus:outline-none focus:ring-2 focus:ring-signal"
            />
          </div>
        </div>
      )}

      <div className="mt-2 flex justify-end">
        <button
          type="button"
          onClick={submit}
          disabled={disabled}
          className="bg-signal text-white font-display text-sm px-4 py-1.5 rounded-md hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {disabled ? UI_TEXT.investigating : UI_TEXT.send}
        </button>
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npm run test -- ChatComposer.test.tsx`
Expected: PASS, all 4 cases green.

- [ ] **Step 5: Implement `TypingIndicator.tsx` (no test — trivial static markup, YAGNI)**

```tsx
// frontend/src/components/TypingIndicator.tsx
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
```

- [ ] **Step 6: Implement `ChatMessage.tsx` (no dedicated test — thin composition of already-tested `ResultCard`/`ErrorPanel`/`TypingIndicator`; covered end-to-end by Task 9's `App.test.tsx`)**

```tsx
// frontend/src/components/ChatMessage.tsx
import type { Investigation } from '../api/types'
import { ResultCard } from './ResultCard'
import { ErrorPanel } from './ErrorPanel'
import { TypingIndicator } from './TypingIndicator'

export interface ChatTurn {
  id: string
  question: string
  status: 'pending' | 'done' | 'error'
  investigation?: Investigation
  errorMessage?: string
}

export function ChatMessage({ turn }: { turn: ChatTurn }) {
  return (
    <div className="space-y-3">
      <div className="flex justify-end">
        <div className="max-w-[75%] bg-signal text-white rounded-lg rounded-br-sm px-4 py-2.5 text-sm">
          {turn.question}
        </div>
      </div>

      <div className="flex justify-start">
        <div className="max-w-[85%] w-full">
          {turn.status === 'pending' && <TypingIndicator />}
          {turn.status === 'error' && turn.errorMessage && (
            <ErrorPanel message={turn.errorMessage} />
          )}
          {turn.status === 'done' && turn.investigation && (
            <ResultCard investigation={turn.investigation} />
          )}
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 7: Delete the superseded form and run the full test suite**

```bash
rm frontend/src/components/QuestionForm.tsx
```

Run: `npm run test`
Expected: PASS (no test file imports `QuestionForm`, per Task 1-5 file lists — confirm before deleting by grepping `QuestionForm` under `frontend/src`).

- [ ] **Step 8: Commit**

```bash
git add -A frontend/src/components/ChatMessage.tsx frontend/src/components/TypingIndicator.tsx frontend/src/components/ChatComposer.tsx frontend/src/components/ChatComposer.test.tsx
git rm frontend/src/components/QuestionForm.tsx
git commit -m "feat(frontend): chat bubble + composer components, remove single-page form"
```

---

## Task 7: `Sidebar` — thread list + new chat

**Files:**
- Create: `frontend/src/components/Sidebar.tsx`
- Create: `frontend/src/components/Sidebar.test.tsx`

**Interfaces:**
- Consumes: `SessionMeta`, `listSessions` (Task 3); `UI_TEXT` (Task 2).
- Produces (used by Task 9): `Sidebar({ sessions, activeSessionId, onSelect, onNewChat }: { sessions: SessionMeta[]; activeSessionId: string | null; onSelect: (sessionId: string) => void; onNewChat: () => void })`.

- [ ] **Step 1: Write the failing tests**

```tsx
// frontend/src/components/Sidebar.test.tsx
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { Sidebar } from './Sidebar'

const SESSIONS = [
  { sessionId: 's-2', title: 'İkinci sohbet', createdAt: '2026-08-02T10:00:00Z' },
  { sessionId: 's-1', title: 'İlk sohbet', createdAt: '2026-08-01T10:00:00Z' },
]

describe('Sidebar', () => {
  it('renders every session title and marks the active one', () => {
    render(
      <Sidebar sessions={SESSIONS} activeSessionId="s-1" onSelect={vi.fn()} onNewChat={vi.fn()} />
    )

    expect(screen.getByText('İkinci sohbet')).toBeInTheDocument()
    expect(screen.getByText('İlk sohbet').closest('button')).toHaveAttribute(
      'aria-current',
      'true'
    )
  })

  it('calls onSelect with the clicked session id', () => {
    const onSelect = vi.fn()
    render(
      <Sidebar sessions={SESSIONS} activeSessionId="s-1" onSelect={onSelect} onNewChat={vi.fn()} />
    )

    fireEvent.click(screen.getByText('İkinci sohbet'))

    expect(onSelect).toHaveBeenCalledWith('s-2')
  })

  it('calls onNewChat when the new-chat button is clicked', () => {
    const onNewChat = vi.fn()
    render(
      <Sidebar sessions={[]} activeSessionId={null} onSelect={vi.fn()} onNewChat={onNewChat} />
    )

    fireEvent.click(screen.getByText('Yeni sohbet'))

    expect(onNewChat).toHaveBeenCalled()
  })

  it('shows the empty-list message when there are no sessions', () => {
    render(<Sidebar sessions={[]} activeSessionId={null} onSelect={vi.fn()} onNewChat={vi.fn()} />)
    expect(screen.getByText('Henüz sohbet yok')).toBeInTheDocument()
  })
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npm run test -- Sidebar.test.tsx`
Expected: FAIL — module not found.

- [ ] **Step 3: Implement `Sidebar.tsx`**

```tsx
// frontend/src/components/Sidebar.tsx
import type { SessionMeta } from '../lib/sessionStore'
import { UI_TEXT } from '../lib/labels'

interface Props {
  sessions: SessionMeta[]
  activeSessionId: string | null
  onSelect: (sessionId: string) => void
  onNewChat: () => void
}

export function Sidebar({ sessions, activeSessionId, onSelect, onNewChat }: Props) {
  return (
    <aside className="w-64 shrink-0 border-r border-line flex flex-col h-full">
      <div className="p-3 border-b border-line">
        <button
          type="button"
          onClick={onNewChat}
          className="w-full border border-signal text-signal font-display text-sm px-3 py-2 rounded-md hover:bg-signal-soft"
        >
          + {UI_TEXT.newChat}
        </button>
      </div>
      <nav className="flex-1 overflow-y-auto p-2 space-y-1">
        {sessions.length === 0 && (
          <p className="text-xs text-ink-muted px-2 py-4 text-center">{UI_TEXT.emptyThreadList}</p>
        )}
        {sessions.map((s) => (
          <button
            key={s.sessionId}
            type="button"
            aria-current={s.sessionId === activeSessionId}
            onClick={() => onSelect(s.sessionId)}
            className={`w-full text-left truncate text-sm px-3 py-2 rounded-md ${
              s.sessionId === activeSessionId
                ? 'bg-signal-soft text-signal'
                : 'text-ink hover:bg-line/40'
            }`}
          >
            {s.title}
          </button>
        ))}
      </nav>
    </aside>
  )
}
```

Note `SessionMeta` must be exported from `frontend/src/lib/sessionStore.ts` (already `export interface SessionMeta` per Task 3, Step 3 — no change needed there).

- [ ] **Step 4: Run tests to verify they pass**

Run: `npm run test -- Sidebar.test.tsx`
Expected: PASS, all 4 cases green.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/Sidebar.tsx frontend/src/components/Sidebar.test.tsx
git commit -m "feat(frontend): chat thread sidebar"
```

---

## Task 8: `SettingsPanel` — model picker, mode toggle, knowledge base

**Files:**
- Create: `frontend/src/components/SettingsPanel.tsx`
- Create: `frontend/src/components/SettingsPanel.test.tsx`

**Interfaces:**
- Consumes: `listModels`, `listKnowledgeDocuments`, `uploadKnowledgeDocument` (Task 1); `MODE_LABEL_TR`, `DOCUMENT_TYPE_LABEL_TR`, `UI_TEXT` (Task 2); `toUserMessage` (existing `frontend/src/lib/errors.ts`, unmodified).
- Produces (used by Task 9): `SettingsPanel({ modelId, onModelChange, mode, onModeChange, onClose }: { modelId: string | null; onModelChange: (modelId: string) => void; mode: 'quick' | 'thorough'; onModeChange: (mode: 'quick' | 'thorough') => void; onClose: () => void })`.

- [ ] **Step 1: Write the failing tests**

```tsx
// frontend/src/components/SettingsPanel.test.tsx
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { SettingsPanel } from './SettingsPanel'

describe('SettingsPanel', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn((url: string) => {
      if (url.includes('/models')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({ models: ['meta/llama-3.1-8b-instruct', 'meta/llama-3.3-70b-instruct'] }),
        })
      }
      if (url.includes('/knowledge/documents')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => [],
        })
      }
      return Promise.reject(new Error(`unexpected fetch ${url}`))
    }))
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('loads and lists the verified models', async () => {
    render(
      <SettingsPanel
        modelId={null}
        onModelChange={vi.fn()}
        mode="thorough"
        onModeChange={vi.fn()}
        onClose={vi.fn()}
      />
    )

    await waitFor(() =>
      expect(screen.getByText('meta/llama-3.1-8b-instruct')).toBeInTheDocument()
    )
    expect(screen.getByText('meta/llama-3.3-70b-instruct')).toBeInTheDocument()
  })

  it('calls onModeChange when the quick/thorough toggle changes', async () => {
    const onModeChange = vi.fn()
    render(
      <SettingsPanel
        modelId={null}
        onModelChange={vi.fn()}
        mode="thorough"
        onModeChange={onModeChange}
        onClose={vi.fn()}
      />
    )

    fireEvent.click(await screen.findByLabelText('Hızlı'))

    expect(onModeChange).toHaveBeenCalledWith('quick')
  })
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npm run test -- SettingsPanel.test.tsx`
Expected: FAIL — module not found.

- [ ] **Step 3: Implement `SettingsPanel.tsx`**

```tsx
// frontend/src/components/SettingsPanel.tsx
import { useEffect, useState } from 'react'
import {
  listModels,
  listKnowledgeDocuments,
  uploadKnowledgeDocument,
} from '../api/client'
import { toUserMessage } from '../lib/errors'
import { MODE_LABEL_TR, DOCUMENT_TYPE_LABEL_TR, UI_TEXT } from '../lib/labels'
import type { KnowledgeDocumentSummary } from '../api/types'

const DOCUMENT_TYPES = Object.keys(DOCUMENT_TYPE_LABEL_TR)

interface Props {
  modelId: string | null
  onModelChange: (modelId: string) => void
  mode: 'quick' | 'thorough'
  onModeChange: (mode: 'quick' | 'thorough') => void
  onClose: () => void
}

export function SettingsPanel({ modelId, onModelChange, mode, onModeChange, onClose }: Props) {
  const [models, setModels] = useState<string[]>([])
  const [documents, setDocuments] = useState<KnowledgeDocumentSummary[]>([])
  const [uploadError, setUploadError] = useState<string | null>(null)
  const [uploadOk, setUploadOk] = useState(false)
  const [title, setTitle] = useState('')
  const [documentType, setDocumentType] = useState(DOCUMENT_TYPES[0])
  const [effectiveFrom, setEffectiveFrom] = useState('')
  const [content, setContent] = useState('')

  useEffect(() => {
    listModels().then(setModels).catch(() => setModels([]))
    refreshDocuments()
  }, [])

  function refreshDocuments() {
    listKnowledgeDocuments().then(setDocuments).catch(() => setDocuments([]))
  }

  async function handleUpload(e: React.FormEvent) {
    e.preventDefault()
    setUploadError(null)
    setUploadOk(false)
    try {
      await uploadKnowledgeDocument({ title, documentType, effectiveFrom, content })
      setUploadOk(true)
      setTitle('')
      setContent('')
      refreshDocuments()
    } catch (err) {
      setUploadError(toUserMessage(err))
    }
  }

  return (
    <div className="fixed inset-0 bg-ink/20 flex justify-end z-10">
      <div className="w-96 bg-paper h-full border-l border-line overflow-y-auto p-5">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-display text-lg">{UI_TEXT.settings}</h2>
          <button type="button" onClick={onClose} className="text-ink-muted text-sm">
            {UI_TEXT.closeSettings}
          </button>
        </div>

        <section className="mb-6">
          <label htmlFor="model" className="block font-display text-sm mb-2">
            {UI_TEXT.modelLabel}
          </label>
          <select
            id="model"
            value={modelId ?? ''}
            onChange={(e) => onModelChange(e.target.value)}
            className="w-full border border-line-strong rounded-md p-2 text-sm bg-paper"
          >
            {models.map((m) => (
              <option key={m} value={m}>
                {m}
              </option>
            ))}
          </select>
        </section>

        <section className="mb-6">
          <p className="font-display text-sm mb-2">{UI_TEXT.modeLabel}</p>
          <div className="flex gap-4">
            {(['quick', 'thorough'] as const).map((m) => (
              <label key={m} className="flex items-center gap-1.5 text-sm">
                <input
                  type="radio"
                  name="mode"
                  checked={mode === m}
                  onChange={() => onModeChange(m)}
                  className="accent-signal"
                />
                {MODE_LABEL_TR[m]}
              </label>
            ))}
          </div>
        </section>

        <section>
          <p className="font-display text-sm mb-2">{UI_TEXT.knowledgeSectionTitle}</p>
          {documents.length === 0 && (
            <p className="text-xs text-ink-muted mb-3">{UI_TEXT.knowledgeListEmpty}</p>
          )}
          <ul className="text-xs text-ink-muted mb-4 space-y-1">
            {documents.map((d) => (
              <li key={`${d.documentId}-${d.version}`}>
                {d.title} — {DOCUMENT_TYPE_LABEL_TR[d.documentType] ?? d.documentType}
              </li>
            ))}
          </ul>

          <form onSubmit={handleUpload} className="border-t border-line pt-3 space-y-2">
            <p className="font-display text-xs uppercase text-ink-muted">{UI_TEXT.uploadTitle}</p>
            <input
              required
              placeholder={UI_TEXT.uploadTitleField}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full border border-line-strong rounded-md p-2 text-sm bg-paper"
            />
            <select
              value={documentType}
              onChange={(e) => setDocumentType(e.target.value)}
              className="w-full border border-line-strong rounded-md p-2 text-sm bg-paper"
            >
              {DOCUMENT_TYPES.map((t) => (
                <option key={t} value={t}>
                  {DOCUMENT_TYPE_LABEL_TR[t]}
                </option>
              ))}
            </select>
            <input
              required
              type="date"
              value={effectiveFrom}
              onChange={(e) => setEffectiveFrom(e.target.value)}
              className="w-full border border-line-strong rounded-md p-2 text-sm bg-paper"
            />
            <textarea
              required
              placeholder={UI_TEXT.uploadContentField}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              rows={4}
              className="w-full border border-line-strong rounded-md p-2 text-sm bg-paper"
            />
            <button
              type="submit"
              className="w-full bg-signal text-white font-display text-sm px-3 py-2 rounded-md hover:opacity-90"
            >
              {UI_TEXT.uploadSubmit}
            </button>
            {uploadOk && <p className="text-xs text-confirm">{UI_TEXT.uploadSuccess}</p>}
            {uploadError && <p className="text-xs text-danger">{uploadError}</p>}
          </form>
        </section>
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npm run test -- SettingsPanel.test.tsx`
Expected: PASS, both cases green.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/SettingsPanel.tsx frontend/src/components/SettingsPanel.test.tsx
git commit -m "feat(frontend): settings panel (model picker, mode toggle, knowledge base)"
```

---

## Task 9: `App.tsx` rewrite — three-pane chat layout + navbar

**Files:**
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/Header.tsx`
- Create: `frontend/src/App.test.tsx`

**Interfaces:**
- Consumes: `Sidebar` (Task 7), `SettingsPanel` (Task 8), `ChatMessage`/`ChatTurn`/`TypingIndicator` (Task 6), `ChatComposer` (Task 6), `listSessions`/`createSession`/`renameSession`/`recordQuestion`/`getRecordedQuestion` (Task 3), `createInvestigation`/`listSessionInvestigations` (Task 1), `UI_TEXT` (Task 2).
- Produces: `export default function App()` — the app root, unchanged export contract (`frontend/src/main.tsx` already does `import App from './App'`, no change needed there).

- [ ] **Step 1: Write the failing integration test**

```tsx
// frontend/src/App.test.tsx
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import App from './App'

describe('App chat flow', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string, init?: RequestInit) => {
        if (url.includes('/models')) {
          return Promise.resolve({
            ok: true,
            status: 200,
            json: async () => ({ models: ['meta/llama-3.1-8b-instruct'] }),
          })
        }
        if (url.includes('/knowledge/documents')) {
          return Promise.resolve({ ok: true, status: 200, json: async () => [] })
        }
        if (url.includes('/investigations') && init?.method === 'POST') {
          const body = JSON.parse(init.body as string)
          return Promise.resolve({
            ok: true,
            status: 200,
            json: async () => ({
              investigationId: `inv-${body.question.length}`,
              status: 'ANOMALY_CONFIRMED',
              severity: 'HIGH',
              summary: 'ANOMALY_CONFIRMED',
              timeWindow: { startAt: '2026-07-30T11:15:00Z', endAt: '2026-07-30T11:30:00Z' },
              evidence: [],
              hypotheses: [],
              recommendedActions: [],
              knowledgeReferences: [],
              confidence: 0.9,
              approvalRequired: false,
              validation: { status: 'PASSED', warnings: [] },
            }),
          })
        }
        return Promise.reject(new Error(`unexpected fetch ${url}`))
      })
    )
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('sends a question, then a follow-up, reusing the same sessionId both times', async () => {
    render(<App />)

    const textarea = await screen.findByPlaceholderText(/Ne araştırmak istersiniz/)
    fireEvent.change(textarea, { target: { value: 'OTP oranı neden düştü?' } })
    fireEvent.keyDown(textarea, { key: 'Enter', code: 'Enter' })

    await waitFor(() => expect(screen.getByText('OTP oranı neden düştü?')).toBeInTheDocument())
    await waitFor(() => expect(screen.getByText(/Anomali doğrulandı/)).toBeInTheDocument())

    fireEvent.change(textarea, { target: { value: 'Peki ya operatör B?' } })
    fireEvent.keyDown(textarea, { key: 'Enter', code: 'Enter' })

    await waitFor(() => expect(screen.getByText('Peki ya operatör B?')).toBeInTheDocument())

    const postCalls = (fetch as unknown as ReturnType<typeof vi.fn>).mock.calls.filter(
      ([url, init]: [string, RequestInit]) =>
        url.includes('/investigations') && init?.method === 'POST'
    )
    expect(postCalls).toHaveLength(2)
    const firstSessionId = JSON.parse(postCalls[0][1].body as string).sessionId
    const secondSessionId = JSON.parse(postCalls[1][1].body as string).sessionId
    expect(firstSessionId).toBeTruthy()
    expect(firstSessionId).toBe(secondSessionId)
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm run test -- App.test.tsx`
Expected: FAIL — current `App.tsx` has no composer/sidebar/session wiring.

- [ ] **Step 3: Update `Header.tsx` into the top navbar with a settings toggle**

```tsx
// frontend/src/components/Header.tsx
import { UI_TEXT } from '../lib/labels'

export function Header({ onOpenSettings }: { onOpenSettings: () => void }) {
  return (
    <header className="border-b border-line px-6 py-3 flex items-center justify-between shrink-0">
      <div>
        <span className="font-display font-semibold text-lg tracking-tight">{UI_TEXT.appName}</span>
        <span className="ml-2 text-ink-muted text-sm">{UI_TEXT.appTagline}</span>
      </div>
      <button
        type="button"
        onClick={onOpenSettings}
        className="font-mono text-xs uppercase tracking-wide text-ink-muted border border-line rounded px-3 py-1.5 hover:bg-line/30"
      >
        {UI_TEXT.settings}
      </button>
    </header>
  )
}
```

- [ ] **Step 4: Rewrite `App.tsx`**

```tsx
// frontend/src/App.tsx
import { useEffect, useRef, useState } from 'react'
import { Header } from './components/Header'
import { Sidebar } from './components/Sidebar'
import { SettingsPanel } from './components/SettingsPanel'
import { ChatMessage, type ChatTurn } from './components/ChatMessage'
import { ChatComposer } from './components/ChatComposer'
import { createInvestigation, listSessionInvestigations } from './api/client'
import {
  listSessions,
  createSession,
  renameSession,
  recordQuestion,
  getRecordedQuestion,
  type SessionMeta,
} from './lib/sessionStore'
import { toUserMessage } from './lib/errors'
import type { InvestigationRequest } from './api/types'

export default function App() {
  const [sessions, setSessions] = useState<SessionMeta[]>([])
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null)
  const [turns, setTurns] = useState<ChatTurn[]>([])
  const [modelId, setModelId] = useState<string | null>(null)
  const [mode, setMode] = useState<'quick' | 'thorough'>('thorough')
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const existing = listSessions()
    if (existing.length > 0) {
      setSessions(existing)
      selectSession(existing[0].sessionId)
    } else {
      handleNewChat()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [turns])

  function handleNewChat() {
    const session = createSession()
    setSessions(listSessions())
    setActiveSessionId(session.sessionId)
    setTurns([])
  }

  async function selectSession(sessionId: string) {
    setActiveSessionId(sessionId)
    try {
      const investigations = await listSessionInvestigations(sessionId)
      setTurns(
        investigations.map((investigation) => ({
          id: investigation.investigationId,
          question: getRecordedQuestion(sessionId, investigation.investigationId) ?? '—',
          status: 'done' as const,
          investigation,
        }))
      )
    } catch {
      setTurns([])
    }
  }

  async function handleSubmit(question: string, timeWindow?: { startAt: string; endAt: string }) {
    if (!activeSessionId) return
    const turnId = crypto.randomUUID()
    setTurns((prev) => [...prev, { id: turnId, question, status: 'pending' }])
    setBusy(true)

    if (turns.length === 0) {
      renameSession(activeSessionId, question)
      setSessions(listSessions())
    }

    const req: InvestigationRequest = {
      question,
      timeWindow,
      sessionId: activeSessionId,
      modelId: modelId ?? undefined,
      mode,
    }

    try {
      const investigation = await createInvestigation(req)
      recordQuestion(activeSessionId, investigation.investigationId, question)
      setTurns((prev) =>
        prev.map((t) =>
          t.id === turnId ? { ...t, id: investigation.investigationId, status: 'done', investigation } : t
        )
      )
    } catch (err) {
      setTurns((prev) =>
        prev.map((t) => (t.id === turnId ? { ...t, status: 'error', errorMessage: toUserMessage(err) } : t))
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="h-screen bg-paper text-ink font-body flex flex-col">
      <Header onOpenSettings={() => setSettingsOpen(true)} />
      <div className="flex flex-1 min-h-0">
        <Sidebar
          sessions={sessions}
          activeSessionId={activeSessionId}
          onSelect={selectSession}
          onNewChat={handleNewChat}
        />
        <main className="flex-1 flex flex-col min-h-0">
          <div ref={scrollRef} className="flex-1 overflow-y-auto px-6 py-6 max-w-[880px] w-full mx-auto">
            {turns.map((turn) => (
              <div key={turn.id} className="mb-6">
                <ChatMessage turn={turn} />
              </div>
            ))}
          </div>
          <div className="px-6 pb-6 max-w-[880px] w-full mx-auto shrink-0">
            <ChatComposer disabled={busy} onSubmit={handleSubmit} />
          </div>
        </main>
      </div>
      {settingsOpen && (
        <SettingsPanel
          modelId={modelId}
          onModelChange={setModelId}
          mode={mode}
          onModeChange={setMode}
          onClose={() => setSettingsOpen(false)}
        />
      )}
    </div>
  )
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npm run test -- App.test.tsx`
Expected: PASS.

- [ ] **Step 6: Run the full frontend suite and build**

Run: `npm run test && npm run build`
Expected: all suites PASS, `tsc -b && vite build` succeeds with no type errors (this will surface any stale import — e.g. `Header` now requires `onOpenSettings`, `Footer` import removed from `App.tsx` if unused — fix any such compile error directly).

- [ ] **Step 7: Commit**

```bash
git add frontend/src/App.tsx frontend/src/App.test.tsx frontend/src/components/Header.tsx
git commit -m "feat(frontend): three-pane chat console (sidebar, thread, settings)"
```

---

## Self-review notes (already applied above)

- **Spec coverage:** layout/sidebar (Task 7, 9) · chat flow + memory (Task 6, 9) · settings panel model/mode/knowledge (Task 8) · charts (Task 4) · Turkish localization (Task 2, 5, 6, 7, 8, 9) · `datetime-local` time window (Task 6) · document upload verification is a manual/browser step, not unit-testable against the real backend — see "Bitti sayılması için" in the M12 prompt, done after Task 9 in a live `docker compose` + Chrome session, not as a plan task.
- **`Footer.tsx`** (existing, unmodified component) is dropped from the new `App.tsx` layout — a footer does not fit a full-height chat console. It is not deleted (still exported, in case a later milestone wants it), just unused; confirm `npm run build` has no unused-import error since `App.tsx`'s new version does not import it at all.
- **Type/name consistency check:** `ChatTurn` (Task 6) is consumed identically in `App.tsx` (Task 9); `SessionMeta` (Task 3) matches the `Sidebar` props (Task 7); `KnowledgeDocumentSummary`/`KnowledgeDocumentUploadRequest` (Task 1) match `SettingsPanel`'s usage (Task 8); all chart components (Task 4) use the exact `Hypothesis`/`KnowledgeReference` field names from `frontend/src/api/types.ts` (unchanged by this plan).
