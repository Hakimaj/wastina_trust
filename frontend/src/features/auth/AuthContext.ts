import { createContext, useContext } from 'react'
import type { AuthSession, OrganizationMembership, UserRole } from './authTypes'

export type AuthContextValue = AuthSession & {
  login: (returnPath?: string) => Promise<void>
  register: (returnPath?: string) => Promise<void>
  logout: () => Promise<void>
  refreshSession: () => Promise<void>
  provisionAccount: (email: string, phoneNumber: string) => Promise<void>
  setActiveRole: (role: UserRole) => void
  setActiveOrganization: (organization: OrganizationMembership | null) => void
  hasRole: (role: UserRole) => boolean
  hasAnyRole: (roles: UserRole[]) => boolean
  hasAllRoles: (roles: UserRole[]) => boolean
  hasPermission: (permission: string) => boolean
  hasAnyPermission: (permissions: string[]) => boolean
}

export const AuthContext = createContext<AuthContextValue | undefined>(
  undefined,
)

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider.')
  return context
}
