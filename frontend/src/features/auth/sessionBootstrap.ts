import { apiClient } from '../../api'
import { ApiError } from '../../api/errors'
import type {
  AuthStatus,
  BootstrapErrorKind,
  OidcIdentity,
  OrganizationMembership,
  WastinaAccount,
} from './authTypes'

export type ProvisionRequest = { email: string; phoneNumber: string }

function readString(
  source: Record<string, unknown>,
  key: string,
): string | null {
  const value = source[key]
  return typeof value === 'string' && value.trim() ? value : null
}

function readBoolean(source: Record<string, unknown>, key: string, fallback: boolean) {
  const value = source[key]
  return typeof value === 'boolean' ? value : fallback
}

function readStringArray(source: Record<string, unknown>, key: string): string[] {
  const value = source[key]
  if (!Array.isArray(value)) return []
  return value.filter((entry): entry is string => typeof entry === 'string')
}

export function normalizeIdentity(claims: Record<string, unknown>): OidcIdentity {
  const subject = readString(claims, 'sub')
  if (!subject) {
    throw new Error('The identity provider did not return a subject claim.')
  }
  return {
    subject,
    email: readString(claims, 'email') ?? undefined,
    name: readString(claims, 'name') ?? undefined,
    preferredUsername:
      readString(claims, 'preferred_username') ??
      readString(claims, 'email') ??
      subject,
    claims,
  }
}

/**
 * Normalizes `GET /api/v1/me`.
 *
 * The backend emits both the original snake_case keys and camelCase equivalents, so the legacy
 * names are read first with the camelCase values as a fallback.
 */
export function normalizeAccount(value: unknown): WastinaAccount {
  if (typeof value !== 'object' || value === null) {
    throw new Error('The Wastina account response was invalid.')
  }
  const account = value as Record<string, unknown>
  const id = readString(account, 'id')
  const accountStatus = readString(account, 'account_status') ?? readString(account, 'accountStatus')
  const identityStatus =
    readString(account, 'identity_status') ?? readString(account, 'identityStatus')

  if (!id || !accountStatus || !identityStatus) {
    throw new Error('The Wastina account response was missing required fields.')
  }

  return {
    id,
    accountStatus,
    identityStatus,
    roles: readStringArray(account, 'roles'),
    email: readString(account, 'email'),
    phoneNumber: readString(account, 'phoneNumber'),
    identitySubject: readString(account, 'identitySubject'),
    verifiedAt: readString(account, 'verifiedAt'),
    createdAt: readString(account, 'createdAt'),
    updatedAt: readString(account, 'updatedAt'),
    enabled: readBoolean(account, 'enabled', true),
  }
}

export function normalizeOrganizations(value: unknown): OrganizationMembership[] {
  if (!Array.isArray(value)) {
    throw new Error('The organization response was invalid.')
  }
  return value.map((item) => {
    if (typeof item !== 'object' || item === null) {
      throw new Error('An organization membership was invalid.')
    }
    const membership = item as Record<string, unknown>
    const id = readString(membership, 'id')
    const organizationId = readString(membership, 'organizationId')
    const role = readString(membership, 'role')
    const status = readString(membership, 'membershipStatus') ?? readString(membership, 'status')

    if (!id || !organizationId || !role || !status) {
      throw new Error('An organization membership was missing required fields.')
    }

    return {
      id,
      organizationId,
      name: readString(membership, 'name'),
      legalName: readString(membership, 'legalName'),
      organizationStatus: (readString(membership, 'organizationStatus') ??
        null) as OrganizationMembership['organizationStatus'],
      role,
      status,
      joinedAt: readString(membership, 'joinedAt'),
      endedAt: readString(membership, 'endedAt'),
      // Server-derived; absent only on very old payloads, where a conservative false is correct.
      canVerifyEmployment: readBoolean(membership, 'canVerifyEmployment', false),
      canManageMemberships: readBoolean(membership, 'canManageMemberships', false),
    }
  })
}

export async function getCurrentAccount(signal?: AbortSignal): Promise<WastinaAccount> {
  return normalizeAccount(await apiClient.get('/api/v1/me', { signal }))
}

export async function getCurrentOrganizations(
  signal?: AbortSignal,
): Promise<OrganizationMembership[]> {
  return normalizeOrganizations(
    await apiClient.get('/api/v1/me/organizations', { signal }),
  )
}

export async function provisionCurrentAccount(
  request: ProvisionRequest,
): Promise<WastinaAccount> {
  return normalizeAccount(await apiClient.post('/api/v1/me/provision', request))
}

export async function updateCurrentAccount(
  request: Partial<ProvisionRequest>,
): Promise<WastinaAccount> {
  return normalizeAccount(await apiClient.patch('/api/v1/me', request))
}

export function classifyBootstrapError(error: unknown): BootstrapErrorKind {
  if (!(error instanceof ApiError)) return 'unknown'
  if (error.status === 401) return 'unauthorized'
  if (error.status === 403) return 'forbidden'
  if (error.status === 404) return 'not-found'
  if (error.status === 409) return 'conflict'
  if (error.status !== undefined && error.status >= 500) return 'server'
  return 'unknown'
}

/** Maps an account status onto the session state the route guards understand. */
export function statusForAccount(accountStatus: string): AuthStatus {
  return accountStatus === 'ACTIVE' ? 'authenticated' : 'account-disabled'
}