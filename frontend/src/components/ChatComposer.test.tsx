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

  it('includes the time window when the toggle is checked', () => {
    const onSubmit = vi.fn()
    render(<ChatComposer disabled={false} onSubmit={onSubmit} />)

    fireEvent.click(screen.getByLabelText(/Zaman aralığı belirt/))
    const [startInput, endInput] = screen.getAllByDisplayValue('') as HTMLInputElement[]
    fireEvent.change(startInput, { target: { value: '2026-07-30T11:15' } })
    fireEvent.change(endInput, { target: { value: '2026-07-30T11:30' } })

    const textarea = screen.getByPlaceholderText(/Ne araştırmak istersiniz/)
    fireEvent.change(textarea, { target: { value: 'soru' } })
    fireEvent.keyDown(textarea, { key: 'Enter', code: 'Enter' })

    expect(onSubmit).toHaveBeenCalledWith('soru', {
      startAt: '2026-07-30T11:15:00Z',
      endAt: '2026-07-30T11:30:00Z',
    })
  })
})
