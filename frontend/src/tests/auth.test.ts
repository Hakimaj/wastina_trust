import { describe, expect, it } from 'vitest'
import { hasAnyPermission, hasAnyRole, hasOrganizationAccess, selectDefaultRole } from '../features/auth/authorization'
import { classifyBootstrapError, normalizeAccount, normalizeIdentity } from '../features/auth/sessionBootstrap'
import { isSafeReturnPath, saveReturnPath, consumeReturnPath } from '../features/auth/redirects'
import type { AuthSession } from '../features/auth/authTypes'

const session: AuthSession = {
  status: 'authenticated',
  identity: null,
  account: null,
  roles: ['ROLE_ADMIN', 'ROLE_WORKER'],
  organizations: [{ id: 'membership-1', organizationId: 'org-1', role: 'OWNER', status: 'ACTIVE' }],
  activeRole: 'ROLE_ADMIN',
  activeOrganization: null,
  scopes: ['role:read'],
  error: null,
}

describe('authentication normalization and authorization foundation', () => {
  it('normalizes the actual /me response shape', () => {
    expect(normalizeAccount({ id: 'user-1', account_status: 'ACTIVE', identity_status: 'UNVERIFIED', roles: ['ROLE_WORKER'] })).toEqual({
      id: 'user-1',
      accountStatus: 'ACTIVE',
      identityStatus: 'UNVERIFIED',
      roles: ['ROLE_WORKER'],
    })
    expect(normalizeIdentity({ sub: 'subject-1', email: 'person@example.test' }).subject).toBe('subject-1')
  })

  it('keeps roles and scopes distinct', () => {
    expect(hasAnyRole(session, ['ROLE_WORKER'])).toBe(true)
    expect(hasAnyPermission(session, ['role:read'])).toBe(true)
    expect(hasAnyPermission(session, ['role:manage'])).toBe(false)
    expect(hasOrganizationAccess(session, 'org-1')).toBe(true)
    expect(selectDefaultRole(['ROLE_WORKER', 'ROLE_ADMIN'])).toBe('ROLE_ADMIN')
  })

  it('allows only internal return paths', () => {
    expect(isSafeReturnPath('/app')).toBe(true)
    expect(isSafeReturnPath('/app?tab=one')).toBe(true)
    expect(isSafeReturnPath('https://evil.example')).toBe(false)
    expect(isSafeReturnPath('//evil.example')).toBe(false)
    saveReturnPath('/app')
    expect(consumeReturnPath()).toBe('/app')
    expect(consumeReturnPath()).toBe('/app')
  })

  it('classifies backend bootstrap failures without logging out on server errors', () => {
    expect(classifyBootstrapError({ status: 404 })).toBe('unknown')
    expect(classifyBootstrapError(new Error('offline'))).toBe('unknown')
  })
})
