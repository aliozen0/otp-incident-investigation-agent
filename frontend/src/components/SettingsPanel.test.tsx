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
