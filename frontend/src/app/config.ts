export type AppConfig = {
  apiBaseUrl: string
  environment: string
  defaultLocale: string
  oidcIssuer: string
  oidcClientId: string
  oidcRedirectUri: string
  oidcPostLogoutRedirectUri: string
}

let cachedConfig: AppConfig | undefined

export function loadConfig(): AppConfig {
  if (cachedConfig) return cachedConfig

  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
  const environment = import.meta.env.VITE_ENVIRONMENT ?? 'development'
  const defaultLocale = import.meta.env.VITE_DEFAULT_LOCALE ?? 'en'
  const testDefaults = import.meta.env.MODE === 'test'
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

  if (!['en', 'am'].includes(defaultLocale)) {
    throw new Error('VITE_DEFAULT_LOCALE must be en or am.')
  }

  for (const [name, value] of Object.entries({
    oidcIssuer,
    oidcClientId,
    oidcRedirectUri,
    oidcPostLogoutRedirectUri,
  })) {
    if (!value.trim())
      throw new Error(`${name} must be configured for OIDC authentication.`)
  }

  cachedConfig = {
    apiBaseUrl: apiBaseUrl.replace(/\/$/, ''),
    environment,
    defaultLocale,
    oidcIssuer: oidcIssuer.replace(/\/$/, ''),
    oidcClientId,
    oidcRedirectUri,
    oidcPostLogoutRedirectUri,
  }
  return cachedConfig
}

export const config = loadConfig()
