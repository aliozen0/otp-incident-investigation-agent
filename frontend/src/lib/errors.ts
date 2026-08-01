import { ApiError } from '../api/client'

const MESSAGES: Record<string, string> = {
  INVALID_TIME_WINDOW: 'That time window is not valid — it must be between 1 minute and 24 hours, and cannot be in the future.',
  INVALID_REQUEST: 'The request could not be processed. Check the question and try again.',
  QUESTION_NOT_ACTIONABLE: "This question doesn't give the investigation enough to work with. Try to be more specific about the metric and timeframe.",
  INVESTIGATION_RATE_LIMITED: 'Too many investigations were started in a short time. Wait a moment and try again.',
  MODEL_PROVIDER_ERROR: 'The analysis model is currently unavailable. This is a live dependency, not a bug — try again shortly.',
  INVESTIGATION_TIMEOUT: 'The investigation took too long and timed out before finishing.',
}

export function toUserMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return MESSAGES[error.problemDetails.errorCode] ?? error.problemDetails.detail
  }
  return 'Something unexpected happened and the request failed. Check your connection and try again.'
}
