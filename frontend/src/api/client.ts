import type {
  Investigation,
  InvestigationRequest,
  IncidentDraftPreview,
  IncidentDecisionRequest,
  IncidentDecisionResponse,
  ProblemDetails,
  ModelsResponse,
  ModelOption,
  KnowledgeDocumentSummary,
  KnowledgeDocumentDetail,
  KnowledgeSearchResponse,
  KnowledgeDocumentUploadRequest,
  KnowledgeDocumentUploadResponse,
  ChatMessageRequest,
  ChatMessageResponse,
  OperationsOverview,
  OperationsSampleRow,
} from './types'

const BASE = '/api/v1'

export class ApiError extends Error {
  problemDetails: ProblemDetails

  constructor(problemDetails: ProblemDetails) {
    super(problemDetails.detail || problemDetails.title)
    this.name = 'ApiError'
    this.problemDetails = problemDetails
  }
}

async function request<T>(path: string, init: RequestInit): Promise<T> {
  const response = await fetch(`${BASE}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init.headers,
    },
  })

  if (!response.ok) {
    let problemDetails: ProblemDetails
    try {
      problemDetails = (await response.json()) as ProblemDetails
    } catch {
      // Fallback when error response body isn't valid JSON (e.g., 502 gateway, 504 load balancer)
      problemDetails = {
        type: 'about:blank',
        title: response.statusText || 'Request failed',
        status: response.status,
        detail: 'The server returned an unreadable error response.',
        instance: '',
        correlationId: '',
        errorCode: 'UNKNOWN_ERROR',
      }
    }
    throw new ApiError(problemDetails)
  }

  return (await response.json()) as T
}

// The backend omits/nulls these fields when not yet populated (e.g. validation
// before a report exists). Normalize once at the API boundary so every
// downstream consumer can rely on safe defaults instead of null-checking.
export function normalizeInvestigation(raw: Investigation): Investigation {
  return {
    ...raw,
    validation: raw.validation ?? { status: 'PASSED', warnings: [] },
    evidence: raw.evidence ?? [],
    hypotheses: raw.hypotheses ?? [],
    recommendedActions: raw.recommendedActions ?? [],
    knowledgeReferences: raw.knowledgeReferences ?? [],
    visualizations: raw.visualizations ?? [],
  }
}

export async function sendChatMessage(req: ChatMessageRequest): Promise<ChatMessageResponse> {
  const raw = await request<ChatMessageResponse>('/chat/messages', {
    method: 'POST',
    body: JSON.stringify(req),
  })
  return {
    ...raw,
    suggestions: raw.suggestions ?? [],
    investigation: raw.investigation ? normalizeInvestigation(raw.investigation) : null,
  }
}

export async function createInvestigation(req: InvestigationRequest): Promise<Investigation> {
  const raw = await request<Investigation>('/investigations', {
    method: 'POST',
    body: JSON.stringify(req),
  })
  return normalizeInvestigation(raw)
}

export async function getInvestigation(id: string): Promise<Investigation> {
  const raw = await request<Investigation>(`/investigations/${id}`, { method: 'GET' })
  return normalizeInvestigation(raw)
}

export function previewIncidentDraft(investigationId: string): Promise<IncidentDraftPreview> {
  return request<IncidentDraftPreview>(
    `/investigations/${investigationId}/incident-draft/preview`,
    { method: 'POST' }
  )
}

export function submitIncidentDecision(
  investigationId: string,
  req: IncidentDecisionRequest,
  idempotencyKey: string
): Promise<IncidentDecisionResponse> {
  return request<IncidentDecisionResponse>(
    `/investigations/${investigationId}/incident-draft/decisions`,
    {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },
      body: JSON.stringify(req),
    }
  )
}

export async function listSessionInvestigations(sessionId: string): Promise<Investigation[]> {
  const raw = await request<Investigation[]>(`/sessions/${sessionId}/investigations`, {
    method: 'GET',
  })
  return raw.map(normalizeInvestigation)
}

export async function listModels(): Promise<string[]> {
  return (await getModelCatalog()).models
}

export async function getModelCatalog(): Promise<{
  models: string[]
  options: ModelOption[]
  defaultModelId: string | null
}> {
  const raw = await request<ModelsResponse>('/models', { method: 'GET' })
  const options =
    raw.options ??
    raw.models.map((id) => ({
      id,
      label: id.split('/').at(-1) ?? id,
      provider: 'NVIDIA NIM',
      profile: 'BALANCED',
      description: 'Doğrulanmış analiz modeli',
      verified: true,
    }))
  return {
    models: raw.models,
    options,
    defaultModelId: raw.defaultModelId ?? raw.models[0] ?? null,
  }
}

export function listKnowledgeDocuments(): Promise<KnowledgeDocumentSummary[]> {
  return request<KnowledgeDocumentSummary[]>('/knowledge/documents', { method: 'GET' })
}

export function getKnowledgeDocument(
  documentId: string,
  version: string
): Promise<KnowledgeDocumentDetail> {
  return request<KnowledgeDocumentDetail>(
    `/knowledge/documents/${encodeURIComponent(documentId)}/versions/${encodeURIComponent(version)}`,
    { method: 'GET' }
  )
}

export async function previewKnowledgeSearch(
  query: string,
  provider?: string,
  topK = 5
) {
  const response = await request<KnowledgeSearchResponse>('/knowledge/search-preview', {
    method: 'POST',
    body: JSON.stringify({ query, provider: provider || undefined, topK }),
  })
  return response.results
}

export function uploadKnowledgeDocument(
  req: KnowledgeDocumentUploadRequest
): Promise<KnowledgeDocumentUploadResponse> {
  return request<KnowledgeDocumentUploadResponse>('/knowledge/documents', {
    method: 'POST',
    body: JSON.stringify(req),
  })
}

function windowQuery(startAt: string, endAt: string, extra?: Record<string, string>): string {
  const params = new URLSearchParams({ startAt, endAt, ...(extra ?? {}) })
  return `?${params.toString()}`
}

export function getOperationsOverview(startAt: string, endAt: string): Promise<OperationsOverview> {
  return request<OperationsOverview>(`/operations/overview${windowQuery(startAt, endAt)}`, {
    method: 'GET',
  })
}

export function getOperationsSamples(
  startAt: string,
  endAt: string,
  provider?: string
): Promise<OperationsSampleRow[]> {
  return request<OperationsSampleRow[]>(
    `/operations/samples${windowQuery(startAt, endAt, provider ? { provider } : undefined)}`,
    { method: 'GET' }
  )
}
