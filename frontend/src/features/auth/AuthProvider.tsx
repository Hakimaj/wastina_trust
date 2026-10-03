import Keycloak from 'keycloak-js'
import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { configureApiAuth } from '../../api'
import { ApiError } from '../../api/errors'
import { authConfig } from './authConfig'
import { AuthContext, type AuthContextValue } from './AuthContext'
import { broadcastLogout, subscribeToAuthEvents } from './authEvents'
import {
  hasAllRoles,
  hasAnyPermission,
  hasAnyRole,
  hasPermission,
  hasRole,
  selectDefaultOrganization,
  selectDefaultRole,
} from './authorization'
import type {
  AuthSession,
  OrganizationMembership,
  OidcIdentity,
} from './authTypes'
import { saveReturnPath } from './redirects'
import {
  classifyBootstrapError,
  getCurrentAccount,
  normalizeIdentity,
  provisionCurrentAccount,
} from './sessionBootstrap'

const unauthenticatedSession: AuthSession = {
  status: 'initializing',
  identity: null,
  account: null,
  roles: [],
  organizations: [],
  activeRole: null,
  activeOrganization: null,
  scopes: [],
  error: null,
}

function scopesFromToken(keycloak: Keycloak): string[] {
  const scope = keycloak.tokenParsed?.scope
  return typeof scope === 'string' ? scope.split(' ').filter(Boolean) : []
}

function identityFromKeycloak(keycloak: Keycloak): OidcIdentity {
  return normalizeIdentity(
    (keycloak.tokenParsed ?? {}) as Record<string, unknown>,
  )
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const keycloakRef = useRef<Keycloak | null>(null)
  const initializationPromiseRef = useRef<Promise<boolean> | null>(null)
  const [session, setSession] = useState<AuthSession>(unauthenticatedSession)

  const clearSession = useCallback(
    (status: AuthSession['status'], error: string | null = null) => {
      setSession({ ...unauthenticatedSession, status, error })
    },
    [],
  )

  const expireSession = useCallback(() => {
    keycloakRef.current?.clearToken()
    clearSession('session-expired', 'Your authentication session has expired.')
    if (window.location.pathname !== '/session-expired')
      window.location.assign('/session-expired')
  }, [clearSession])

  const bootstrap = useCallback(async (keycloak: Keycloak) => {
    if (!keycloak.authenticated) {
      clearSession('unauthenticated')
      return
    }

    const identity = identityFromKeycloak(keycloak)
    const scopes = scopesFromToken(keycloak)
    setSession((current) => ({
      ...current,
      status: 'initializing',
      identity,
      scopes,
      error: null,
    }))

    try {
      const account = await getCurrentAccount()
      const organizations: OrganizationMembership[] = []
      const roles = account.roles
      setSession({
        status:
          account.accountStatus === 'ACTIVE'
            ? 'authenticated'
            : 'account-disabled',
        identity,
        account,
        roles,
        organizations,
        activeRole: selectDefaultRole(roles),
        activeOrganization: selectDefaultOrganization(organizations),
        scopes,
        error:
          account.accountStatus === 'ACTIVE'
            ? null
            : `Account status: ${account.accountStatus}.`,
      })
    } catch (error) {
      const kind = classifyBootstrapError(error)
      if (kind === 'not-found') {
        setSession({
          ...unauthenticatedSession,
          status: 'provisioning',
          identity,
          scopes,
          error:
            'Your identity is authenticated, but your Wastina account needs provisioning.',
        })
        return
      }
      if (kind === 'unauthorized') {
        expireSession()
        return
      }
      if (kind === 'forbidden') {
        setSession({
          ...unauthenticatedSession,
          status: 'forbidden',
          identity,
          scopes,
          error: 'Your account cannot access Wastina.',
        })
        return
      }
      const message =
        error instanceof ApiError
          ? error.message
          : 'Wastina account bootstrap failed.'
      setSession({
        ...unauthenticatedSession,
        status: 'bootstrap-error',
        identity,
        scopes,
        error: message,
      })
    }
  }, [clearSession, expireSession])

  const refreshAccessToken = useCallback(async (): Promise<boolean> => {
    const keycloak = keycloakRef.current
    if (!keycloak?.authenticated) return false
    try {
      await keycloak.updateToken(30)
      return Boolean(keycloak.token)
    } catch {
      expireSession()
      return false
    }
  }, [expireSession])

  useEffect(() => {
    const keycloak =
      keycloakRef.current ??
      new Keycloak({
        url: authConfig.issuer.replace(/\/realms\/[^/]+$/, ''),
        realm: authConfig.realm,
        clientId: authConfig.clientId,
      })
    keycloakRef.current = keycloak
    configureApiAuth({
      getAccessToken: () => keycloak.token,
      refreshAccessToken,
      handleUnauthorized: expireSession,
    })
    keycloak.onTokenExpired = () => void refreshAccessToken()
    keycloak.onAuthLogout = () =>
      clearSession('session-expired', 'You have been signed out.')

    const unsubscribe = subscribeToAuthEvents(() => {
      keycloak.clearToken()
      clearSession(
        'session-expired',
        'You have been signed out in another tab.',
      )
      if (window.location.pathname !== '/session-expired')
        window.location.assign('/session-expired')
    })

    if (!initializationPromiseRef.current) {
      initializationPromiseRef.current = keycloak.init({
        onLoad: 'check-sso',
        pkceMethod: authConfig.pkceMethod,
        checkLoginIframe: authConfig.checkLoginIframe,
        redirectUri: authConfig.redirectUri,
        scope: authConfig.scope,
      })
    }

    void initializationPromiseRef.current
      .then((authenticated) => {
        if (authenticated) void bootstrap(keycloak)
        else clearSession('unauthenticated')
      })
      .catch(() =>
        clearSession(
          'bootstrap-error',
          'Keycloak authentication could not be initialized.',
        ),
      )

    return () => {
      unsubscribe()
    }
    // Initialization is deliberately singleton-scoped for the browser session.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      ...session,
      login: async (returnPath) => {
        saveReturnPath(
          returnPath ?? `${window.location.pathname}${window.location.search}`,
        )
        await keycloakRef.current?.login({
          redirectUri: authConfig.redirectUri,
          scope: authConfig.scope,
        })
      },
      register: async (returnPath) => {
        saveReturnPath(returnPath ?? '/app')
        await keycloakRef.current?.register({
          redirectUri: authConfig.redirectUri,
          scope: authConfig.scope,
        })
      },
      logout: async () => {
        broadcastLogout()
        clearSession('unauthenticated')
        try {
          await keycloakRef.current?.logout({
            redirectUri: authConfig.postLogoutRedirectUri,
          })
        } catch {
          keycloakRef.current?.clearToken()
          window.location.assign(authConfig.postLogoutRedirectUri)
        }
      },
      refreshSession: async () => {
        const keycloak = keycloakRef.current
        if (!keycloak?.authenticated) {
          clearSession('unauthenticated')
          return
        }
        if (!(await refreshAccessToken())) return
        await bootstrap(keycloak)
      },
      provisionAccount: async (email, phoneNumber) => {
        const keycloak = keycloakRef.current
        if (!keycloak?.authenticated) {
          clearSession('unauthenticated')
          return
        }
        setSession((current) => ({
          ...current,
          status: 'provisioning',
          error: null,
        }))
        try {
          const account = await provisionCurrentAccount({ email, phoneNumber })
          const organizations: OrganizationMembership[] = []
          setSession({
            status:
              account.accountStatus === 'ACTIVE'
                ? 'authenticated'
                : 'account-disabled',
            identity: identityFromKeycloak(keycloak),
            account,
            roles: account.roles,
            organizations,
            activeRole: selectDefaultRole(account.roles),
            activeOrganization: selectDefaultOrganization(organizations),
            scopes: scopesFromToken(keycloak),
            error: null,
          })
        } catch (error) {
          setSession((current) => ({
            ...current,
            status: 'bootstrap-error',
            error:
              error instanceof ApiError
                ? error.message
                : 'Account provisioning failed.',
          }))
          throw error
        }
      },
      setActiveRole: (role) => {
        setSession((current) =>
          current.roles.includes(role)
            ? { ...current, activeRole: role }
            : current,
        )
      },
      setActiveOrganization: (organization) => {
        setSession((current) =>
          organization &&
          !current.organizations.some((item) => item.id === organization.id)
            ? current
            : { ...current, activeOrganization: organization },
        )
      },
      hasRole: (role) => hasRole(session, role),
      hasAnyRole: (roles) => hasAnyRole(session, roles),
      hasAllRoles: (roles) => hasAllRoles(session, roles),
      hasPermission: (permission) => hasPermission(session, permission),
      hasAnyPermission: (permissions) => hasAnyPermission(session, permissions),
    }),
    [bootstrap, clearSession, refreshAccessToken, session],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
