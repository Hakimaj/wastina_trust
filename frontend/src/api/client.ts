import { config } from '../app/config'
import { getApiAuth } from './authBridge'
import { ApiError, codeForStatus, normalizeApiError } from './errors'
import type { RequestOptions } from './types'

const DEFAULT_TIMEOUT_MS = 15_000

type RequestBody = BodyInit | Record<string, unknown> | undefined

async function parseBody(response: Response): Promise<unknown> {
  if (response.status === 204) return undefined
  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) return response.json()
  return response.text()
}

function messageFromBody(body: unknown, fallback: string): string {
  if (
    typeof body === 'object' &&
    body !== null &&
    'message' in body &&
    typeof body.message === 'string'
  ) {
    return body.message
  }
  return fallback
}

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
    const accessToken = getApiAuth()?.getAccessToken()
    if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
    if (
      body !== undefined &&
      !(body instanceof FormData) &&
      typeof body !== 'string'
    ) {
      headers.set('Content-Type', 'application/json')
    }

    const response = await fetch(`${config.apiBaseUrl}${path}`, {
      method,
      headers,
      body:
        body === undefined
          ? undefined
          : typeof body === 'string' || body instanceof FormData
            ? body
            : JSON.stringify(body),
      signal: controller.signal,
    })
    const responseBody = await parseBody(response)
    const requestId =
      response.headers.get('x-request-id') ??
      response.headers.get('x-correlation-id') ??
      undefined

    if (!response.ok) {
      if (response.status === 401 && method === 'GET' && attempt === 0) {
        const refreshed = await getApiAuth()?.refreshAccessToken()
        if (refreshed)
          return request<T>(method, path, body, options, attempt + 1)
        getApiAuth()?.handleUnauthorized()
      }
      throw new ApiError({
        message: messageFromBody(
          responseBody,
          `Request failed with status ${response.status}.`,
        ),
        code: codeForStatus(response.status),
        status: response.status,
        requestId,
        details: responseBody as Record<string, unknown> | string | undefined,
      })
    }

    return responseBody as T
  } catch (error) {
    if (error instanceof ApiError) throw error
    if (error instanceof DOMException && error.name === 'AbortError') {
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
}
