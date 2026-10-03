export type ApiMetadata = {
  requestId?: string
}

export type ApiResponse<T> = {
  data: T
  meta?: ApiMetadata
}

export type Page<T> = {
  items: T[]
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}

export type RequestOptions = {
  signal?: AbortSignal
  timeoutMs?: number
  headers?: HeadersInit
}
