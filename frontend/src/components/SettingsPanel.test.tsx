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
})
