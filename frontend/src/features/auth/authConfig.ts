import { config } from '../../app/config'

function realmFromIssuer(issuer: string): string {
  const match = issuer.match(/\/realms\/([^/]+)$/)
  if (!match) throw new Error('VITE_OIDC_ISSUER must end with /realms/{realm}.')
  return match[1]
}

function createAuthConfig() {
  if (config.authMode !== 'oidc') {
    return null
  }
  return {
    issuer: config.oidcIssuer,
    realm: realmFromIssuer(config.oidcIssuer),
    clientId: config.oidcClientId,
    redirectUri: config.oidcRedirectUri,
    postLogoutRedirectUri: config.oidcPostLogoutRedirectUri,
    scope: 'openid profile email',
    pkceMethod: 'S256' as const,
    checkLoginIframe: false,
  }
}

/**
 * Lazily evaluated OIDC configuration.
 *
 * In dev-JWT mode this returns `null` immediately without validating OIDC env vars.
 * In OIDC mode it computes and caches the config on first access.
 */
let cachedAuthConfig: ReturnType<typeof createAuthConfig> | undefined

export function getAuthConfig() {
  if (cachedAuthConfig === undefined) {
    cachedAuthConfig = createAuthConfig()
  }
  return cachedAuthConfig
}

export type AuthConfig = NonNullable<ReturnType<typeof createAuthConfig>>