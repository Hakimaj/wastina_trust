/**
 * Query key factories.
 *
 * Factories rather than constants so every cache entry is namespaced consistently and invalidation
 * can target a whole family (`queryKeys.employment.all`) or a single row.
 *
 * Only keys for endpoints that exist are declared. Keys for guarantees, payments, documents,
 * compliance cases and notifications were removed: nothing consumes them, and the backend surfaces
 * for those areas are not yet safe to build against.
 */
export const queryKeys = {
  account: {
    all: ['account'] as const,
    me: () => ['account', 'me'] as const,
  },
  organizations: {
    all: ['organizations'] as const,
    mine: () => ['organizations', 'mine'] as const,
    detail: (organizationId: string) => ['organizations', 'detail', organizationId] as const,
    members: (organizationId: string) => ['organizations', 'members', organizationId] as const,
    invitations: (organizationId: string) =>
      ['organizations', 'invitations', organizationId] as const,
  },
  profiles: {
    all: ['profiles'] as const,
    worker: (userId: string) => ['profiles', 'worker', userId] as const,
    employer: (userId: string) => ['profiles', 'employer', userId] as const,
    guarantor: (userId: string) => ['profiles', 'guarantor', userId] as const,
  },
  identity: {
    all: ['identity'] as const,
    verifications: () => ['identity', 'verifications'] as const,
    verification: (id: string) => ['identity', 'verifications', id] as const,
  },
  employment: {
    all: ['employment'] as const,
    history: () => ['employment', 'history'] as const,
    record: (id: string) => ['employment', 'record', id] as const,
    verificationRequests: {
      all: ['employment', 'verification-requests'] as const,
      worker: () => ['employment', 'verification-requests', 'worker'] as const,
      organization: (organizationId: string) =>
        ['employment', 'verification-requests', 'organization', organizationId] as const,
      detail: (organizationId: string, requestId: string) =>
        ['employment', 'verification-requests', 'organization', organizationId, requestId] as const,
    },
  },
  trust: {
    all: ['trust'] as const,
    mine: () => ['trust', 'me'] as const,
    history: (limit: number) => ['trust', 'me', 'history', limit] as const,
  },
} as const