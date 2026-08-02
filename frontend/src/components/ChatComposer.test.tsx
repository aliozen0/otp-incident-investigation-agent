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

  it('guides a short message without submitting an investigation request', () => {
    const onSubmit = vi.fn()
    render(<ChatComposer disabled={false} onSubmit={onSubmit} />)

    const textarea = screen.getByPlaceholderText(/Ne araştırmak istersiniz/)
    fireEvent.change(textarea, { target: { value: 'Merhaba' } })
    fireEvent.keyDown(textarea, { key: 'Enter', code: 'Enter' })

    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent(/daha açıklayıcı bir inceleme sorusu/i)
    expect((textarea as HTMLTextAreaElement).value).toBe('Merhaba')
  })

  it('includes the time window when the toggle is checked', () => {
    const onSubmit = vi.fn()
    render(<ChatComposer disabled={false} onSubmit={onSubmit} />)

    fireEvent.click(screen.getByLabelText(/Zaman aralığı belirt/))
    const [startInput, endInput] = screen.getAllByDisplayValue('') as HTMLInputElement[]
    fireEvent.change(startInput, { target: { value: '2026-07-30T11:15' } })
    fireEvent.change(endInput, { target: { value: '2026-07-30T11:30' } })

    const textarea = screen.getByPlaceholderText(/Ne araştırmak istersiniz/)
    fireEvent.change(textarea, { target: { value: 'OTP oranını incele' } })
    fireEvent.keyDown(textarea, { key: 'Enter', code: 'Enter' })

    expect(onSubmit).toHaveBeenCalledWith('OTP oranını incele', {
      startAt: new Date('2026-07-30T11:15').toISOString(),
      endAt: new Date('2026-07-30T11:30').toISOString(),
    })
  })

  it('requires both manual time fields before submitting', () => {
    const onSubmit = vi.fn()
    render(<ChatComposer disabled={false} onSubmit={onSubmit} />)

    fireEvent.click(screen.getByLabelText(/Zaman aralığı belirt/))
    const [startInput] = screen.getAllByDisplayValue('') as HTMLInputElement[]
    fireEvent.change(startInput, { target: { value: '2026-07-30T11:15' } })
    const textarea = screen.getByPlaceholderText(/Ne araştırmak istersiniz/)
    fireEvent.change(textarea, { target: { value: 'OTP oranını incele' } })
    fireEvent.keyDown(textarea, { key: 'Enter', code: 'Enter' })

    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent(/Başlangıç ve bitiş zamanını birlikte/)
  })

  it('keeps verified model selection next to the composer', () => {
    const onModelChange = vi.fn()
    render(
      <ChatComposer
        disabled={false}
        onSubmit={vi.fn()}
        models={[
          {
            id: 'meta/llama-3.1-8b-instruct',
            label: 'Llama 3.1 8B',
            provider: 'Meta / NVIDIA NIM',
            profile: 'FAST',
            description: 'Hızlı',
            verified: true,
          },
          {
            id: 'meta/llama-3.3-70b-instruct',
            label: 'Llama 3.3 70B',
            provider: 'Meta / NVIDIA NIM',
            profile: 'BALANCED',
            description: 'Dengeli',
            verified: true,
          },
        ]}
        modelId="meta/llama-3.1-8b-instruct"
        onModelChange={onModelChange}
      />
    )

    fireEvent.change(screen.getByLabelText('Analiz modeli'), {
      target: { value: 'meta/llama-3.3-70b-instruct' },
    })

    expect(onModelChange).toHaveBeenCalledWith('meta/llama-3.3-70b-instruct')
  })
})
