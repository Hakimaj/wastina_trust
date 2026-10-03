export const queryKeys = {
  me: ['me'] as const,
  identityVerification: ['identity-verification'] as const,
  employment: ['employment'] as const,
  employmentById: (employmentId: string) =>
    ['employment', employmentId] as const,
  trust: (userId: string) => ['trust', userId] as const,
  guarantees: ['guarantees'] as const,
  payments: ['payments'] as const,
  documents: ['documents'] as const,
  cases: ['cases'] as const,
  notifications: ['notifications'] as const,
}
