import { apiClient } from '../../api'
import { ApiError } from '../../api/errors'

export type WorkerProfile = {
  userId: string
  profession: string | null
  location: string | null
  trustScore: number | null
  available?: boolean
  isAvailable?: boolean
  verificationBadge: boolean
}

export type EmployerProfile = {
  userId: string
  companyName: string | null
  location: string | null
  subscriptionTier: string | null
  totalHires: number | null
}

export type GuarantorProfile = {
  userId: string
  financialLimit: number | null
  employerName: string | null
  kycStatus: string | null
}

export type WorkerProfileInput = {
  professionName: string
  locationName: string
  isAvailable: boolean
}

export type EmployerProfileInput = {
  companyName: string
  locationName: string
  subscriptionTier: string
}

export type GuarantorProfileInput = {
  financialLimit: number | null
  employerName: string
}

async function getProfile<T>(path: string): Promise<T | null> {
  try {
    return await apiClient.get<T>(path)
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null
    throw error
  }
}

export const getWorkerProfile = (userId: string) =>
  getProfile<WorkerProfile>(`/api/workers/${userId}/profile`)

export const saveWorkerProfile = (userId: string, input: WorkerProfileInput) =>
  apiClient.put<WorkerProfile>(`/api/workers/${userId}/profile`, input)

export const getEmployerProfile = (userId: string) =>
  getProfile<EmployerProfile>(`/api/employers/${userId}/profile`)

export const saveEmployerProfile = (userId: string, input: EmployerProfileInput) =>
  apiClient.put<EmployerProfile>(`/api/employers/${userId}/profile`, input)

export const getGuarantorProfile = (userId: string) =>
  getProfile<GuarantorProfile>(`/api/guarantors/${userId}/profile`)

export const saveGuarantorProfile = (userId: string, input: GuarantorProfileInput) =>
  apiClient.put<GuarantorProfile>(`/api/guarantors/${userId}/profile`, input)