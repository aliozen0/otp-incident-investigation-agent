import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ChatMessage } from './ChatMessage'

describe('ChatMessage response density', () => {
  it('renders chat as a simple assistant bubble without investigation panels', () => {
    render(
      <ChatMessage
        turn={{
          kind: 'chat', id: 'm-1', question: 'Merhaba', assistantMessage: 'Merhaba!',
          suggestions: ['Neleri inceleyebilirsin?'],
        }}
      />
    )

    expect(screen.getByText('Merhaba!')).toBeInTheDocument()
    expect(screen.queryByText('Kanıtlar')).not.toBeInTheDocument()
    expect(screen.queryByText('Hipotezler')).not.toBeInTheDocument()
    expect(screen.queryByText(/incident/i)).not.toBeInTheDocument()
  })

  it('renders clarification question without analysis panels', () => {
    render(
      <ChatMessage
        turn={{ kind: 'clarification', id: 'm-2', question: 'Operatör B nasıl?', assistantMessage: 'Hangi zaman aralığı?' }}
      />
    )
    expect(screen.getByText('Hangi zaman aralığı?')).toBeInTheDocument()
    expect(screen.queryByText('Kanıtlar')).not.toBeInTheDocument()
  })
})
