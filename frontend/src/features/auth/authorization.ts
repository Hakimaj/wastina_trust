import type { AuthSession, OrganizationMembership, UserRole } from './authTypes'

export type Capability = string

export function hasRole(
  session: Pick<AuthSession, 'roles'>,
  role: UserRole,
): boolean {
  return session.roles.includes(role)
}

export function hasAnyRole(
  session: Pick<AuthSession, 'roles'>,
  roles: UserRole[],
): boolean {
  return roles.some((role) => hasRole(session, role))
}

export function hasAllRoles(
  session: Pick<AuthSession, 'roles'>,
  roles: UserRole[],
): boolean {
  return roles.every((role) => hasRole(session, role))
}

export function hasPermission(
  session: Pick<AuthSession, 'scopes'>,
  permission: Capability,
): boolean {
  return session.scopes.includes(permission)
}

export function hasAnyPermission(
  session: Pick<AuthSession, 'scopes'>,
  permissions: Capability[],
): boolean {
  return permissions.some((permission) => hasPermission(session, permission))
}

export function hasOrganizationAccess(
  session: Pick<AuthSession, 'organizations'>,
  organizationId: string,
): boolean {
  return session.organizations.some(
    (membership) =>
      membership.organizationId === organizationId &&
      membership.status === 'ACTIVE',
  )
}

export function selectDefaultRole(roles: UserRole[]): UserRole | null {
  return [...roles].sort((left, right) => left.localeCompare(right))[0] ?? null
}

export function selectDefaultOrganization(
  organizations: OrganizationMembership[],
): OrganizationMembership | null {
  return (
    organizations.find((membership) => membership.status === 'ACTIVE') ?? null
  )
}
