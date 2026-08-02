import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  createInvestigation,
  ApiError,
  listSessionInvestigations,
  listModels,
  listKnowledgeDocuments,
  uploadKnowledgeDocument,
} from './client'

describe('createInvestigation', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('returns parsed investigation on 200', async () => {
    const body = {
      investigationId: 'inv-1',
      status: 'ANOMALY_CONFIRMED',
      validation: { status: 'PASSED', warnings: [] },
      evidence: [],
      hypotheses: [],
      recommendedActions: [],
      knowledgeReferences: [],
    }
    ;(fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => body,
    })

    const result = await createInvestigation({ question: 'Why did OTP delivery drop?' })

    expect(result).toEqual(body)
    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/investigations',
      expect.objectContaining({ method: 'POST' })
    )
  })

  it('normalizes null validation and missing arrays to safe defaults', async () => {
    const body = {
      investigationId: 'inv-2',
      status: 'PARTIAL_ANALYSIS',
      validation: null,
      // evidence, hypotheses, recommendedActions, knowledgeReferences omitted entirely
    }
    ;(fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => body,
    })

    const result = await createInvestigation({ question: 'Why did OTP delivery drop?' })

    expect(result.validation).toEqual({ status: 'PASSED', warnings: [] })
    expect(result.evidence).toEqual([])
    expect(result.hypotheses).toEqual([])
    expect(result.recommendedActions).toEqual([])
    expect(result.knowledgeReferences).toEqual([])
  })

  it('throws ApiError with parsed problem-details on non-2xx', async () => {
    const problem = {
      type: 'https://errors.example.local/investigation-timeout',
      title: 'Investigation timed out',
      status: 504,
      detail: 'The investigation exceeded the configured deadline.',
      instance: '/api/v1/investigations',
      correlationId: 'corr-ec3c',
      errorCode: 'INVESTIGATION_TIMEOUT',
    }
    ;(fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: false,
      status: 504,
      json: async () => problem,
    })

    await expect(createInvestigation({ question: 'x' })).rejects.toBeInstanceOf(ApiError)
    try {
      await createInvestigation({ question: 'x' })
      throw new Error('expected rejection')
    } catch (err) {
      expect((err as ApiError).problemDetails.errorCode).toBe('INVESTIGATION_TIMEOUT')
    }
  })

  it('throws ApiError with fallback problem-details when non-2xx body is not JSON', async () => {
    ;(fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: false,
      status: 502,
      statusText: 'Bad Gateway',
      json: async () => {
        throw new SyntaxError('Unexpected end of JSON input')
      },
    })

    await expect(createInvestigation({ question: 'x' })).rejects.toBeInstanceOf(ApiError)
    try {
      await createInvestigation({ question: 'x' })
      throw new Error('expected rejection')
    } catch (err) {
      expect((err as ApiError).problemDetails.errorCode).toBe('UNKNOWN_ERROR')
      expect((err as ApiError).problemDetails.status).toBe(502)
      expect((err as ApiError).problemDetails.title).toBe('Bad Gateway')
    }
  })
})

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
