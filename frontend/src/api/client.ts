import type {
  Investigation,
  InvestigationRequest,
  IncidentDraftPreview,
  IncidentDecisionRequest,
  IncidentDecisionResponse,
  ProblemDetails,
  ModelsResponse,
  KnowledgeDocumentSummary,
  KnowledgeDocumentUploadRequest,
  KnowledgeDocumentUploadResponse,
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
