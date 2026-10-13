import { apiClient } from '../../api'
import { ApiError } from '../../api/errors'

/**
 * Identity verification API.
 *
 * The backend mock provider is deterministic and has no out-of-band step, so `redirectUrl` is
 * normally `null` and `actionRequired` is `false`. Both are still modelled: when a real provider is
 * configured the user must be sent to `redirectUrl`, and the UI has to reflect that rather than
 * implying verification is complete.
 */

export type IdentityVerification = {
  id: string
  provider: string
  providerReference: string | null
  status: string
  identityStatus: string
  assuranceLevel: string | null
  failureReasonCode: string | null
  /** Where the user must continue, when the provider requires an out-of-band step. */
  redirectUrl: string | null
  /** True only while the user still has a step to complete outside this application. */
  actionRequired: boolean
  consentId: string | null
  initiatedAt: string
  completedAt: string | null
}

export type StartIdentityVerification = {
  purpose: string
  policyVersion: string
  /** BCP-47. The backend validates this with `^[a-z]{2}(-[A-Z]{2})?$`. */
  locale: string
}

/** Statuses that mean the attempt has finished, one way or the other. */
const TERMINAL_STATUSES = new Set(['SUCCEEDED', 'FAILED', 'CANCELLED', 'EXPIRED'])

export function isVerificationFinished(verification: IdentityVerification): boolean {
  return TERMINAL_STATUSES.has(verification.status)
}

/**
 * True when the attempt succeeded.
 *
 * Deliberately narrower than "not failed": a pending or cancelled attempt must never be rendered
 * as a successful verification.
 */
export function isVerificationSuccessful(verification: IdentityVerification): boolean {
  return verification.status === 'SUCCEEDED' && verification.identityStatus === 'VERIFIED'
}

export function getIdentityVerifications(signal?: AbortSignal): Promise<IdentityVerification[]> {
  return apiClient.get<IdentityVerification[]>('/api/v1/identity-verifications', { signal })
}

export function getIdentityVerification(
  id: string,
  signal?: AbortSignal,
): Promise<IdentityVerification> {
  return apiClient.get<IdentityVerification>(`/api/v1/identity-verifications/${id}`, { signal })
}

/**
 * Starts a verification attempt.
 *
 * Returns the created attempt together with `redirectUrl` / `actionRequired`, so the caller can
 * send the user onward. Starting an attempt is not the same as passing it.
 */
export function startIdentityVerification(
  input: StartIdentityVerification,
): Promise<IdentityVerification> {
  return apiClient.post<IdentityVerification>('/api/v1/identity-verifications', input)
}

/**
 * Resolves an outstanding attempt against the provider.
 *
 * This is the step that moves `PENDING` to a terminal state. The backend scopes it to the caller,
 * so it can only ever complete the signed-in user's own attempt.
 */
export function completeIdentityVerification(id: string): Promise<IdentityVerification> {
  return apiClient.post<IdentityVerification>(`/api/v1/identity-verifications/${id}/complete`)
}

export function describeIdentityError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'CONFLICT') {
      return 'A verification attempt is already in progress.'
    }
    if (error.code === 'VALIDATION_ERROR') {
      return error.fieldErrors[0]?.message ?? error.message
    }
    return error.message
  }
  return error instanceof Error ? error.message : 'The request could not be completed.'
}