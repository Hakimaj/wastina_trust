import type { ReactNode } from 'react'

export type NavigationItem = {
  id: string
  path: string
  labelKey: string
  roles?: string[]
  comingSoon?: boolean
  icon?: ReactNode
}

export const navigationItems: NavigationItem[] = [
  { id: 'dashboard', path: '/app', labelKey: 'navigation.dashboard', roles: ['ROLE_WORKER', 'ROLE_GUARANTOR', 'ROLE_EMPLOYER', 'ROLE_ADMIN'] },
  { id: 'profile', path: '/app/profile', labelKey: 'navigation.profile', roles: ['ROLE_WORKER', 'ROLE_GUARANTOR', 'ROLE_EMPLOYER', 'ROLE_ADMIN'] },
  { id: 'identity', path: '/app/identity', labelKey: 'navigation.identity', roles: ['ROLE_WORKER', 'ROLE_GUARANTOR', 'ROLE_EMPLOYER', 'ROLE_ADMIN'] },
  { id: 'employment', path: '/app/employment', labelKey: 'navigation.employment', roles: ['ROLE_WORKER'] },
  { id: 'trust', path: '/app/trust', labelKey: 'navigation.trust', roles: ['ROLE_WORKER', 'ROLE_GUARANTOR'] },
  { id: 'guarantees', path: '/app/guarantees', labelKey: 'navigation.guarantees', roles: ['ROLE_WORKER', 'ROLE_GUARANTOR'] },
  { id: 'documents', path: '/app/documents', labelKey: 'navigation.documents', roles: ['ROLE_WORKER', 'ROLE_GUARANTOR', 'ROLE_EMPLOYER', 'ROLE_ADMIN'] },
  { id: 'payments', path: '/app/payments', labelKey: 'navigation.payments', roles: ['ROLE_WORKER', 'ROLE_GUARANTOR', 'ROLE_EMPLOYER', 'ROLE_ADMIN'] },
  { id: 'security', path: '/app/security', labelKey: 'navigation.security', roles: ['ROLE_WORKER', 'ROLE_GUARANTOR', 'ROLE_EMPLOYER', 'ROLE_ADMIN'] },
  { id: 'support', path: '/app/support', labelKey: 'navigation.support', roles: ['ROLE_WORKER', 'ROLE_GUARANTOR', 'ROLE_EMPLOYER', 'ROLE_ADMIN'] },
  { id: 'guarantor-workspace', path: '/guarantor', labelKey: 'navigation.guarantorWorkspace', roles: ['ROLE_GUARANTOR'] },
  { id: 'employer-workspace', path: '/employer', labelKey: 'navigation.employerWorkspace', roles: ['ROLE_EMPLOYER'] },
  { id: 'admin-workspace', path: '/admin', labelKey: 'navigation.adminWorkspace', roles: ['ROLE_ADMIN'] },
]

export function getVisibleNavigationItems(activeRole: string | null): NavigationItem[] {
  if (!activeRole) return []
  return navigationItems.filter((item) => !item.roles || item.roles.includes(activeRole))
}
