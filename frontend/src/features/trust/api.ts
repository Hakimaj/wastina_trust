import { apiClient } from '../../api'
import { ApiError } from '../../api/errors'

/**
 * Trust score API.
 *
 * Only the caller's own score is read. `GET /api/v1/trust-score/{userId}` exists for
 * administrators, but the application never renders another account's private score, so no call to
 * it is made from here.
 *
 * `GET` is read-only. Recalculation is an explicit `POST`, so simply loading this page never
 * mutates a score — which is what the audited `/score` endpoint used to do.
 */

export type TrustScore = {
  userId: string
  /** Overall score, 0-100. */
  score: number
  /** Unrounded score before policy capping. */
  internalScore: number | null
  confidence: string | null
  evidenceStatus: string | null
  policyVersion: string | null
  calculatedAt: string | null
}

export type TrustScoreHistoryEntry = {
  id: string
  userId: string
  previousScore: number
  newScore: number
  previousConfidence: string | null
  newConfidence: string | null
  policyVersion: string | null
  reasonCode: string | null
  createdAt: string
}

/**
 * Reads the stored score.
 *
 * Returns `null` on `404`, which the backend uses when no snapshot has been calculated yet. That is
 * a normal first-run state, not an error, so it must not be rendered as a failure.
 */
export async function getMyTrustScore(signal?: AbortSignal): Promise<TrustScore | null> {
  try {
    return await apiClient.get<TrustScore>('/api/v1/trust-score/me', { signal })
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null
    throw error
  }
}

export function getMyTrustScoreHistory(
  limit = 50,
  signal?: AbortSignal,
): Promise<TrustScoreHistoryEntry[]> {
  return apiClient.get<TrustScoreHistoryEntry[]>(
    `/api/v1/trust-score/me/history?limit=${limit}`,
    { signal },
  )
}

/** Explicit recalculation. Only ever called from a user-initiated action. */
export function recalculateMyTrustScore(): Promise<TrustScore> {
  return apiClient.post<TrustScore>('/api/v1/trust-score/me/recalculate')
}

/** Signed change in points, for the history timeline. */
export function scoreDelta(entry: TrustScoreHistoryEntry): number {
  return entry.newScore - entry.previousScore
}

export function describeTrustError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'FORBIDDEN') return 'You may only view your own trust score.'
    if (error.code === 'NOT_FOUND') return 'No trust score has been calculated for your account yet.'
    return error.message
  }
  return error instanceof Error ? error.message : 'The request could not be completed.'
}