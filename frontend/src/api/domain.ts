import { z } from 'zod'

/**
 * Enum values shared with the backend, in one place so forms, filters and zod schemas cannot drift
 * from each other or from the server contract.
 *
 * Source of truth: the Java enums on `EmploymentType`, `WorkRelationshipType`, `SectorType`,
 * `EmploymentRecordStatus`, `EmploymentVerificationStatus`,
 * `EmploymentVerificationRequestStatus` and `OrganizationMembershipRole`.
 */

export const employmentTypeValues = [
  'FULL_TIME',
  'PART_TIME',
  'TEMPORARY',
  'CONTRACT',
  'CASUAL',
  'INTERNSHIP',
  'APPRENTICESHIP',
  'CONSULTANT',
  'SEASONAL',
  'OTHER',
] as const

export const workRelationshipTypeValues = [
  'EMPLOYEE',
  'CONTRACTOR',
  'CONSULTANT',
  'SELF_EMPLOYED',
  'VOLUNTEER',
  'INTERN',
  'OTHER',
] as const

export const sectorValues = ['PUBLIC', 'PRIVATE', 'NGO', 'OTHER'] as const

export const subscriptionTierValues = ['FREE', 'PREMIUM'] as const

export const membershipRoleValues = [
  'OWNER',
  'HR_ADMIN',
  'HR_MANAGER',
  'MANAGER',
  'VERIFIER',
  'VIEWER',
  'EMPLOYEE',
] as const
export type MembershipRole = (typeof membershipRoleValues)[number]

export const membershipStatusValues = [
  'ACTIVE',
  'PENDING',
  'REVOKED',
  'SUSPENDED',
  'LEFT',
] as const
export type MembershipStatus = (typeof membershipStatusValues)[number]

export const organizationStatusValues = [
  'PENDING',
  'VERIFICATION_REQUIRED',
  'VERIFIED',
  'SUSPENDED',
  'REJECTED',
  'DEACTIVATED',
] as const
export type OrganizationStatus = (typeof organizationStatusValues)[number]

export const identityStatusValues = [
  'UNVERIFIED',
  'VERIFICATION_PENDING',
  'VERIFIED',
  'VERIFICATION_FAILED',
  'VERIFICATION_REVOKED',
] as const

export const verificationStatusValues = [
  'STARTED',
  'PENDING',
  'SUCCEEDED',
  'FAILED',
  'CANCELLED',
  'EXPIRED',
] as const

export const employmentTypeSchema = z.enum(employmentTypeValues)
export const workRelationshipTypeSchema = z.enum(workRelationshipTypeValues)
export const sectorSchema = z.enum(sectorValues)
export const subscriptionTierSchema = z.enum(subscriptionTierValues)

/** Roles the backend marks as self-service during provisioning. */
export const selfServiceRoles = ['ROLE_WORKER', 'ROLE_GUARANTOR', 'ROLE_EMPLOYER'] as const

/** Every application role the backend recognises. */
export const backendRoles = [
  'ROLE_WORKER',
  'ROLE_GUARANTOR',
  'ROLE_EMPLOYER',
  'ROLE_ADMIN',
] as const

/** Request states in which a verification request is still outstanding. */
export const openVerificationRequestStates = [
  'CREATED',
  'SENT',
  'DELIVERED',
  'VIEWED',
  'IN_REVIEW',
] as const

export type OpenVerificationRequestState =
  (typeof openVerificationRequestStates)[number]

export function isOpenVerificationRequest(status: string): boolean {
  return (openVerificationRequestStates as readonly string[]).includes(status)
}