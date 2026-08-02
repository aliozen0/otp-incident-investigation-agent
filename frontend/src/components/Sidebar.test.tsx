import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { Sidebar } from './Sidebar'

const SESSIONS = [
  { sessionId: 's-2', title: 'İkinci sohbet', createdAt: '2026-08-02T10:00:00Z' },
  { sessionId: 's-1', title: 'İlk sohbet', createdAt: '2026-08-01T10:00:00Z' },
]

describe('Sidebar', () => {
  it('renders every session title and marks the active one', () => {
    render(
      <Sidebar sessions={SESSIONS} activeSessionId="s-1" onSelect={vi.fn()} onNewChat={vi.fn()} />
    )

    expect(screen.getByText('İkinci sohbet')).toBeInTheDocument()
    expect(screen.getByText('İlk sohbet').closest('button')).toHaveAttribute(
      'aria-current',
      'true'
    )
  })

  it('calls onSelect with the clicked session id', () => {
    const onSelect = vi.fn()
    render(
      <Sidebar sessions={SESSIONS} activeSessionId="s-1" onSelect={onSelect} onNewChat={vi.fn()} />
    )

    fireEvent.click(screen.getByText('İkinci sohbet'))

    expect(onSelect).toHaveBeenCalledWith('s-2')
  })

  it('calls onNewChat when the new-chat button is clicked', () => {
    const onNewChat = vi.fn()
    render(
      <Sidebar sessions={[]} activeSessionId={null} onSelect={vi.fn()} onNewChat={onNewChat} />
    )

    fireEvent.click(screen.getByText('Yeni sohbet'))

    expect(onNewChat).toHaveBeenCalled()
  })

  it('shows the empty-list message when there are no sessions', () => {
    render(<Sidebar sessions={[]} activeSessionId={null} onSelect={vi.fn()} onNewChat={vi.fn()} />)
    expect(screen.getByText('Henüz sohbet yok')).toBeInTheDocument()
  })
})
