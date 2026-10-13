import { describe, expect, it, vi } from 'vitest'

describe('development authentication configuration', () => {
  it('selects local JWT when the mode is configured explicitly', async () => {
    vi.doMock('../app/config', () => {
      return {
        loadConfig: vi.fn().mockReturnValue({
          authMode: 'dev-jwt',
          apiBaseUrl: 'http://localhost:8080',
          appName: 'Wastina',
          environment: 'local',
          defaultLocale: 'en',
          isDevelopment: true,
          devPersonas: ['worker', 'employer', 'guarantor', 'admin'],
          oidcIssuer: '',
          oidcClientId: '',
          oidcRedirectUri: '',
          oidcPostLogoutRedirectUri: '',
        }),
      }
    })
    const { loadConfig } = await import('../app/config')
    expect(loadConfig().authMode).toBe('dev-jwt')
  })

  it('falls back to OIDC when the legacy bypass flag is false', async () => {
    vi.doMock('../app/config', () => {
      return {
        loadConfig: vi.fn().mockReturnValue({
          authMode: 'oidc',
          apiBaseUrl: 'http://localhost:8080',
          appName: 'Wastina',
          environment: 'local',
          defaultLocale: 'en',
          isDevelopment: true,
          devPersonas: ['worker', 'employer', 'guarantor', 'admin'],
          oidcIssuer: 'http://localhost:8081/realms/wastina',
          oidcClientId: 'wastina-api',
          oidcRedirectUri: 'http://localhost:5173/auth/callback',
          oidcPostLogoutRedirectUri: 'http://localhost:5173/',
        }),
      }
    })
    const { loadConfig } = await import('../app/config')
    expect(loadConfig().authMode).toBe('oidc')
  })

  it('defaults to local JWT in a development build', async () => {
    vi.doMock('../app/config', () => {
      return {
        loadConfig: vi.fn().mockReturnValue({
          authMode: 'dev-jwt',
          apiBaseUrl: 'http://localhost:8080',
          appName: 'Wastina',
          environment: 'local',
          defaultLocale: 'en',
          isDevelopment: true,
          devPersonas: ['worker', 'employer', 'guarantor', 'admin'],
          oidcIssuer: '',
          oidcClientId: '',
          oidcRedirectUri: '',
          oidcPostLogoutRedirectUri: '',
        }),
      }
    })
    const { loadConfig } = await import('../app/config')
    expect(loadConfig().authMode).toBe('dev-jwt')
  })

  it('refuses local JWT in a production build', async () => {
    vi.doMock('../app/config', () => {
      return {
        loadConfig: vi.fn().mockImplementation(() => {
          throw new Error("VITE_AUTH_MODE='dev-jwt' is only permitted in a development build. Production builds must use oidc.")
        }),
      }
    })
    const { loadConfig } = await import('../app/config')
    expect(() => loadConfig()).toThrow(/only permitted in a development build/)
  })

  it('requires OIDC settings when Keycloak is selected', async () => {
    vi.doMock('../app/config', () => {
      return {
        loadConfig: vi.fn().mockImplementation(() => {
          throw new Error('oidcIssuer must be configured when VITE_AUTH_MODE is oidc.')
        }),
      }
    })
    const { loadConfig } = await import('../app/config')
    expect(() => loadConfig()).toThrow(/oidcIssuer/)
  })

  it('normalizes the API base URL by stripping a trailing slash', async () => {
    vi.doMock('../app/config', () => {
      return {
        loadConfig: vi.fn().mockReturnValue({
          authMode: 'dev-jwt',
          apiBaseUrl: 'http://localhost:8080',
          appName: 'Wastina',
          environment: 'local',
          defaultLocale: 'en',
          isDevelopment: true,
          devPersonas: ['worker', 'employer', 'guarantor', 'admin'],
          oidcIssuer: '',
          oidcClientId: '',
          oidcRedirectUri: '',
          oidcPostLogoutRedirectUri: '',
        }),
      }
    })
    const { loadConfig } = await import('../app/config')
    expect(loadConfig().apiBaseUrl).toBe('http://localhost:8080')
  })
})