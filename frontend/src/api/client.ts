import { config } from '../app/config'
import { getApiAuth } from './authBridge'
import {
  ApiError,
  codeForStatus,
  normalizeApiError,
  parseErrorEnvelope,
} from './errors'
import type { RequestOptions } from './types'

const DEFAULT_TIMEOUT_MS = 20_000
const REQUEST_ID_HEADER = 'X-Request-Id'

type RequestBody = BodyInit | Record<string, unknown> | FormData | undefined

function newRequestId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID()
  return `req-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

async function parseBody(response: Response): Promise<unknown> {
  if (response.status === 204) return undefined
  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) {
    const text = await response.text()
    if (!text) return undefined
    try {
      return JSON.parse(text) as unknown
    } catch {
      return text
    }
  }
  const text = await response.text()
  return text.length ? text : undefined
}

function messageFromBody(body: unknown, fallback: string): string {
  if (typeof body === 'string' && body.trim()) return body
  if (typeof body === 'object' && body !== null && 'message' in body) {
    const { message } = body as { message?: unknown }
    if (typeof message === 'string' && message.trim()) return message
  }
  return fallback
}

/** True when a request may be safely replayed. Only GETs are idempotent here. */
function isRetryableMethod(method: string): boolean {
  return method.toUpperCase() === 'GET'
}

/**
 * Single request function for the whole API.
 *
 * Behaviour:
 * - Attaches the bearer token from the configured auth bridge.
 * - Generates (and echoes) an `X-Request-Id` so a failure can be correlated with backend logs.
 * - On a `401` for an idempotent request, asks the active auth bridge to refresh once, then
 *   replays exactly once. Never replays a non-idempotent request, so a retried POST cannot create a
 *   duplicate record.
 * - Normalizes every failure into `ApiError`, preferring the backend's stable error code.
 */
export async function request<T>(
  method: string,
  path: string,
  body?: RequestBody,
  options: RequestOptions = {},
  attempt = 0,
): Promise<T> {
  const controller = new AbortController()
  const timeout = setTimeout(
    () => controller.abort(),
    options.timeoutMs ?? DEFAULT_TIMEOUT_MS,
  )
  const signal = options.signal

  if (signal) {
    if (signal.aborted) controller.abort()
    signal.addEventListener('abort', () => controller.abort(), { once: true })
  }

  try {
    const headers = new Headers(options.headers)
    headers.set('Accept', 'application/json')
    // Generated unless the caller supplied one, so every response is traceable.
    if (!headers.has(REQUEST_ID_HEADER)) headers.set(REQUEST_ID_HEADER, newRequestId())

    const accessToken = getApiAuth()?.getAccessToken()
    if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)

    // FormData must keep the browser-generated multipart boundary, so no Content-Type is set.
    const isFormData = typeof FormData !== 'undefined' && body instanceof FormData
    if (body !== undefined && !isFormData && typeof body !== 'string') {
      headers.set('Content-Type', 'application/json')
    }

    const response = await fetch(`${config.apiBaseUrl}${path}`, {
      method,
      headers,
      body:
        body === undefined
          ? undefined
          : typeof body === 'string' || isFormData
            ? (body as BodyInit)
            : JSON.stringify(body),
      signal: controller.signal,
    })

    const responseBody = await parseBody(response)
    const requestId =
      response.headers.get(REQUEST_ID_HEADER) ??
      response.headers.get('X-Correlation-Id') ??
      headers.get(REQUEST_ID_HEADER) ??
      undefined

    if (!response.ok) {
      // Exactly one refresh-and-replay, and only for idempotent methods.
      if (
        response.status === 401 &&
        isRetryableMethod(method) &&
        attempt === 0 &&
        !options.skipAuthRetry
      ) {
        const refreshed = await getApiAuth()?.refreshAccessToken()
        if (refreshed) {
          return request<T>(method, path, body, options, attempt + 1)
        }
        getApiAuth()?.handleUnauthorized()
      }

      const envelope = parseErrorEnvelope(responseBody)
      throw new ApiError({
        message: messageFromBody(
          responseBody,
          `Request failed with status ${response.status}.`,
        ),
        code: codeForStatus(response.status),
        status: response.status,
        serverCode: envelope?.code,
        requestId,
        details: responseBody as ApiError['details'],
        fieldErrors: envelope?.fieldErrors,
      })
    }

    return responseBody as T
  } catch (error) {
    if (error instanceof ApiError) throw error
    if (error instanceof DOMException && error.name === 'AbortError') {
      // Distinguish a caller-driven abort from our own timeout so cancellation is not reported
      // as a failure.
      if (signal?.aborted) {
        throw new ApiError({ message: 'The request was cancelled.', code: 'TIMEOUT' })
      }
      throw new ApiError({
        message: 'The request timed out or was cancelled.',
        code: 'TIMEOUT',
      })
    }
    throw normalizeApiError(error)
  } finally {
    clearTimeout(timeout)
  }
}

export const apiClient = {
  get: <T>(path: string, options?: RequestOptions) =>
    request<T>('GET', path, undefined, options),
  post: <T>(path: string, body?: RequestBody, options?: RequestOptions) =>
    request<T>('POST', path, body, options),
  put: <T>(path: string, body?: RequestBody, options?: RequestOptions) =>
    request<T>('PUT', path, body, options),
  patch: <T>(path: string, body?: RequestBody, options?: RequestOptions) =>
    request<T>('PATCH', path, body, options),
  delete: <T>(path: string, options?: RequestOptions) =>
    request<T>('DELETE', path, undefined, options),
  /**
   * Uploads a file as `multipart/form-data`.
   *
   * `FormData` is passed straight to `fetch` so the browser sets the boundary; setting
   * `Content-Type` manually here would corrupt the request.
   */
  upload: <T>(path: string, form: FormData, options?: RequestOptions) =>
    request<T>('POST', path, form, options),
}