import { apiClient } from '../../api'
import { ApiError } from '../../api/errors'

/**
 * Employment API.
 *
 * Uses only the canonical `/api/v1/me/employment*` family. The legacy endpoints
 * (`POST /api/v1/workers/{id}/employment`, `POST /api/v1/employment/{id}/verification-requests`,
 * `POST /api/v1/organizations/{id}/verification-requests/{id}/verify`) were removed server-side
 * because they accepted a caller-supplied actor id and returned raw JPA entities.
 */

export type EmploymentRecord = {
  id: string
  organizationId: string
  organizationName: string
  jobTitleEn: string
  jobTitleAm: string | null
  occupationCode: string | null
  department: string | null
  location: string | null
  employmentType: string
  workRelationshipType: string
  sector: string
  startDate: string
  endDate: string | null
  currentEmployment: boolean
  description: string | null
  status: string
  verificationStatus: string
}

export type EmploymentVerificationRequest = {
  id: string
  employmentRecordId: string
  jobTitleEn: string
  organizationId: string
  organizationName: string
  status: string
  verificationMethod: string
  requestedAt: string
  responseAt: string | null
}

export type EmploymentCreateInput = {
  organizationId: string
  jobTitleEn: string
  jobTitleAm?: string | null
  occupationCode?: string | null
  department?: string | null
  location?: string | null
  employmentType: string
  workRelationshipType: string
  sector: string
  startDate: string
  endDate?: string | null
  currentEmployment: boolean
  description?: string | null
}

/**
 * A verification request that is still outstanding.
 *
 * The backend rejects a second request while one is open with `409`, so the form uses this to
 * disable submission rather than letting the user trigger a guaranteed failure.
 */
export function openRequestForRecord(
  requests: EmploymentVerificationRequest[],
  employmentRecordId: string,
): EmploymentVerificationRequest | undefined {
  return requests.find(
    (request) =>
      request.employmentRecordId === employmentRecordId &&
      ['CREATED', 'SENT', 'DELIVERED', 'VIEWED', 'IN_REVIEW'].includes(request.status),
  )
}

/**
 * True when this record has already been verified.
 *
 * A verified record must not be re-requested: it would be redundant, and the backend treats a new
 * attempt on a verified record as a business-rule violation.
 */
export function isAlreadyVerified(record: EmploymentRecord): boolean {
  return record.verificationStatus === 'VERIFIED'
}

export function getEmploymentHistory(signal?: AbortSignal): Promise<EmploymentRecord[]> {
  return apiClient.get<EmploymentRecord[]>('/api/v1/me/employment', { signal })
}

export function getEmploymentRecord(
  id: string,
  signal?: AbortSignal,
): Promise<EmploymentRecord> {
  return apiClient.get<EmploymentRecord>(`/api/v1/me/employment/${id}`, { signal })
}

export function createEmployment(input: EmploymentCreateInput): Promise<EmploymentRecord> {
  // Not retried on failure: the API client only replays idempotent methods, so a timed-out POST
  // cannot silently create a second record.
  return apiClient.post<EmploymentRecord>('/api/v1/me/employment', input)
}

export function getWorkerVerificationRequests(
  signal?: AbortSignal,
): Promise<EmploymentVerificationRequest[]> {
  return apiClient.get<EmploymentVerificationRequest[]>(
    '/api/v1/me/employment-verification-requests',
    { signal },
  )
}

/** Asks the record's own organization to verify it. */
export function requestEmploymentVerification(
  employmentRecordId: string,
): Promise<EmploymentVerificationRequest> {
  return apiClient.post<EmploymentVerificationRequest>(
    `/api/v1/me/employment/${employmentRecordId}/verification-requests`,
  )
}

/** Employer side. Requires membership that carries verification rights. */
export function getOrganizationVerificationRequests(
  organizationId: string,
  signal?: AbortSignal,
): Promise<EmploymentVerificationRequest[]> {
  return apiClient.get<EmploymentVerificationRequest[]>(
    `/api/v1/organizations/${organizationId}/employment-verification-requests`,
    { signal },
  )
}

export function beginEmploymentReview(
  organizationId: string,
  requestId: string,
): Promise<EmploymentVerificationRequest> {
  return apiClient.post<EmploymentVerificationRequest>(
    `/api/v1/organizations/${organizationId}/employment-verification-requests/${requestId}/review`,
  )
}

export function verifyEmployment(
  organizationId: string,
  requestId: string,
): Promise<EmploymentVerificationRequest> {
  return apiClient.post<EmploymentVerificationRequest>(
    `/api/v1/organizations/${organizationId}/employment-verification-requests/${requestId}/verify`,
  )
}

/** Normalizes a thrown value into the message shown to the user. */
export function describeEmploymentError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'CONFLICT') {
      return 'A verification request for this record is already in progress.'
    }
    if (error.code === 'FORBIDDEN') {
      return 'You are not allowed to act on this employment record.'
    }
    return error.message
  }
  return error instanceof Error ? error.message : 'The request could not be completed.'
}