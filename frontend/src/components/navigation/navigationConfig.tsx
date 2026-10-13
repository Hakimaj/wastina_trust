import type { ReactNode } from 'react'
import type { OrganizationMembership } from '../../features/auth/authTypes'

export type NavigationItem = {
  id: string
  path: string
  labelKey: string
  /** Account roles that make this item relevant. Empty means "every authenticated user". */
  roles?: string[]
  /**
   * Marks a section whose backend endpoints exist but are not yet safe to expose.
   *
   * Shown in navigation with an explicit "unavailable" marker rather than hidden, so the gap is
   * visible and documented instead of silently missing. None are currently enabled — the audits
   * behind guarantees, payments, documents, compliance and role administration found insufficient
   * authorization guarantees to build against.
   */
  unavailableInDevelopment?: boolean
  /**
   * Requires an organization membership that carries employment-verification rights.
   *
   * Evaluated against the server-computed `canVerifyEmployment`, not against a hardcoded role list.
   */
  requiresVerificationMembership?: boolean
  icon?: ReactNode
}

export const navigationItems: NavigationItem[] = [
  {
    id: 'dashboard',
    path: '/app',
    labelKey: 'navigation.dashboard',
    roles: ['ROLE_WORKER', 'ROLE_GUARANTOR', 'ROLE_EMPLOYER', 'ROLE_ADMIN'],
  },
  {
    id: 'profile',
    path: '/app/profile',
    labelKey: 'navigation.profile',
    roles: ['ROLE_WORKER', 'ROLE_GUARANTOR', 'ROLE_EMPLOYER', 'ROLE_ADMIN'],
  },
  {
    id: 'identity',
    path: '/app/identity',
    labelKey: 'navigation.identity',
    roles: ['ROLE_WORKER', 'ROLE_GUARANTOR', 'ROLE_EMPLOYER', 'ROLE_ADMIN'],
  },
  {
    id: 'employment',
    path: '/app/employment',
    labelKey: 'navigation.employment',
    roles: ['ROLE_WORKER', 'ROLE_GUARANTOR', 'ROLE_EMPLOYER', 'ROLE_ADMIN'],
  },
  {
    id: 'organizations',
    path: '/app/organizations',
    labelKey: 'navigation.organizations',
    roles: ['ROLE_WORKER', 'ROLE_GUARANTOR', 'ROLE_EMPLOYER', 'ROLE_ADMIN'],
  },
  {
    id: 'verifications',
    path: '/app/verifications',
    labelKey: 'navigation.verificationInbox',
    requiresVerificationMembership: true,
  },
  {
    // Read-only: every authenticated account may read its own score. Administrators may read
    // others' server-side, but the UI never exposes that.
    id: 'trust',
    path: '/app/trust',
    labelKey: 'navigation.trust',
    roles: ['ROLE_WORKER', 'ROLE_GUARANTOR', 'ROLE_EMPLOYER', 'ROLE_ADMIN'],
  },
]

/**
 * Sections that exist in the backend but are deliberately not linked.
 *
 * Kept in the codebase as data rather than deleted, so the audit trail survives and a future
 * increment has a record of why they are absent.
 */
export const unavailableSections = [
  {
    id: 'guarantees',
    labelKey: 'navigation.guarantees',
    reason: 'Guarantee issuance and acceptance were reworked server-side; the public-reference ' +
      'lookup and acceptance rules still need verification before a UI is exposed.',
  },
  {
    id: 'payments',
    labelKey: 'navigation.payments',
    reason: 'Payment intents exist but transaction verification and webhook settlement are not ' +
      'confirmed end to end.',
  },
  {
    id: 'documents',
    labelKey: 'navigation.documents',
    reason: 'Document upload requires a server-derived download authorization flow that has not ' +
      'been exercised end to end.',
  },
  {
    id: 'compliance',
    labelKey: 'navigation.cases',
    reason: 'Compliance case administration is restricted to administrators and has no ' +
      'non-admin workflow to build.',
  },
  {
    id: 'roles',
    labelKey: 'navigation.roles',
    reason: 'Role administration endpoints exist but grantable roles are not yet enumerated by ' +
      'the backend, so a form cannot offer a safe subset.',
  },
] as const

export function hasVerificationMembership(
  organizations: OrganizationMembership[],
): boolean {
  return organizations.some(
    (membership) => membership.status === 'ACTIVE' && membership.canVerifyEmployment,
  )
}

export function getVisibleNavigationItems(
  roles: string[],
  organizations: OrganizationMembership[] = [],
): NavigationItem[] {
  return navigationItems.filter((item) => {
    if (item.roles && !item.roles.some((role) => roles.includes(role))) return false
    if (item.requiresVerificationMembership && !hasVerificationMembership(organizations)) {
      return false
    }
    return true
  })
}