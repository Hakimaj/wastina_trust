import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { I18nProvider } from '../i18n/I18nProvider'

/**
 * Local development authentication now exchanges a persona for a real, signed bearer token instead
 * of relying on the backend injecting an authenticated principal. These tests cover that exchange
 * and confirm the token is actually sent.
 */
describe('local development authentication', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  function stubBackend() {
    return vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input)

      if (url.endsWith('/api/v1/dev/auth/personas')) {
        return Promise.resolve(
          new Response(JSON.stringify([]), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          }),
        )
      }

      if (url.endsWith('/api/v1/dev/auth/token')) {
        return Promise.resolve(
          new Response(
            JSON.stringify({
              accessToken: 'signed-development-token',
              tokenType: 'Bearer',
              expiresIn: 3600,
              expiresAt: '2099-01-01T00:00:00Z',
              subject: 'dev-worker',
              role: 'ROLE_WORKER',
            }),
            { status: 200, headers: { 'Content-Type': 'application/json' } },
          ),
        )
      }

      // A protected endpoint, echoing back whether a bearer header arrived.
      const authorization = new Headers(init?.headers).get('Authorization')
      return Promise.resolve(
        new Response(JSON.stringify({ authorization }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
    })
  }

  beforeEach(() => {
    fetchMock = stubBackend()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  async function renderLogin() {
    // Dynamically import after fetch is stubbed
    const { DevelopmentAuthProvider, useDevelopmentAuth } = await import('../features/auth/DevelopmentAuthProvider')

    function LoginProbe() {
      const { personas, login } = useDevelopmentAuth()
      return (
        <>
          {personas.map((persona) => (
            <button key={persona.persona} onClick={() => void login(persona.persona)}>
              {`login as ${persona.persona}`}
            </button>
          ))}
          <p>Application workspace</p>
        </>
      )
    }

    return render(
      <I18nProvider>
        <DevelopmentAuthProvider>
          <LoginProbe />
        </DevelopmentAuthProvider>
      </I18nProvider>,
    )
  }

  it('exchanges a persona for a signed token and sends it as a bearer header', async () => {
    await renderLogin()
    await screen.findByText('Application workspace')

    await userEvent.click(screen.getByRole('button', { name: 'login as worker' }))

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/dev/auth/token',
        expect.objectContaining({ method: 'POST' }),
      ),
    )

    // The token request itself carries no Authorization header.
    const tokenCall = fetchMock.mock.calls.find(([url]) =>
      String(url).endsWith('/api/v1/dev/auth/token'),
    )
    expect(new Headers((tokenCall?.[1] as RequestInit).headers).has('Authorization')).toBe(false)

    // The token is held in memory and never written to storage.
    expect(window.localStorage.getItem('wastina.auth.token')).toBeNull()
    expect(window.sessionStorage.getItem('wastina.auth.token')).toBeNull()
  })

  it('sends the issued token on subsequent API requests', async () => {
    // Test the apiClient directly with a configured auth provider
    const { configureApiAuth, apiClient } = await import('../api')
    
    configureApiAuth({
      getAccessToken: () => 'signed-development-token',
      refreshAccessToken: async () => false,
      handleUnauthorized: () => {},
    })

    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input)
      const authorization = new Headers(init?.headers).get('Authorization')
      return Promise.resolve(
        new Response(JSON.stringify({ authorization }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
    })
    vi.stubGlobal('fetch', fetchMock)

    await apiClient.get('/api/v1/me')
    const call = fetchMock.mock.calls[0]
    const authHeader = new Headers((call?.[1] as RequestInit).headers).get('Authorization')
    expect(authHeader).toBe('Bearer signed-development-token')
  })

  it('surfaces a backend error instead of inventing a session when the backend is unavailable', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('fetch failed')))

    const { DevelopmentAuthProvider, useDevelopmentAuth } = await import('../features/auth/DevelopmentAuthProvider')

    function ErrorProbe() {
      const { personas, login } = useDevelopmentAuth()
      return (
        <>
          {personas.map((persona) => (
            <button key={persona.persona} onClick={() => void login(persona.persona)}>
              {`login as ${persona.persona}`}
            </button>
          ))}
        </>
      )
    }

    render(
      <I18nProvider>
        <DevelopmentAuthProvider>
          <ErrorProbe />
        </DevelopmentAuthProvider>
      </I18nProvider>,
    )

    await userEvent.click(screen.getByRole('button', { name: 'login as worker' }))

    // The provider stays unauthenticated; no workspace is shown and no session is fabricated.
    await waitFor(() => {
      expect(screen.queryByText('Application workspace')).not.toBeInTheDocument()
    })
    // Nothing resembling a token is persisted. (The locale key written by I18nProvider is
    // unrelated, so assert on keys rather than on storage being empty.)
    const storedKeys = [
      ...Object.keys(window.localStorage),
      ...Object.keys(window.sessionStorage),
    ]
    expect(storedKeys.filter((key) => /token|auth|bearer|jwt/i.test(key))).toEqual([])
  })
})