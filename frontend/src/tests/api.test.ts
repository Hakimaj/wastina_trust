import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiClient, configureApiAuth } from '../api'
import { ApiError, codeForStatus, normalizeApiError } from '../api/errors'

describe('API error foundation', () => {
  afterEach(() => {
    configureApiAuth(undefined)
    vi.restoreAllMocks()
  })

  it('normalizes common HTTP statuses', () => {
    expect(codeForStatus(401)).toBe('AUTHENTICATION_REQUIRED')
    expect(codeForStatus(403)).toBe('FORBIDDEN')
    expect(codeForStatus(409)).toBe('CONFLICT')
    expect(codeForStatus(503)).toBe('SERVER_ERROR')
  })

  it('preserves structured errors', () => {
    const error = new ApiError({
      message: 'Conflict',
      code: 'CONFLICT',
      status: 409,
      requestId: 'request-1',
    })
    expect(normalizeApiError(error)).toBe(error)
    expect(error.requestId).toBe('request-1')
  })

  it('renews and retries a GET once after a 401', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: 'expired' }), { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ ok: true }), { status: 200, headers: { 'content-type': 'application/json' } }))
    const refreshAccessToken = vi.fn().mockResolvedValue(true)
    configureApiAuth({ getAccessToken: () => 'memory-token', refreshAccessToken, handleUnauthorized: vi.fn() })

    await expect(apiClient.get<{ ok: boolean }>('/api/v1/me')).resolves.toEqual({ ok: true })
    expect(refreshAccessToken).toHaveBeenCalledOnce()
    expect(fetchMock).toHaveBeenCalledTimes(2)
    const firstHeaders = fetchMock.mock.calls[0][1]?.headers
    expect(firstHeaders).toBeInstanceOf(Headers)
    expect(new Headers(firstHeaders).get('Authorization')).toBe('Bearer memory-token')
  })

  it('does not retry mutation requests after a 401', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ message: 'expired' }), { status: 401 }),
    )
    const refreshAccessToken = vi.fn().mockResolvedValue(true)
    configureApiAuth({ getAccessToken: () => 'memory-token', refreshAccessToken, handleUnauthorized: vi.fn() })

    await expect(apiClient.post('/api/v1/me/provision', { email: 'person@example.test', phoneNumber: '+251' })).rejects.toMatchObject({ status: 401 })
    expect(refreshAccessToken).not.toHaveBeenCalled()
    expect(fetchMock).toHaveBeenCalledOnce()
  })
})
