import type {
  Investigation,
  InvestigationRequest,
  IncidentDraftPreview,
  IncidentDecisionRequest,
  IncidentDecisionResponse,
  ProblemDetails,
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

export function createInvestigation(req: InvestigationRequest): Promise<Investigation> {
  return request<Investigation>('/investigations', {
    method: 'POST',
    body: JSON.stringify(req),
  })
}

export function getInvestigation(id: string): Promise<Investigation> {
  return request<Investigation>(`/investigations/${id}`, { method: 'GET' })
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
