import { ApiError } from '../api/client'

const MESSAGES: Record<string, string> = {
  INVALID_TIME_WINDOW: 'That time window is not valid — it must be between 1 minute and 24 hours, and cannot be in the future.',
  INVALID_REQUEST: 'The request could not be processed. Check the question and try again.',
  QUESTION_NOT_ACTIONABLE: "This question doesn't give the investigation enough to work with. Try to be more specific about the metric and timeframe.",
  INVESTIGATION_RATE_LIMITED: 'Too many investigations were started in a short time. Wait a moment and try again.',
  MODEL_PROVIDER_ERROR: 'The analysis model is currently unavailable. This is a live dependency, not a bug — try again shortly.',
  INVESTIGATION_TIMEOUT: 'The investigation took too long and timed out before finishing.',
  INVESTIGATION_NOT_ACTIONABLE: 'This investigation did not pass validation, so no incident action can be taken on it yet.',
  INVESTIGATION_NOT_FOUND: 'This investigation could not be found. It may have expired or the link may be incorrect.',
}

export function toUserMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return (
      MESSAGES[error.problemDetails.errorCode] ??
      error.problemDetails.detail ??
      'The request failed for an unknown reason. Check your connection and try again.'
    )
  }
  return 'Something unexpected happened and the request failed. Check your connection and try again.'
}
