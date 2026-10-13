import type { ReactNode } from 'react'
import { Outlet } from 'react-router-dom'
import { config } from '../config'
import { AuthProvider } from '../../features/auth/AuthProvider'
import { DevelopmentAuthProvider } from '../../features/auth/DevelopmentAuthProvider'

/**
 * Selects the session provider.
 *
 * `VITE_AUTH_MODE` decides, so the Keycloak path is never removed — only not selected. A
 * production build cannot select `dev-jwt`; `loadConfig` throws if that is attempted.
 */
export function AuthSessionBoundary({ children }: { children?: ReactNode }) {
  const SessionProvider = config.authMode === 'dev-jwt' ? DevelopmentAuthProvider : AuthProvider

  return <SessionProvider>{children ?? <Outlet />}</SessionProvider>
}