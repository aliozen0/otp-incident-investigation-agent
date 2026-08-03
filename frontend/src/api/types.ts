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
  sessionId?: string
  modelId?: string
  mode?: 'quick' | 'thorough'
}

export interface Evidence {
  id: string
  sourceType: string
  sourceReference: string
  observation: string
  observedAt: string
  metricName?: string | null
  metricValue?: number | null
  metricUnit?: string | null
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
  visualizations: VisualizationSpec[]
}

export type InteractionMode = 'AUTO' | 'CHAT' | 'INVESTIGATION'
export type InvestigationMode = 'QUICK' | 'THOROUGH'
export type ResponseType = 'CHAT' | 'CLARIFICATION' | 'INVESTIGATION'

export type VisualizationType = 'LINE' | 'BAR' | 'GROUPED_BAR' | 'GAUGE' | 'TABLE'
export type VisualizationUnit =
  | 'PERCENT' | 'RATIO' | 'COUNT' | 'MILLISECONDS' | 'CONNECTIONS' | 'NONE'

export interface VisualizationSpec {
  id: string
  type: VisualizationType
  title: string
  xAxisLabel?: string | null
  yAxisLabel?: string | null
  unit: VisualizationUnit
  series: { key: string; label: string }[]
  points: { label: string; seriesKey: string; value: number; evidenceId: string }[]
}

export interface ChatMessageRequest {
  message: string
  sessionId: string
  modelId: string
  interactionMode: InteractionMode
  investigationMode: InvestigationMode
  timeWindow?: { startAt: string; endAt: string }
  locale: string
}

export interface ChatMessageResponse {
  messageId: string
  sessionId: string
  responseType: ResponseType
  assistantMessage: string
  route: { intent: ResponseType; confidence: number; modelId: string }
  suggestions: string[]
  investigation: Investigation | null
}

export type ChatTurn =
  | { kind: 'pending'; id: string; question: string }
  | { kind: 'chat'; id: string; question: string; assistantMessage: string; suggestions?: string[] }
  | { kind: 'clarification'; id: string; question: string; assistantMessage: string; suggestions?: string[] }
  | { kind: 'investigation'; id: string; question: string; assistantMessage: string; investigation: Investigation; suggestions?: string[] }
  | { kind: 'error'; id: string; question: string; errorMessage: string }

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

export interface ModelsResponse {
  models: string[]
  options?: ModelOption[]
  defaultModelId?: string
}

export interface ModelOption {
  id: string
  label: string
  provider: string
  profile: 'FAST' | 'BALANCED' | 'DEEP' | string
  description: string
  verified: boolean
}

export interface KnowledgeDocumentSummary {
  documentId: string
  version: string
  title: string
  documentType: string
  provider?: string
  tags?: string[]
  effectiveFrom: string
  effectiveTo?: string
  language?: string
  chunkCount?: number
  embeddingModel?: string
  createdAt?: string
}

export interface KnowledgeChunk {
  chunkId: string
  sectionTitle?: string
  content: string
  tokenCount: number
  embeddingModel: string
}

export interface KnowledgeDocumentDetail extends KnowledgeDocumentSummary {
  sanitizedContent: string
  chunks: KnowledgeChunk[]
}

export interface KnowledgeSearchResult {
  documentId: string
  version: string
  title: string
  chunkId: string
  sectionTitle?: string
  similarityScore: number
  contentExcerpt: string
}

export interface KnowledgeSearchResponse {
  results: KnowledgeSearchResult[]
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

// Operational telemetry behind every investigation — the rows the agent's tools read.
export interface OperationsTotals {
  attempted: number
  delivered: number
  failed: number
  retries: number
  successRate: number
  averageDeliverySeconds: number
  p95DeliverySeconds: number
}

export interface OperationsSeriesPoint {
  bucketAt: string
  attempted: number
  delivered: number
  failed: number
  retries: number
  successRate: number
  averageDeliverySeconds: number
  p95DeliverySeconds: number
}

export interface OperationsProviderRow {
  provider: string
  attempted: number
  delivered: number
  failed: number
  successRate: number
  status: string | null
  averageResponseSeconds: number
  timeoutRate: number
  circuitBreakerState: string | null
  activeConnections: number
  maxConnections: number
}

export interface OperationsErrorRow {
  errorCode: string
  failures: number
  share: number
}

export interface OperationsQueueRow {
  bucketAt: string
  pendingMessages: number
  normalPendingThreshold: number
  oldestMessageAgeSeconds: number
  activeConsumers: number
  expectedConsumers: number
  deadLetterCount: number
  processingRateStatus: string
  status: string
}

export interface OperationsChangeRow {
  changeId: string
  occurredAt: string
  type: string
  component: string
  description: string
  version?: string | null
  approved?: boolean | null
}

export interface OperationsOverview {
  startAt: string
  endAt: string
  totals: OperationsTotals
  series: OperationsSeriesPoint[]
  providers: OperationsProviderRow[]
  errors: OperationsErrorRow[]
  queue: OperationsQueueRow | null
  changes: OperationsChangeRow[]
}

export interface OperationsSampleRow {
  bucketAt: string
  provider: string
  attempted: number
  delivered: number
  failed: number
  retries: number
  averageDeliverySeconds: number
  p95DeliverySeconds: number
  providerStatus: string | null
  timeoutRate: number
  errors: string | null
}
