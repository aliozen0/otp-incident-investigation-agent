import type { Investigation } from '../api/types'
import { STATUS_LABEL_TR, SEVERITY_LABEL_TR } from './labels'

export function synthesizeSummary(investigation: Investigation): string {
  const parts = [STATUS_LABEL_TR[investigation.status]]

  if (investigation.severity) {
    parts.push(SEVERITY_LABEL_TR[investigation.severity])
  }

  const top = investigation.hypotheses.find((h) => h.rank === 1) ?? investigation.hypotheses[0]
  if (top) {
    parts.push(`en olası neden: ${top.possibleCause}`)
  }

  return parts.join(' — ')
}
