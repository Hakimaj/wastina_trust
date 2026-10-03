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

export type WastinaAccount = {
  id: string
  accountStatus: string
  identityStatus: string
  roles: UserRole[]
}

export type OrganizationMembership = {
  id: string
  organizationId: string
  role: string
  status: string
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
  'not-found' | 'unauthorized' | 'forbidden' | 'conflict' | 'server' | 'unknown'
