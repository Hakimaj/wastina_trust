/// <reference types="vite/client" />

/**
 * Typed access to Vite environment variables.
 *
 * Vite exposes only `VITE_`-prefixed variables, so the interface intentionally has no index
 * signature: a typo in a variable name is then a compile error rather than an `undefined` at
 * runtime.
 */
interface ImportMetaEnv {
  readonly VITE_APP_NAME?: string
  readonly VITE_API_BASE_URL?: string
  readonly VITE_ENVIRONMENT?: string
  readonly VITE_DEFAULT_LOCALE?: string

  /** Selects the auth provider: `dev-jwt` (local) or `oidc` (Keycloak). */
  readonly VITE_AUTH_MODE?: string

  /** Personas offered by the local development login screen. */
  readonly VITE_DEV_PERSONAS?: string

  /** @deprecated superseded by `VITE_AUTH_MODE`. Retained so existing `.env` files still work. */
  readonly VITE_DEV_AUTH_BYPASS?: string

  readonly VITE_OIDC_ISSUER?: string
  readonly VITE_OIDC_CLIENT_ID?: string
  readonly VITE_OIDC_REDIRECT_URI?: string
  readonly VITE_OIDC_POST_LOGOUT_REDIRECT_URI?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}