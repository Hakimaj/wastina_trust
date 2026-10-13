import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { apiClient, ApiError, configureApiAuth } from '../../api'
import { config } from '../../app/config'
import { broadcastLogout, subscribeToAuthEvents } from './authEvents'
import { AuthContext, type AuthContextValue } from './AuthContext'
import type {
  AuthSession,
  OidcIdentity,
  OrganizationMembership,
  UserRole,
} from './authTypes'
import { saveReturnPath } from './redirects'
import {
  classifyBootstrapError,
  getCurrentAccount,
  getCurrentOrganizations,
  normalizeAccount,
  provisionCurrentAccount,
  statusForAccount,
} from './sessionBootstrap'

const TOKEN_PATH = '/api/v1/dev/auth/token'
const PERSONA_LIST_PATH = '/api/v1/dev/auth/personas'

export type DevelopmentPersona = {
  persona: string
  subject: string
  email: string
  role: string
}

type TokenResponse = {
  accessToken: string
  tokenType: string
  expiresIn: number
  expiresAt: string
  subject: string
  role: string
}

/** Used until the backend responds, and if it cannot be reached. */
const FALLBACK_PERSONAS: DevelopmentPersona[] = [
  { persona: 'worker', subject: 'dev-worker', email: 'worker@wastina.local', role: 'ROLE_WORKER' },
  { persona: 'employer', subject: 'dev-employer', email: 'employer@wastina.local', role: 'ROLE_EMPLOYER' },
  { persona: 'guarantor', subject: 'dev-guarantor', email: 'guarantor@wastina.local', role: 'ROLE_GUARANTOR' },
  { persona: 'admin', subject: 'dev-admin', email: 'admin@wastina.local', role: 'ROLE_ADMIN' },
]

const EMPTY_SESSION: AuthSession = {
  status: 'unauthenticated',
  identity: null,
  account: null,
  roles: [],
  organizations: [],
  activeRole: null,
  activeOrganization: null,
  scopes: [],
  error: null,
}

export type DevelopmentAuthBridgeValue = {
  personas: DevelopmentPersona[]
  persona: string | null
  expiresAt: string | null
  login: (persona: string, returnPath?: string) => Promise<void>
  pendingPersona: string | null
}

const DevelopmentAuthBridge = createContext<DevelopmentAuthBridgeValue | undefined>(undefined)

export function useDevelopmentAuth(): DevelopmentAuthBridgeValue {
  const value = useContext(DevelopmentAuthBridge)
  if (!value) throw new Error('useDevelopmentAuth must be used inside DevelopmentAuthProvider.')
  return value
}

/**
 * Reads a JWT payload without verifying it.
 *
 * The token was issued by our own backend over a trusted channel and is never trusted for
 * authorization here — the backend remains authoritative. This only extracts display fields so the
 * shell can name the signed-in persona before `/api/v1/me` responds.
 */
function readTokenClaims(token: string): Record<string, unknown> {
  const segments = token.split('.')
  if (segments.length < 2) return {}
  try {
    const normalized = segments[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
    const json = atob(padded)
    return JSON.parse(decodeURIComponent(escape(json))) as Record<string, unknown>
  } catch {
    return {}
  }
}

function identityFromToken(token: string, persona: string): OidcIdentity | null {
  const claims = readTokenClaims(token)
  if (typeof claims.sub !== 'string') {
    return {
      subject: persona,
      preferredUsername: persona,
      claims: { developmentOnly: true, persona },
    }
  }
  return {
    subject: claims.sub,
    email: typeof claims.email === 'string' ? claims.email : undefined,
    name: typeof claims.name === 'string' ? claims.name : undefined,
    preferredUsername:
      typeof claims.preferred_username === 'string' ? claims.preferred_username : claims.sub,
    // Marks the session as development-only so the shell can show a banner and label logout
    // accordingly. It carries no authority.
    claims: { ...claims, developmentOnly: true, persona },
  }
}

/**
 * Local development authentication backed by the backend's signed-JWT endpoint.
 *
 * Unlike the previous request filter that injected an authenticated principal, this exchanges a
 * persona for a real signed bearer token which is then verified by the backend on every request. The
 * token is held in component state only: it is lost on reload and never written to storage.
 *
 * The backend only exposes these endpoints under a development profile, so this cannot silently
 * authenticate against a deployment.
 */
export function DevelopmentAuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [expiresAt, setExpiresAt] = useState<string | null>(null)
  const [persona, setPersona] = useState<string | null>(null)
  const [pendingPersona, setPendingPersona] = useState<string | null>(null)
  const [session, setSession] = useState<AuthSession>(EMPTY_SESSION)
  const [personas, setPersonas] = useState<DevelopmentPersona[]>(FALLBACK_PERSONAS)

  // The backend owns the persona list; fetching it keeps the picker from drifting.
  useEffect(() => {
    let cancelled = false
    apiClient
      .get<DevelopmentPersona[]>(PERSONA_LIST_PATH, { skipAuthRetry: true })
      .then((remote) => {
        if (!cancelled && Array.isArray(remote) && remote.length > 0) setPersonas(remote)
      })
      .catch(() => {
        // Keep the fallback list. Login will surface the real connectivity error.
      })
    return () => {
      cancelled = true
    }
  }, [])

  const clearSession = useCallback(
    (status: AuthSession['status'], error: string | null = null) => {
      setSession({ ...EMPTY_SESSION, status, error })
      setAccessToken(null)
      setExpiresAt(null)
      setPersona(null)
    },
    [],
  )

  const mintToken = useCallback(async (nextPersona: string): Promise<TokenResponse> => {
    const response = await fetch(`${config.apiBaseUrl}${TOKEN_PATH}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ persona: nextPersona }),
    })
    if (!response.ok) {
      throw new Error(
        `The local development token endpoint returned ${response.status}. ` +
          'Is the backend running with the local or development profile?',
      )
    }
    return (await response.json()) as TokenResponse
  }, [])

  /**
   * Populates the session from `/api/v1/me` and `/api/v1/me/organizations`.
   *
   * Roles always come from the persisted account, never from the token, so what the UI shows is
   * what the backend will authorize.
   */
  const bootstrap = useCallback(
    async (token: string, nextPersona: string) => {
      const identity = identityFromToken(token, nextPersona)
      setSession((current) => ({ ...current, status: 'initializing', identity, error: null }))

      try {
        const [account, organizations] = await Promise.all([
          getCurrentAccount(),
          getCurrentOrganizations(),
        ])
        setSession({
          status: statusForAccount(account.accountStatus),
          identity,
          account,
          roles: account.roles,
          organizations,
          activeRole: null,
          activeOrganization:
            organizations.find((membership) => membership.status === 'ACTIVE') ?? null,
          scopes: [],
          error:
            account.accountStatus === 'ACTIVE'
              ? null
              : `Account status: ${account.accountStatus}.`,
        })
      } catch (error) {
        const kind = classifyBootstrapError(error)
        if (kind === 'unauthorized') {
          clearSession('session-expired', 'Your session has expired. Sign in again.')
          return
        }
        if (kind === 'forbidden') {
          setSession({
            ...EMPTY_SESSION,
            status: 'forbidden',
            identity,
            error: 'Your account cannot access Wastina.',
          })
          return
        }
        if (kind === 'not-found') {
          setSession({
            ...EMPTY_SESSION,
            status: 'provisioning',
            identity,
            error: 'Your Wastina account still needs to be created.',
          })
          return
        }
        setSession({
          ...EMPTY_SESSION,
          status: 'bootstrap-error',
          identity,
          error: error instanceof Error ? error.message : 'Wastina bootstrap failed.',
        })
      }
    },
    [clearSession],
  )

  /**
   * Wires the token into the API client.
   *
   * There is no refresh grant in local JWT mode, so a 401 re-mints a token for the current
   * persona; if that fails the session ends. `skipAuthRetry` is set on the token request itself so
   * a failed mint does not recurse.
   */
  useEffect(() => {
    configureApiAuth({
      getAccessToken: () => accessToken ?? undefined,
      refreshAccessToken: async () => {
        if (!accessToken || !persona) return false
        try {
          const reissued = await mintToken(persona)
          setAccessToken(reissued.accessToken)
          setExpiresAt(reissued.expiresAt)
          return true
        } catch {
          return false
        }
      },
      handleUnauthorized: () => {
        broadcastLogout()
        clearSession('session-expired', 'Your session has expired. Sign in again.')
      },
    })
  }, [accessToken, persona, mintToken, clearSession])

  // Signing out in another tab ends the session here too.
  useEffect(() => {
    return subscribeToAuthEvents(() => {
      clearSession('session-expired', 'You have been signed out in another tab.')
    })
  }, [clearSession])

  const login = useCallback(
    async (nextPersona: string, returnPath?: string) => {
      setPendingPersona(nextPersona)
      setSession((current) => ({ ...current, status: 'initializing', error: null }))
      try {
        const issued = await mintToken(nextPersona)
        setAccessToken(issued.accessToken)
        setExpiresAt(issued.expiresAt)
        setPersona(nextPersona)
        if (returnPath) saveReturnPath(returnPath)
        await bootstrap(issued.accessToken, nextPersona)
      } catch (error) {
        clearSession(
          'unauthenticated',
          error instanceof ApiError || error instanceof Error
            ? error.message
            : 'The local development login failed.',
        )
      } finally {
        setPendingPersona(null)
      }
    },
    [mintToken, bootstrap, clearSession],
  )

  const logout = useCallback(async () => {
    broadcastLogout()
    clearSession('unauthenticated')
  }, [clearSession])

  const contextValue = useMemo<AuthContextValue>(
    () => ({
      ...session,
      // The login screen calls `useDevelopmentAuth().login(persona)` instead, because the persona
      // is the credential in this mode.
      login: async () => undefined,
      register: async () => {
        throw new Error(
          'Registration is handled by the identity provider. Start one of the development personas instead.',
        )
      },
      logout,
      refreshSession: async () => {
        if (!accessToken || !persona) {
          clearSession('unauthenticated')
          return
        }
        await bootstrap(accessToken, persona)
      },
      provisionAccount: async (email: string, phoneNumber: string) => {
        if (!accessToken) {
          clearSession('unauthenticated')
          throw new Error('Sign in as a development persona first.')
        }
        setSession((current) => ({ ...current, status: 'provisioning', error: null }))
        try {
          const account = await provisionCurrentAccount({ email, phoneNumber })
          const organizations = await getCurrentOrganizations()
          setSession({
            status: statusForAccount(account.accountStatus),
            identity: session.identity,
            account,
            roles: account.roles,
            organizations,
            activeRole: null,
            activeOrganization:
              organizations.find((membership) => membership.status === 'ACTIVE') ?? null,
            scopes: [],
            error: null,
          })
        } catch (error) {
          setSession((current) => ({
            ...current,
            status: 'bootstrap-error',
            error: error instanceof Error ? error.message : 'Account provisioning failed.',
          }))
          throw error
        }
      },
      setActiveRole: (role: UserRole) => {
        setSession((current) =>
          current.roles.includes(role) ? { ...current, activeRole: role } : current,
        )
      },
      setActiveOrganization: (organization: OrganizationMembership | null) => {
        setSession((current) => {
          if (!organization) return { ...current, activeOrganization: null }
          const known = current.organizations.some(
            (membership) => membership.id === organization.id,
          )
          return known ? { ...current, activeOrganization: organization } : current
        })
      },
      hasRole: (role: UserRole) => session.roles.includes(role),
      hasAnyRole: (roles: UserRole[]) => roles.some((role) => session.roles.includes(role)),
      hasAllRoles: (roles: UserRole[]) => roles.every((role) => session.roles.includes(role)),
      // Local JWT tokens carry no OAuth scopes, so permissions are never asserted from them.
      // Capabilities come from persisted roles and organization membership instead.
      hasPermission: () => false,
      hasAnyPermission: () => false,
    }),
    [session, accessToken, persona, bootstrap, logout, clearSession],
  )

  const bridgeValue = useMemo<DevelopmentAuthBridgeValue>(
    () => ({ personas, persona, expiresAt, login, pendingPersona }),
    [personas, persona, expiresAt, login, pendingPersona],
  )

  return (
    <DevelopmentAuthBridge.Provider value={bridgeValue}>
      <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>
    </DevelopmentAuthBridge.Provider>
  )
}

export { normalizeAccount }