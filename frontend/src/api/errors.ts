export type ApiErrorCode =
  | 'NETWORK_ERROR'
  | 'TIMEOUT'
  | 'AUTHENTICATION_REQUIRED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'VALIDATION_ERROR'
  | 'CONFLICT'
  | 'RATE_LIMITED'
  | 'EXTERNAL_PROVIDER_ERROR'
  | 'SERVER_ERROR'
  | 'UNKNOWN_ERROR'

export type ApiErrorDetails = Record<string, unknown> | string | undefined

export class ApiError extends Error {
  readonly status: number | undefined
  readonly code: ApiErrorCode
  readonly requestId: string | undefined
  readonly details: ApiErrorDetails

  constructor(options: {
    message: string
    code: ApiErrorCode
    status?: number
    requestId?: string
    details?: ApiErrorDetails
  }) {
    super(options.message)
    this.name = 'ApiError'
    this.status = options.status
    this.code = options.code
    this.requestId = options.requestId
    this.details = options.details
  }
}

export function normalizeApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error
  if (error instanceof DOMException && error.name === 'AbortError') {
    return new ApiError({
      message: 'The request was cancelled.',
      code: 'TIMEOUT',
    })
  }
  if (error instanceof TypeError) {
    return new ApiError({
      message: 'The service could not be reached.',
      code: 'NETWORK_ERROR',
    })
  }
  return new ApiError({
    message: 'The request could not be completed.',
    code: 'UNKNOWN_ERROR',
  })
}

export function codeForStatus(status: number): ApiErrorCode {
  if (status === 401) return 'AUTHENTICATION_REQUIRED'
  if (status === 403) return 'FORBIDDEN'
  if (status === 404) return 'NOT_FOUND'
  if (status === 409) return 'CONFLICT'
  if (status === 422) return 'VALIDATION_ERROR'
  if (status === 429) return 'RATE_LIMITED'
  if (status >= 500) return 'SERVER_ERROR'
  return 'UNKNOWN_ERROR'
}
