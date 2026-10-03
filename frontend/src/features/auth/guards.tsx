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

export function PermissionGuard({ permission }: { permission: string }) {
  const auth = useAuth()
  if (auth.status !== 'authenticated') return <Navigate to="/login" replace />
  return auth.hasPermission(permission) ? <Outlet /> : <UnauthorizedPage />
}
