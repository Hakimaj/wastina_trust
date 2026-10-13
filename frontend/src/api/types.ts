export type ApiMetadata = {
  requestId?: string
}

export type ApiResponse<T> = {
  data: T
  meta?: ApiMetadata
}

/**
 * The backend's single page envelope, shared by every paged endpoint.
 */
export type Page<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export type RequestOptions = {
  signal?: AbortSignal
  timeoutMs?: number
  headers?: HeadersInit
  /**
   * Suppresses the single automatic refresh-and-replay on a 401.
   *
   * Needed for the local JWT flow, where there is no refresh grant: the caller decides what to do.
   */
  skipAuthRetry?: boolean
}