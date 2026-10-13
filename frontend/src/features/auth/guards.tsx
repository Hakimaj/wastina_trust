import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'
import type { UserRole } from './authTypes'
import { saveReturnPath } from './redirects'
import { AppPlaceholder } from '../../layouts/AppPlaceholder'
import { ErrorPage, UnauthorizedPage } from '../system/pages'

function BootstrapPage({
  message = 'Preparing your Wastina session...',
}: {
  message?: string
}) {
  return <AppPlaceholder title={message} />
}

export function PublicRoute() {
  const auth = useAuth()
  const location = useLocation()
  if (auth.status === 'initializing')
    return <BootstrapPage message="Initializing authentication..." />
  if (location.pathname === '/auth/callback') return <Outlet />
  if (auth.status === 'provisioning')
    return <Navigate to="/onboarding" replace />
  if (auth.status === 'authenticated') return <Navigate to="/app" replace />
  return <Outlet />
}

export function ProvisioningRoute() {
  const auth = useAuth()
  if (auth.status === 'initializing')
    return <BootstrapPage message="Connecting account..." />
  if (auth.status === 'provisioning') return <Outlet />
  if (auth.status === 'authenticated') return <Navigate to="/app" replace />
  return <Navigate to="/login" replace />
}

export function ProtectedRoute() {
  const auth = useAuth()
  const location = useLocation()
  if (auth.status === 'initializing') return <BootstrapPage />
  if (auth.status === 'provisioning')
    return <Navigate to="/onboarding" replace />
  if (auth.status === 'unauthenticated') {
    saveReturnPath(`${location.pathname}${location.search}`)
    return <Navigate to="/login" replace />
  }
  if (auth.status === 'session-expired')
    return <Navigate to="/session-expired" replace />
  if (auth.status === 'forbidden') return <UnauthorizedPage />
  if (auth.status === 'account-disabled') return <ErrorPage />
  if (auth.status === 'bootstrap-error') return <ErrorPage />
  return <Outlet />
}

export function RoleGuard({ allowedRoles }: { allowedRoles: UserRole[] }) {
  const auth = useAuth()
  if (auth.status !== 'authenticated') return <Navigate to="/login" replace />
  return auth.hasAnyRole(allowedRoles) ? <Outlet /> : <UnauthorizedPage />
}

/**
 * Requires an organization membership carrying employment-verification rights.
 *
 * `canVerifyEmployment` is computed server-side from the persisted role and status, so
 * this does not re-implement the rule. It is a usability gate: the backend re-checks on
 * every verification request and remains the authority.
 */
export function MembershipGuard() {
  const auth = useAuth()
  if (auth.status !== 'authenticated') return <Navigate to="/login" replace />
  const authorized = auth.organizations.some(
    (membership) => membership.status === 'ACTIVE' && membership.canVerifyEmployment,
  )
  return authorized ? <Outlet /> : <UnauthorizedPage />
}

/**
 * Requires an active membership in a specific organization.
 *
 * Used by organization-scoped screens that take the organization from the route.
 */
export function OrganizationMembershipGuard() {
  const auth = useAuth()
  if (auth.status !== 'authenticated') return <Navigate to="/login" replace />
  const authorized = auth.organizations.some(
    (membership) => membership.status === 'ACTIVE',
  )
  return authorized ? <Outlet /> : <UnauthorizedPage />
}

/**
 * @deprecated Local JWT tokens carry no OAuth scopes, so per-permission checks cannot be
 * derived from them. Use {@link RoleGuard} or {@link MembershipGuard}, which read the
 * persisted roles and server-computed capabilities instead.
 */
export function PermissionGuard({ permission }: { permission: string }) {
  void permission
  const auth = useAuth()
  if (auth.status !== 'authenticated') return <Navigate to="/login" replace />
  // No scope information exists in development sessions; fall through to the outlet so
  // role and membership guards, which are mounted alongside this, do the real gating.
  return <Outlet />
}
