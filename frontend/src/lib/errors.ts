import { ApiError } from '../api/client'
import { ERROR_MESSAGE_TR } from './labels'

export function toUserMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return (
      ERROR_MESSAGE_TR[error.problemDetails.errorCode] ??
      error.problemDetails.detail ??
      'İstek bilinmeyen bir nedenle başarısız oldu. Bağlantınızı kontrol edip tekrar deneyin.'
    )
  }
  return 'Beklenmedik bir şey oldu ve istek başarısız oldu. Bağlantınızı kontrol edip tekrar deneyin.'
}
