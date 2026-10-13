import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { queryKeys } from '../../api/queryKeys'
import {
  AsyncContent,
  Badge,
  Card,
  CellText,
  DefinitionList,
  ErrorState,
  StatusBadge,
} from '../../components/primitives'
import { PageContainer } from '../../components/layout/PageContainer'
import { useAuth } from '../auth/AuthContext'
import { getEmploymentHistory } from '../employment/api'
import { getIdentityVerifications } from '../identity/api'
import { getEmployerProfile, getGuarantorProfile, getWorkerProfile } from '../profile/api'
import { getMyTrustScore } from '../trust/api'
import { getMyMemberships } from '../organizations/api'
import { useI18n } from '../../i18n/useI18n'

/** Contact details the account exposes, shown only when the backend returned them. */
function ContactSection() {
  const auth = useAuth()
  const account = auth.account
  if (!account) return null
  const hasContact = Boolean(account.email || account.phoneNumber)
  if (!hasContact) return null
  return (
    <DefinitionList
      items={[
        ...(account.email
          ? [{ label: 'Email', value: account.email, key: 'email' }]
          : []),
        ...(account.phoneNumber
          ? [{ label: 'Phone', value: account.phoneNumber, key: 'phone' }]
          : []),
      ]}
    />
  )
}

export function DashboardPage() {
  const auth = useAuth()
  const { t } = useI18n()
  const userId = auth.account?.id ?? ''
  const isWorker = auth.roles.includes('ROLE_WORKER')
  const isEmployer = auth.roles.includes('ROLE_EMPLOYER')
  const isGuarantor = auth.roles.includes('ROLE_GUARANTOR')

  const workerProfile = useQuery({
    queryKey: queryKeys.profiles.worker(userId),
    queryFn: () => getWorkerProfile(userId),
    enabled: Boolean(userId && isWorker),
  })
  const employerProfile = useQuery({
    queryKey: queryKeys.profiles.employer(userId),
    queryFn: () => getEmployerProfile(userId),
    enabled: Boolean(userId && isEmployer),
  })
  const guarantorProfile = useQuery({
    queryKey: queryKeys.profiles.guarantor(userId),
    queryFn: () => getGuarantorProfile(userId),
    enabled: Boolean(userId && isGuarantor),
  })
  const employment = useQuery({
    queryKey: queryKeys.employment.history(),
    queryFn: () => getEmploymentHistory(),
    enabled: Boolean(userId),
  })
  const verifications = useQuery({
    queryKey: queryKeys.identity.verifications(),
    queryFn: () => getIdentityVerifications(),
    enabled: Boolean(userId),
  })
  const organizations = useQuery({
    queryKey: queryKeys.organizations.mine(),
    queryFn: () => getMyMemberships(),
    enabled: Boolean(userId),
  })
  const trustScore = useQuery({
    queryKey: queryKeys.trust.mine(),
    queryFn: () => getMyTrustScore(),
    enabled: Boolean(userId),
  })

  if (auth.status !== 'authenticated' || !auth.account) {
    return (
      <PageContainer title={t('dashboard.title')}>
        <ErrorState
          title={t('common.status')}
          message={auth.error ?? t('dashboard.loadError')}
        />
      </PageContainer>
    )
  }

  const account = auth.account
  const latestVerification = verifications.data?.[0]

  return (
    <PageContainer
      title={t('dashboard.title')}
      description={t('dashboard.intro')}
    >
      <div className="dashboard-grid">
        <Card title={t('dashboard.account')} className="dashboard-card dashboard-card--wide">
          <DefinitionList
            items={[
              {
                label: t('dashboard.accountId'),
                value: <span className="reference">{account.id}</span>,
                key: 'id',
              },
              {
                label: t('dashboard.accountStatus'),
                value: <StatusBadge value={account.accountStatus} />,
                key: 'accountStatus',
              },
              {
                label: t('dashboard.identityStatus'),
                value: <StatusBadge value={account.identityStatus} />,
                key: 'identityStatus',
              },
              {
                label: t('dashboard.roles'),
                value:
                  account.roles
                    .map((role) => role.replace('ROLE_', '').toLowerCase())
                    .join(', ') || '—',
                key: 'roles',
              },
            ]}
          />
          <ContactSection />
          {auth.identity?.claims.developmentOnly ? (
            <p className="data-note">
              {t('devAuth.title')}:{' '}
              <code>{String(auth.identity.claims.persona ?? auth.identity.preferredUsername)}</code>
            </p>
          ) : null}
        </Card>

        <Card title={t('dashboard.profiles')} className="dashboard-card">
          <ProfileSummary
            label={t('profile.worker')}
            query={workerProfile}
            enabled={isWorker}
          />
          <ProfileSummary
            label={t('profile.employer')}
            query={employerProfile}
            enabled={isEmployer}
          />
          <ProfileSummary
            label={t('profile.guarantor')}
            query={guarantorProfile}
            enabled={isGuarantor}
          />
          <Link className="button button--secondary" to="/app/profile">
            {t('profile.title')}
          </Link>
        </Card>

        <Card title={t('dashboard.employment')} className="dashboard-card">
          <AsyncContent
            isLoading={employment.isLoading}
            error={employment.error}
            data={employment.data}
            isEmpty={(records) => records.length === 0}
            emptyTitle={t('dashboard.noEmployment')}
            onRetry={() => void employment.refetch()}
          >
            {(records) => (
              <>
                <p className="metric-value">{records.length}</p>
                <p>{t('dashboard.employmentCount')}</p>
                <p>
                  <CellText value={records[0]?.jobTitleEn} /> ·{' '}
                  <CellText value={records[0]?.organizationName} />
                </p>
              </>
            )}
          </AsyncContent>
          <Link className="text-link" to="/app/employment">
            {t('employment.history')}
          </Link>
        </Card>

        <Card title={t('dashboard.organizations')} className="dashboard-card">
          <AsyncContent
            isLoading={organizations.isLoading}
            error={organizations.error}
            data={organizations.data}
            isEmpty={(memberships) => memberships.length === 0}
            emptyTitle={t('dashboard.noOrganizations')}
            onRetry={() => void organizations.refetch()}
          >
            {(memberships) => (
              <ul className="summary-list">
                {memberships.map((membership) => (
                  <li className="summary-row" key={membership.id}>
                    <span>
                      <CellText
                        value={membership.name ?? membership.legalName}
                      />
                    </span>
                    <Badge>
                      {membership.role} · {membership.status}
                    </Badge>
                  </li>
                ))}
              </ul>
            )}
          </AsyncContent>
          <Link className="text-link" to="/app/organizations">
            {t('organizations.title')}
          </Link>
        </Card>

        <Card title={t('dashboard.verification')} className="dashboard-card">
          <AsyncContent
            isLoading={verifications.isLoading}
            error={verifications.error}
            data={verifications.data}
            isEmpty={(attempts) => attempts.length === 0}
            emptyTitle={t('dashboard.noVerification')}
            onRetry={() => void verifications.refetch()}
          >
            {(attempts) => (
              <div className="summary-row">
                <span>
                  <CellText value={attempts[0]?.provider} />
                </span>
                <StatusBadge value={attempts[0]?.status} />
              </div>
            )}
          </AsyncContent>
          <Link className="text-link" to="/app/identity">
            {t('identity.title')}
          </Link>
        </Card>

        <Card title={t('navigation.trust')} className="dashboard-card">
          <AsyncContent
            isLoading={trustScore.isLoading}
            error={trustScore.error}
            data={trustScore.data}
            isEmpty={(score) => score === null}
            emptyTitle={t('trust.noScore')}
            onRetry={() => void trustScore.refetch()}
          >
            {(score) =>
              score ? (
                <DefinitionList
                  items={[
                    {
                      label: t('trust.score'),
                      value: <span className="metric-value">{score.score}</span>,
                      key: 'score',
                    },
                    {
                      label: t('trust.confidence'),
                      value: <CellText value={score.confidence} />,
                      key: 'confidence',
                    },
                    {
                      label: t('trust.calculatedAt'),
                      value: <CellText value={score.calculatedAt} />,
                      key: 'calculatedAt',
                    },
                  ]}
                />
              ) : null
            }
          </AsyncContent>
          <Link className="text-link" to="/app/trust">
            {t('navigation.trust')}
          </Link>
        </Card>
      </div>
    </PageContainer>
  )
}

function ProfileSummary({
  label,
  query,
  enabled,
}: {
  label: string
  query: {
    isLoading: boolean
    isError: boolean
    isSuccess: boolean
    data: unknown
  }
  enabled: boolean
}) {
  const { t } = useI18n()
  if (!enabled) return null
  return (
    <div className="summary-row">
      <span>{label}</span>
      {query.isLoading ? <span>{t('common.loading')}…</span> : null}
      {query.isError ? <Badge tone="danger">{t('dashboard.loadError')}</Badge> : null}
      {query.isSuccess ? (
        <Badge tone={query.data ? 'success' : 'warning'}>
          {query.data ? t('status.active') : t('dashboard.noProfile')}
        </Badge>
      ) : null}
    </div>
  )
}
