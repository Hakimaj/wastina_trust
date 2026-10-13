export { apiClient, request } from './client'
export { configureApiAuth } from './authBridge'
export {
  ApiError,
  errorMessage,
  normalizeApiError,
  parseErrorEnvelope,
} from './errors'
export type {
  ApiErrorCode,
  ApiFieldError,
  ApiServerCode,
} from './errors'
export {
  backendRoles,
  employmentTypeValues,
  employmentTypeSchema,
  isOpenVerificationRequest,
  membershipRoleValues,
  membershipStatusValues,
  openVerificationRequestStates,
  organizationStatusValues,
  sectorSchema,
  sectorValues,
  selfServiceRoles,
  subscriptionTierSchema,
  subscriptionTierValues,
  workRelationshipTypeSchema,
  workRelationshipTypeValues,
} from './domain'
export type { ApiMetadata, ApiResponse, Page, RequestOptions } from './types'
export type {
  MembershipRole,
  MembershipStatus,
  OrganizationStatus,
} from './domain'