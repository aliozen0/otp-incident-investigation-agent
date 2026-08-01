export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export type InvestigationStatus =
  | 'ANOMALY_CONFIRMED'
  | 'NO_ANOMALY'
  | 'PARTIAL_ANALYSIS'
  | 'FAILED'

export interface TimeWindow {
  startAt: string
  endAt: string
  timezone?: string
}

export interface InvestigationRequest {
  question: string
  timeWindow?: {
    startAt: string
    endAt: string
  }
  locale?: string
}

export interface Evidence {
  id: string
  sourceType: string
  sourceReference: string
  observation: string
  observedAt: string
}

export interface Hypothesis {
  rank: number
  possibleCause: string
  probability: 'LOW' | 'MEDIUM' | 'HIGH'
  supportingEvidenceIds: string[]
  verificationSteps: string[]
}

export interface RecommendedAction {
  actionType: string
  description: string
  risk: 'LOW' | 'MEDIUM' | 'HIGH'
  requiresApproval: boolean
}

export interface KnowledgeReference {
  documentId: string
  version?: string
  chunkId?: string
  title?: string
  similarityScore?: number
}

export interface Validation {
  status: 'PASSED' | 'FAILED'
  warnings: string[]
}

export interface Investigation {
  investigationId: string
  status: InvestigationStatus
  severity: Severity | null
  summary: string
  timeWindow: TimeWindow
  evidence: Evidence[]
  hypotheses: Hypothesis[]
  recommendedActions: RecommendedAction[]
  knowledgeReferences: KnowledgeReference[]
  confidence: number | null
  approvalRequired: boolean
  validation: Validation
}

export interface IncidentDraftPreview {
  title: string
  severity: Severity
  summary: string
  evidenceCount: number
  recommendedChecks: string[]
  requiresExplicitApproval: boolean
}

export interface IncidentDecisionRequest {
  decision: 'APPROVE' | 'REJECT'
  reason: string
}

export interface IncidentDecisionResponse {
  incidentDraftId: string
  externalIncidentId: string | null
  status: string
  idempotentReplay: boolean
}

export interface ProblemDetails {
  type: string
  title: string
  status: number
  detail: string
  instance: string
  correlationId: string
  errorCode: string
}
