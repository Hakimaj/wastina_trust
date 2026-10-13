import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Badge,
  Button,
  Card,
  CellText,
  DefinitionList,
  EmptyState,
  ErrorState,
  LoadingState,
  StatusBadge,
} from '../../components/primitives'
import { PageContainer } from '../../components/layout/PageContainer'
import { useOrganizationScope } from '../../components/layout/OrganizationSwitcher'
import { useToast } from '../../components/feedback/useToast'
import { queryKeys } from '../../api/queryKeys'
import { useAuth } from '../auth/AuthContext'
import {
  beginEmploymentReview,
  describeEmploymentError,
  getOrganizationVerificationRequests,
  verifyEmployment,
  type EmploymentVerificationRequest,
} from '../employment/api'
import { useI18n } from '../../i18n/useI18n'

const OPEN_STATES = ['CREATED', 'SENT', 'DELIVERED', 'VIEWED', 'IN_REVIEW']

/**
 * Employer verification inbox.
 *
 * Lists outstanding employment-verification requests addressed to organizations the
 * signed-in account can verify through. Access is guarded both here and by the
 * backend: the list only ever shows requests the caller is authorized to act on.
 */
export function VerificationsPage() {
  const auth = useAuth()
  const { t } = useI18n()
  const navigate = useNavigate()
  const { effective } = useOrganizationScope()

  const verifiable = auth.organizations.filter(
    (membership) =>
      membership.status === 'ACTIVE' && membership.canVerifyEmployment,
  )

  const query = useQuery({
    queryKey: queryKeys.employment.verificationRequests.all,
    queryFn: async () => {
      const results = await Promise.allSettled(
        verifiable.map((membership) =>
          getOrganizationVerificationRequests(membership.organizationId),
        ),
      )
      return results.flatMap((result, index) =>
        result.status === 'fulfilled'
          ? result.value.map((request) => ({
              ...request,
              organizationName:
                verifiable[index]?.name ?? request.organizationName,
            }))
          : [],
      )
    },
    enabled: verifiable.length > 0,
  })

  if (verifiable.length === 0) {
    return (
      <PageContainer title={t('employment.employerInbox')}>
        <EmptyState
          title={t('verifications.noAccessTitle')}
          description={t('verifications.noAccess')}
        />
      </PageContainer>
    )
  }

  const requests = query.data ?? []
  const openCount = requests.filter((request) =>
    OPEN_STATES.includes(request.status),
  ).length

  return (
    <PageContainer
      title={t('employment.employerInbox')}
      description={t('verifications.intro')}
    >
      {query.isLoading ? (
        <LoadingState rows={4} />
      ) : query.isError ? (
        <ErrorState
          message={describeEmploymentError(query.error)}
          onRetry={() => void query.refetch()}
        />
      ) : requests.length === 0 ? (
        <EmptyState
          title={t('verifications.emptyTitle')}
          description={t('verifications.empty')}
        />
      ) : (
        <Card title={t('employment.verificationRequests')} className="feature-card">
          <ul className="verification-list">
            {requests.map((request) => (
              <li className="verification-list__row" key={request.id}>
                <button
                  type="button"
                  className="verification-list__button"
                  onClick={() =>
                    navigate(`/app/verifications/${request.id}`, {
                      state: { organizationId: request.organizationId },
                    })
                  }
                >
                  <span className="verification-list__title">
                    <CellText value={request.jobTitleEn} />
                    <span className="verification-list__org">
                      <CellText value={request.organizationName} />
                    </span>
                  </span>
                  <span className="verification-list__meta">
                    <StatusBadge value={request.status} />
                    <Badge>{request.verificationMethod}</Badge>
                  </span>
                </button>
              </li>
            ))}
          </ul>
          <p className="data-note">
            {t('verifications.openCount')}: {openCount}
          </p>
        </Card>
      )}
    </PageContainer>
  )
}

/**
 * One verification request, with the employer's review and verify actions.
 *
 * The organization the request belongs to arrives through route state from the
 * inbox; the effective organization is the fallback when the page is opened
 * directly. The backend re-authorizes every action regardless.
 */
export function VerificationDetailPage() {
  const auth = useAuth()
  const { t } = useI18n()
  const navigate = useNavigate()
  const { requestId } = useParams<{ requestId: string }>()
  const location = useLocation()
  const { effective } = useOrganizationScope()
  const { notify } = useToast()
  const queryClient = useQueryClient()

  const routeOrganizationId = (location.state as { organizationId?: string } | null)
    ?.organizationId
  const organizationId = routeOrganizationId ?? effective?.organizationId

  // The request is located within the organization's own request list, which is
  // the only endpoint that can return it, so the detail view re-reads that list
  // and finds the row. This keeps the detail page consistent with the inbox.
  const listQuery = useQuery({
    queryKey: queryKeys.employment.verificationRequests.organization(
      organizationId ?? '',
    ),
    queryFn: () =>
      getOrganizationVerificationRequests(organizationId ?? ''),
    enabled: Boolean(organizationId && requestId),
  })

  const request = listQuery.data?.find((candidate) => candidate.id === requestId)

  const review = useMutation({
    mutationFn: () =>
      beginEmploymentReview(organizationId ?? '', requestId ?? ''),
    onSuccess: async () => {
      await invalidate()
      notify(t('employment.reviewStarted'), 'success')
    },
    onError: (error) => notify(describeEmploymentError(error), 'danger'),
  })

  const verify = useMutation({
    mutationFn: () =>
      verifyEmployment(organizationId ?? '', requestId ?? ''),
    onSuccess: async () => {
      await invalidate()
      notify(t('employment.verified'), 'success')
    },
    onError: (error) => notify(describeEmploymentError(error), 'danger'),
  })

  async function invalidate() {
    await Promise.all([
      queryClient.invalidateQueries({
        queryKey: queryKeys.employment.verificationRequests.all,
      }),
      queryClient.invalidateQueries({
        queryKey: queryKeys.employment.history(),
      }),
    ])
  }

  if (!organizationId) {
    return (
      <PageContainer title={t('employment.employerInbox')}>
        <EmptyState
          title={t('verifications.noAccessTitle')}
          description={t('verifications.noAccess')}
        />
      </PageContainer>
    )
  }

  if (listQuery.isLoading) {
    return (
      <PageContainer title={t('employment.employerInbox')}>
        <LoadingState rows={3} />
      </PageContainer>
    )
  }

  if (listQuery.isError) {
    return (
      <PageContainer title={t('employment.employerInbox')}>
        <ErrorState
          message={describeEmploymentError(listQuery.error)}
          onRetry={() => void listQuery.refetch()}
        />
      </PageContainer>
    )
  }

  if (!request) {
    return (
      <PageContainer title={t('employment.employerInbox')}>
        <EmptyState
          title={t('verifications.notFoundTitle')}
          description={t('verifications.notFound')}
        />
      </PageContainer>
    )
  }

  const isOpen = OPEN_STATES.includes(request.status)
  const busy = review.isPending || verify.isPending

  return (
    <PageContainer
      title={t('verifications.detailTitle')}
      description={request.jobTitleEn}
      actions={
        <Button
          type="button"
          variant="ghost"
          onClick={() => navigate('/app/verifications')}
        >
          {t('verifications.back')}
        </Button>
      }
    >
      <Card title={t('verifications.detailTitle')} className="feature-card">
        <DefinitionList
          items={[
            {
              label: t('employment.jobTitleEn'),
              value: <CellText value={request.jobTitleEn} />,
              key: 'jobTitle',
            },
            {
              label: t('employment.organization'),
              value: <CellText value={request.organizationName} />,
              key: 'organization',
            },
            {
              label: t('common.status'),
              value: <StatusBadge value={request.status} />,
              key: 'status',
            },
            {
              label: t('verifications.method'),
              value: <CellText value={request.verificationMethod} />,
              key: 'method',
            },
            {
              label: t('identity.initiated'),
              value: (
                <CellText
                  value={
                    request.requestedAt
                      ? new Date(request.requestedAt).toLocaleString()
                      : null
                  }
                />
              ),
              key: 'requestedAt',
            },
            {
              label: t('verifications.responseAt'),
              value: (
                <CellText
                  value={
                    request.responseAt
                      ? new Date(request.responseAt).toLocaleString()
                      : null
                  }
                />
              ),
              key: 'responseAt',
            },
          ]}
        />

        {isOpen ? (
          <div className="form-actions">
            <Button
              type="button"
              variant="secondary"
              loading={review.isPending}
              disabled={busy || request.status === 'IN_REVIEW'}
              onClick={() => review.mutate()}
            >
              {t('employment.beginReview')}
            </Button>
            <Button
              type="button"
              variant="primary"
              loading={verify.isPending}
              disabled={busy}
              onClick={() => verify.mutate()}
            >
              {t('employment.verify')}
            </Button>
          </div>
        ) : (
          <p className="data-note">{t('verifications.closed')}</p>
        )}
      </Card>
    </PageContainer>
  )
}

export type { EmploymentVerificationRequest }