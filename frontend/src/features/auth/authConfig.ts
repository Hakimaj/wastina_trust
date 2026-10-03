import { config } from '../../app/config'

function realmFromIssuer(issuer: string): string {
  const match = issuer.match(/\/realms\/([^/]+)$/)
  if (!match) throw new Error('VITE_OIDC_ISSUER must end with /realms/{realm}.')
  return match[1]
}

export const authConfig = {
  issuer: config.oidcIssuer,
  realm: realmFromIssuer(config.oidcIssuer),
  clientId: config.oidcClientId,
  redirectUri: config.oidcRedirectUri,
  postLogoutRedirectUri: config.oidcPostLogoutRedirectUri,
  scope: 'openid profile email',
  pkceMethod: 'S256' as const,
  checkLoginIframe: false,
}

export type AuthConfig = typeof authConfig
