export type AuthMode = 'dev-jwt' | 'oidc'

export type AppConfig = {
  /** Display name used across the shell and public pages. */
  appName: string
  apiBaseUrl: string
  environment: string
  defaultLocale: string
  /**
   * Which authentication provider to use.
   *
   * - `dev-jwt` exchanges a synthetic persona for a signed bearer token issued by the backend.
   *   Available only when the backend runs a development profile.
   * - `oidc` uses Keycloak with Authorization Code + PKCE. This is the production path and is
   *   fully preserved.
   *
   * Selection is configuration-driven so the Keycloak path is never removed, only not selected.
   */
  authMode: AuthMode
  /** True when the running application is a development build. */
  isDevelopment: boolean
  devPersonas: string[]
  oidcIssuer: string
  oidcClientId: string
  oidcRedirectUri: string
  oidcPostLogoutRedirectUri: string
}

let cachedConfig: AppConfig | undefined

/**
 * Clears the cached config. Exported for testing only.
 */
export function resetConfig(): void {
  cachedConfig = undefined
}

function readAuthMode(): AuthMode {
  const configured = import.meta.env.VITE_AUTH_MODE?.trim().toLowerCase()
  if (configured === 'dev-jwt' || configured === 'oidc') return configured
  // Fall back to the legacy flag so an existing .env.local keeps working.
  if (import.meta.env.VITE_DEV_AUTH_BYPASS === 'false') return 'oidc'
  return import.meta.env.DEV ? 'dev-jwt' : 'oidc'
}

export function loadConfig(): AppConfig {
  if (cachedConfig) return cachedConfig

  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
  const environment = import.meta.env.VITE_ENVIRONMENT ?? 'local'
  const defaultLocale = import.meta.env.VITE_DEFAULT_LOCALE ?? 'en'
  const isDevelopment = import.meta.env.DEV
  const isProduction = import.meta.env.PROD
  const testDefaults = import.meta.env.MODE === 'test'
  const authMode = readAuthMode()

  const oidcIssuer =
    import.meta.env.VITE_OIDC_ISSUER ??
    (testDefaults ? 'http://localhost:8081/realms/wastina' : '')
  const oidcClientId =
    import.meta.env.VITE_OIDC_CLIENT_ID ?? (testDefaults ? 'wastina-api' : '')
  const oidcRedirectUri =
    import.meta.env.VITE_OIDC_REDIRECT_URI ??
    (testDefaults ? `${window.location.origin}/auth/callback` : '')
  const oidcPostLogoutRedirectUri =
    import.meta.env.VITE_OIDC_POST_LOGOUT_REDIRECT_URI ??
    (testDefaults ? `${window.location.origin}/` : '')

  const devPersonas = (import.meta.env.VITE_DEV_PERSONAS ??
    'worker,employer,guarantor,admin')
    .split(',')
    .map((value) => value.trim())
    .filter(Boolean)

  if (!['en', 'am'].includes(defaultLocale)) {
    throw new Error('VITE_DEFAULT_LOCALE must be en or am.')
  }

  // The OIDC settings are only required when Keycloak is the selected provider. Local JWT mode
  // never contacts an authorization server, so a missing issuer must not break development.
  if (authMode === 'oidc') {
    for (const [name, value] of Object.entries({
      oidcIssuer,
      oidcClientId,
      oidcRedirectUri,
      oidcPostLogoutRedirectUri,
    })) {
      if (!value.trim()) {
        throw new Error(`${name} must be configured when VITE_AUTH_MODE is 'oidc'.`)
      }
    }
  }

  // Local JWT is a development convenience. It is refused in a production build so
  // that a misconfigured deployment cannot silently start issuing bearer tokens
  // without Keycloak. `PROD` is the real build signal, distinct from `DEV`, which is
  // also true under the test runner.
  if (authMode === 'dev-jwt' && isProduction) {
    throw new Error(
      "VITE_AUTH_MODE='dev-jwt' is only permitted in a development build. " +
        'Production builds must use oidc.',
    )
  }

  cachedConfig = {
    appName: import.meta.env.VITE_APP_NAME ?? 'Wastina',
    apiBaseUrl: apiBaseUrl.replace(/\/$/, ''),
    environment,
    defaultLocale,
    authMode,
    isDevelopment,
    devPersonas,
    oidcIssuer: oidcIssuer.replace(/\/$/, ''),
    oidcClientId,
    oidcRedirectUri,
    oidcPostLogoutRedirectUri,
  }
  return cachedConfig
}

export const config = loadConfig()