import { apiClient } from '../../api'
import { ApiError } from '../../api/errors'
import type { OidcIdentity, WastinaAccount } from './authTypes'

export type ProvisionRequest = { email: string; phoneNumber: string }

export function normalizeIdentity(
  claims: Record<string, unknown>,
): OidcIdentity {
  const stringClaim = (key: string) =>
    typeof claims[key] === 'string' ? claims[key] : undefined
  const subject = stringClaim('sub')
  if (!subject)
    throw new Error('The identity provider did not return a subject claim.')
  return {
    subject,
    email: stringClaim('email'),
    name: stringClaim('name'),
    preferredUsername: stringClaim('preferred_username'),
    claims,
  }
}

export function normalizeAccount(value: unknown): WastinaAccount {
  if (typeof value !== 'object' || value === null)
    throw new Error('The Wastina account response was invalid.')
  const account = value as Record<string, unknown>
  if (
    typeof account.id !== 'string' ||
    typeof account.account_status !== 'string' ||
    typeof account.identity_status !== 'string'
  ) {
    throw new Error('The Wastina account response was missing required fields.')
  }
  const roles = Array.isArray(account.roles)
    ? account.roles.filter((role): role is string => typeof role === 'string')
    : []
  return {
    id: account.id,
    accountStatus: account.account_status,
    identityStatus: account.identity_status,
    roles,
  }
}

export async function getCurrentAccount(
  signal?: AbortSignal,
): Promise<WastinaAccount> {
  return normalizeAccount(await apiClient.get('/api/v1/me', { signal }))
}

export async function provisionCurrentAccount(
  request: ProvisionRequest,
): Promise<WastinaAccount> {
  return normalizeAccount(await apiClient.post('/api/v1/me/provision', request))
}

export function classifyBootstrapError(
  error: unknown,
):
  | 'not-found'
  | 'unauthorized'
  | 'forbidden'
  | 'conflict'
  | 'server'
  | 'unknown' {
  if (!(error instanceof ApiError)) return 'unknown'
  if (error.status === 401) return 'unauthorized'
  if (error.status === 403) return 'forbidden'
  if (error.status === 404) return 'not-found'
  if (error.status === 409) return 'conflict'
  if (error.status !== undefined && error.status >= 500) return 'server'
  return 'unknown'
}
