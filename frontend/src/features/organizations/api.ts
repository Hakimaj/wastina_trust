import { apiClient, type Page } from '../../api'
import { ApiError } from '../../api/errors'
import type { OrganizationMembership } from '../auth/authTypes'

export type { OrganizationMembership }

/**
 * Organization and membership API.
 *
 * Every endpoint derives the acting user from the bearer token; none accepts a caller-supplied
 * actor id. `canManageMemberships`, `canVerifyEmployment` and `isOwner` are computed server-side so
 * the UI never has to re-implement the role rules.
 */

export type OrganizationSummary = {
  id: string
  name: string
  legalName: string | null
  description: string | null
  status: string
  createdAt: string | null
  canManageMemberships: boolean
  isOwner: boolean
}

export type OrganizationInvitation = {
  id: string
  organizationId: string
  grantedMembershipRole: string
  status: string
  createdAt: string
  expiresAt: string | null
  redeemable: boolean
  /**
   * Present only in the immediate response to `createInvitation`. The backend stores only a digest,
   * so it can never be shown again — the UI must not offer to "view" it later.
   */
  joinCode: string | null
}

export type CreateOrganizationInput = {
  name: string
  legalName?: string | null
  description?: string | null
}

export function getMyMemberships(signal?: AbortSignal): Promise<OrganizationMembership[]> {
  return apiClient.get<OrganizationMembership[]>('/api/v1/me/organizations', { signal })
}

export function getOrganization(
  organizationId: string,
  signal?: AbortSignal,
): Promise<OrganizationSummary> {
  return apiClient.get<OrganizationSummary>(`/api/v1/organizations/${organizationId}`, { signal })
}

export function createOrganization(
  input: CreateOrganizationInput,
): Promise<OrganizationSummary> {
  return apiClient.post<OrganizationSummary>('/api/v1/organizations', input)
}

export function joinOrganization(joinCode: string): Promise<OrganizationMembership> {
  return apiClient.post<OrganizationMembership>('/api/v1/organizations/join', { joinCode })
}

export function leaveOrganization(organizationId: string): Promise<void> {
  return apiClient.delete<void>(`/api/v1/organizations/${organizationId}/membership`)
}

/**
 * Fetches a member page.
 *
 * Page parameters travel as a query string, so they are built with `URLSearchParams` rather than
 * concatenated by hand. This is the only paged endpoint the organizations screen consumes; the
 * shared `Page` envelope is the same one used by every other paged backend endpoint.
 */
export function getOrganizationMembers(
  organizationId: string,
  page = 0,
  size = 20,
  signal?: AbortSignal,
): Promise<Page<OrganizationMembership>> {
  const query = new URLSearchParams({ page: String(page), size: String(size), sort: 'role' })
  return apiClient.get<Page<OrganizationMembership>>(
    `/api/v1/organizations/${organizationId}/members?${query.toString()}`,
    { signal },
  )
}

export function updateMemberRole(
  organizationId: string,
  memberUserId: string,
  role: string,
): Promise<OrganizationMembership> {
  return apiClient.patch<OrganizationMembership>(
    `/api/v1/organizations/${organizationId}/members/${memberUserId}`,
    { role },
  )
}

export function createInvitation(
  organizationId: string,
  joinCode?: string,
): Promise<OrganizationInvitation> {
  return apiClient.post<OrganizationInvitation>(
    `/api/v1/organizations/${organizationId}/invitations`,
    joinCode ? { joinCode } : {},
  )
}

export function listInvitations(
  organizationId: string,
  signal?: AbortSignal,
): Promise<OrganizationInvitation[]> {
  return apiClient.get<OrganizationInvitation[]>(
    `/api/v1/organizations/${organizationId}/invitations`,
    { signal },
  )
}

export function revokeInvitation(
  organizationId: string,
  invitationId: string,
): Promise<void> {
  return apiClient.delete<void>(
    `/api/v1/organizations/${organizationId}/invitations/${invitationId}`,
  )
}

export function describeOrganizationError(error: unknown): string {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'CONFLICT':
        return 'That action conflicts with the current state of the organization.'
      case 'FORBIDDEN':
        return 'You do not have permission to manage this organization.'
      case 'NOT_FOUND':
        return 'That organization could not be found.'
      case 'VALIDATION_ERROR':
        return error.fieldErrors[0]?.message ?? error.message
      default:
        return error.message
    }
  }
  return error instanceof Error ? error.message : 'The request could not be completed.'
}