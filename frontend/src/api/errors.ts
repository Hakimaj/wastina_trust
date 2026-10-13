/**
 * API error model.
 *
 * The backend returns one envelope for every failure:
 *
 * ```json
 * { "status": 409, "code": "CONFLICT", "message": "...", "path": "/api/...",
 *   "requestId": "…", "timestamp": "…", "fieldErrors": [{ "field": "…", "message": "…" }] }
 * ```
 *
 * `code` is stable and machine-readable, so it is preferred over the HTTP status when available.
 * The status is used as the fallback so an unexpected or empty error body still classifies
 * correctly.
 */

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
  | 'PAYLOAD_TOO_LARGE'
  | 'UNSUPPORTED_MEDIA_TYPE'
  | 'SERVER_ERROR'
  | 'UNKNOWN_ERROR'

/** Stable codes emitted by the backend, surfaced so callers can branch precisely. */
export type ApiServerCode = string

export type ApiFieldError = { field: string; message: string }

export type ApiErrorDetails =
  | { status?: number; code?: string; fieldErrors?: ApiFieldError[]; [key: string]: unknown }
  | string
  | undefined

export class ApiError extends Error {
  readonly status: number | undefined
  /** Coarse client-side classification, derived from the HTTP status. */
  readonly code: ApiErrorCode
  /** The backend's own machine-readable code, when the response carried one. */
  readonly serverCode: ApiServerCode | undefined
  readonly requestId: string | undefined
  readonly details: ApiErrorDetails
  readonly fieldErrors: ApiFieldError[]

  constructor(options: {
    message: string
    code: ApiErrorCode
    status?: number
    serverCode?: string
    requestId?: string
    details?: ApiErrorDetails
    fieldErrors?: ApiFieldError[]
  }) {
    super(options.message)
    this.name = 'ApiError'
    this.status = options.status
    this.code = options.code
    this.serverCode = options.serverCode
    this.requestId = options.requestId
    this.details = options.details
    this.fieldErrors = options.fieldErrors ?? []
  }

  /** True when the failure is a form-level validation problem that can be shown per field. */
  get isValidation(): boolean {
    return this.code === 'VALIDATION_ERROR' && this.fieldErrors.length > 0
  }
}

export function normalizeApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error
  if (error instanceof DOMException && error.name === 'AbortError') {
    return new ApiError({ message: 'The request was cancelled.', code: 'TIMEOUT' })
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
  if (status === 400 || status === 422) return 'VALIDATION_ERROR'
  if (status === 401) return 'AUTHENTICATION_REQUIRED'
  if (status === 403) return 'FORBIDDEN'
  if (status === 404) return 'NOT_FOUND'
  if (status === 409) return 'CONFLICT'
  if (status === 413) return 'PAYLOAD_TOO_LARGE'
  if (status === 415) return 'UNSUPPORTED_MEDIA_TYPE'
  if (status === 429) return 'RATE_LIMITED'
  if (status >= 500) return 'SERVER_ERROR'
  return 'UNKNOWN_ERROR'
}

/**
 * Narrows an unknown response body to the backend error envelope.
 *
 * Returns `undefined` for bodies that are not in that shape (an HTML error page, a plain-text
 * response, an empty body), so the caller can fall back to the HTTP status.
 */
export function parseErrorEnvelope(
  body: unknown,
): { code?: string; message?: string; fieldErrors?: ApiFieldError[] } | undefined {
  if (typeof body !== 'object' || body === null) return undefined
  const candidate = body as Record<string, unknown>
  const hasEnvelope =
    typeof candidate.code === 'string' || typeof candidate.status === 'number'
  if (!hasEnvelope) return undefined

  const fieldErrors = Array.isArray(candidate.fieldErrors)
    ? candidate.fieldErrors.flatMap((entry) => {
        if (typeof entry !== 'object' || entry === null) return []
        const { field, message } = entry as Record<string, unknown>
        return typeof field === 'string' && typeof message === 'string'
          ? [{ field, message }]
          : []
      })
    : []

  return {
    code: typeof candidate.code === 'string' ? candidate.code : undefined,
    message: typeof candidate.message === 'string' ? candidate.message : undefined,
    fieldErrors,
  }
}

/** Human-readable message for an error, preferring the server's own wording. */
export function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message || fallback
  if (error instanceof Error && error.message) return error.message
  return fallback
}