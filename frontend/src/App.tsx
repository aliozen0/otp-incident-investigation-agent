import { useState } from 'react'
import { Header } from './components/Header'
import { Footer } from './components/Footer'
import { QuestionForm } from './components/QuestionForm'
import { LoadingState } from './components/LoadingState'
import { ResultCard } from './components/ResultCard'
import { createInvestigation } from './api/client'
import { toUserMessage } from './lib/errors'
import type { Investigation, InvestigationRequest } from './api/types'

type Phase = 'idle' | 'loading' | 'result' | 'error'

export default function App() {
  const [phase, setPhase] = useState<Phase>('idle')
  const [investigation, setInvestigation] = useState<Investigation | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  async function handleSubmit(req: InvestigationRequest) {
    setPhase('loading')
    setErrorMessage(null)
    try {
      const result = await createInvestigation(req)
      setInvestigation(result)
      setPhase('result')
    } catch (err) {
      setErrorMessage(toUserMessage(err))
      setPhase('error')
    }
  }

  return (
    <div className="min-h-screen bg-paper text-ink font-body flex flex-col">
      <Header />
      <main className="max-w-[880px] w-full mx-auto px-6 py-10 flex-1">
        <QuestionForm disabled={phase === 'loading'} onSubmit={handleSubmit} />

        <div className="mt-6">
          {phase === 'loading' && <LoadingState />}
          {phase === 'error' && errorMessage && (
            <p className="text-danger text-sm">{errorMessage}</p>
          )}
          {phase === 'result' && investigation && (
            <ResultCard investigation={investigation} />
          )}
        </div>
      </main>
      <Footer />
    </div>
  )
}
