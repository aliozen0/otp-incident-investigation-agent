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

  it('renders an existing knowledge document', async () => {
    vi.stubGlobal('fetch', vi.fn((url: string) => {
      if (url.includes('/models')) {
        return Promise.resolve({ ok: true, status: 200, json: async () => ({ models: [] }) })
      }
      if (url.includes('/knowledge/documents')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => [
            {
              documentId: 'doc-1',
              version: '1',
              title: 'Operatör B runbook',
              documentType: 'RUNBOOK',
              effectiveFrom: '2026-01-01',
            },
          ],
        })
      }
      return Promise.reject(new Error(`unexpected fetch ${url}`))
    }))

    render(
      <SettingsPanel
        modelId={null}
        onModelChange={vi.fn()}
        mode="thorough"
        onModeChange={vi.fn()}
        onClose={vi.fn()}
      />
    )

    await waitFor(() => expect(screen.getByText(/Operatör B runbook/)).toBeInTheDocument())
  })

  it('shows a success message and refreshes the list after a successful upload', async () => {
    let documentsCallCount = 0
    const fetchMock = vi.fn((url: string, init?: RequestInit) => {
      if (url.includes('/models')) {
        return Promise.resolve({ ok: true, status: 200, json: async () => ({ models: [] }) })
      }
      if (url.includes('/knowledge/documents') && init?.method === 'POST') {
        return Promise.resolve({ ok: true, status: 201, json: async () => ({ documentId: 'doc-2', version: '1' }) })
      }
      if (url.includes('/knowledge/documents')) {
        documentsCallCount += 1
        return Promise.resolve({ ok: true, status: 200, json: async () => [] })
      }
      return Promise.reject(new Error(`unexpected fetch ${url}`))
    })
    vi.stubGlobal('fetch', fetchMock)

    render(
      <SettingsPanel
        modelId={null}
        onModelChange={vi.fn()}
        mode="thorough"
        onModeChange={vi.fn()}
        onClose={vi.fn()}
      />
    )

    await waitFor(() => expect(documentsCallCount).toBe(1))

    fireEvent.click(screen.getByText('+ Belge ekle'))
    fireEvent.change(screen.getByPlaceholderText('Başlık'), { target: { value: 'Yeni runbook' } })
    fireEvent.change(screen.getByPlaceholderText('İçerik'), { target: { value: 'İçerik' } })
    const dateInput = document.querySelector('input[type="date"]') as HTMLInputElement
    fireEvent.change(dateInput, { target: { value: '2026-08-02' } })
    fireEvent.click(screen.getByText('Yükle'))

    await waitFor(() => expect(screen.getByText('Belge yüklendi')).toBeInTheDocument())
    await waitFor(() => expect(documentsCallCount).toBe(2))
  })

  it('shows an error message when the upload fails', async () => {
    vi.stubGlobal('fetch', vi.fn((url: string, init?: RequestInit) => {
      if (url.includes('/models')) {
        return Promise.resolve({ ok: true, status: 200, json: async () => ({ models: [] }) })
      }
      if (url.includes('/knowledge/documents') && init?.method === 'POST') {
        return Promise.resolve({
          ok: false,
          status: 400,
          json: async () => ({
            type: 'about:blank',
            title: 'Invalid request',
            status: 400,
            detail: 'The request could not be processed.',
            instance: '',
            correlationId: 'corr-1',
            errorCode: 'INVALID_REQUEST',
          }),
        })
      }
      if (url.includes('/knowledge/documents')) {
        return Promise.resolve({ ok: true, status: 200, json: async () => [] })
      }
      return Promise.reject(new Error(`unexpected fetch ${url}`))
    }))

    render(
      <SettingsPanel
        modelId={null}
        onModelChange={vi.fn()}
        mode="thorough"
        onModeChange={vi.fn()}
        onClose={vi.fn()}
      />
    )

    fireEvent.click(screen.getByText('+ Belge ekle'))
    fireEvent.change(screen.getByPlaceholderText('Başlık'), { target: { value: 'Yeni runbook' } })
    fireEvent.change(screen.getByPlaceholderText('İçerik'), { target: { value: 'İçerik' } })
    const dateInput = document.querySelector('input[type="date"]') as HTMLInputElement
    fireEvent.change(dateInput, { target: { value: '2026-08-02' } })
    fireEvent.click(screen.getByText('Yükle'))

    await waitFor(() =>
      expect(screen.getByText('İstek işlenemedi. Soruyu kontrol edip tekrar deneyin.')).toBeInTheDocument()
    )
  })

  it('defaults to the first verified model once models load, if none is selected', async () => {
    const onModelChange = vi.fn()
    render(
      <SettingsPanel
        modelId={null}
        onModelChange={onModelChange}
        mode="thorough"
        onModeChange={vi.fn()}
        onClose={vi.fn()}
      />
    )

    await waitFor(() =>
      expect(onModelChange).toHaveBeenCalledWith('meta/llama-3.1-8b-instruct')
    )
  })

  it('opens sanitized document details and runs a retrieval preview', async () => {
    vi.stubGlobal('fetch', vi.fn((url: string, init?: RequestInit) => {
      if (url.includes('/models')) {
        return Promise.resolve({ ok: true, status: 200, json: async () => ({ models: [] }) })
      }
      if (url.includes('/knowledge/documents/doc-1/versions/1')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({
            documentId: 'doc-1', version: '1', title: 'Operatör B runbook',
            documentType: 'RUNBOOK', provider: 'OPERATOR_B', tags: ['otp'],
            effectiveFrom: '2026-01-01', language: 'tr', createdAt: '2026-01-01T00:00:00Z',
            sanitizedContent: 'Bağlantı havuzunu ve timeout oranını kontrol edin.',
            chunks: [{ chunkId: 'doc-1#v1#c0', sectionTitle: 'Kontrol', content: 'Bağlantı havuzu', tokenCount: 4, embeddingModel: 'hash-embedding-v1' }],
          }),
        })
      }
      if (url.includes('/knowledge/search-preview') && init?.method === 'POST') {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({ results: [{ documentId: 'doc-1', version: '1', title: 'Operatör B runbook', chunkId: 'doc-1#v1#c0', similarityScore: 0.88, contentExcerpt: 'Bağlantı havuzu' }] }),
        })
      }
      if (url.includes('/knowledge/documents')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => [{ documentId: 'doc-1', version: '1', title: 'Operatör B runbook', documentType: 'RUNBOOK', effectiveFrom: '2026-01-01', chunkCount: 1 }],
        })
      }
      return Promise.reject(new Error(`unexpected fetch ${url}`))
    }))

    render(<SettingsPanel modelId={null} onModelChange={vi.fn()} mode="thorough" onModeChange={vi.fn()} onClose={vi.fn()} />)

    fireEvent.click(await screen.findByText('Operatör B runbook'))
    expect(await screen.findByText('Bağlantı havuzunu ve timeout oranını kontrol edin.')).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('Belge detayını kapat'))

    fireEvent.change(screen.getByPlaceholderText('Örn. connection pool timeout'), { target: { value: 'connection pool' } })
    fireEvent.click(screen.getByText('Ara'))
    await waitFor(() => expect(screen.getByText('0,88')).toBeInTheDocument())
    expect(screen.getByText('doc-1#v1#c0')).toBeInTheDocument()
  })
})
