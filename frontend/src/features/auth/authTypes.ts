import type {
  MembershipRole,
  MembershipStatus,
  OrganizationStatus,
} from '../../api/domain'

export type AuthStatus =
  | 'initializing'
  | 'unauthenticated'
  | 'authenticated'
  | 'provisioning'
  | 'bootstrap-error'
  | 'session-expired'
  | 'account-disabled'
  | 'forbidden'

export type OidcIdentity = {
  subject: string
  email?: string
  name?: string
  preferredUsername?: string
  claims: Record<string, unknown>
}

export type BackendRole =
  'ROLE_WORKER' | 'ROLE_GUARANTOR' | 'ROLE_EMPLOYER' | 'ROLE_ADMIN'
export type UserRole = BackendRole | string

/**
 * The authenticated account, from `GET /api/v1/me`.
 *
 * `roles` comes from the persisted database roles, which is the authoritative source for route
 * access and navigation. `identityStatus` drives the identity-verification UI.
 */
export type WastinaAccount = {
  id: string
  accountStatus: string
  identityStatus: string
  roles: UserRole[]
  email: string | null
  phoneNumber: string | null
  identitySubject: string | null
  verifiedAt: string | null
  createdAt: string | null
  updatedAt: string | null
  enabled: boolean
}

/**
 * One organization membership from `GET /api/v1/me/organizations`.
 *
 * `canVerifyEmployment` and `canManageMemberships` are computed server-side, so the frontend does
 * not re-implement the role rules and cannot disagree with the backend.
 */
export type OrganizationMembership = {
  id: string
  organizationId: string
  name: string | null
  legalName: string | null
  organizationStatus: OrganizationStatus | null
  role: MembershipRole | string
  status: MembershipStatus | string
  joinedAt: string | null
  endedAt: string | null
  canVerifyEmployment: boolean
  canManageMemberships: boolean
}

export type AuthSession = {
  status: AuthStatus
  identity: OidcIdentity | null
  account: WastinaAccount | null
  roles: UserRole[]
  organizations: OrganizationMembership[]
  activeRole: UserRole | null
  activeOrganization: OrganizationMembership | null
  scopes: string[]
  error: string | null
}

export type BootstrapErrorKind =
  | 'not-found' | 'unauthorized' | 'forbidden' | 'conflict' | 'server' | 'unknown'

/** True when the session is authenticated against the local development token issuer. */
export function isDevelopmentSession(session: Pick<AuthSession, 'identity'>): boolean {
  return session.identity?.claims.developmentOnly === true
}