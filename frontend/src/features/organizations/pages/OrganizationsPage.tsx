import { useQuery, type UseQueryResult } from '@tanstack/react-query'
import { Alert, Badge, Card, CellText, StatusBadge } from '../../../components/primitives'
import { queryKeys } from '../../../api/queryKeys'
import { useI18n } from '../../../i18n/useI18n'
import { getMyMemberships, type OrganizationMembership } from '../api'

function organizationTone(status: string): 'neutral' | 'success' | 'warning' | 'danger' {
  if (status === 'ACTIVE') return 'success'
  if (status === 'PENDING') return 'warning'
  if (['SUSPENDED', 'REJECTED', 'DISABLED'].includes(status)) return 'danger'
  return 'neutral'
}

export function OrganizationsPage() {
  const { t } = useI18n()
  const queryKey = ['organizations', 'mine'] as const
  const memberships = useQuery({
    queryKey,
    queryFn: async ({ signal }) => getMyMemberships(signal),
  }) satisfies UseQueryResult<OrganizationMembership[]>

  return (
    <div className="workspace-page">
      <header className="page-heading">
        <p className="eyebrow">{t('common.organization')}</p>
        <h1>{t('organizations.title')}</h1>
        <p>{t('organizations.intro')}</p>
      </header>

      <Card title={t('organizations.title')} className="feature-card">
        {memberships.isLoading ? <p role="status">{t('common.loading')}…</p> : null}
        {memberships.isError ? <Alert tone="danger">{memberships.error.message}</Alert> : null}
        {memberships.isSuccess && memberships.data.length === 0 ? <p>{t('organizations.empty')}</p> : null}
        {memberships.isSuccess && memberships.data.length > 0 ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{t('organizations.name')}</th>
                  <th>{t('organizations.legalName')}</th>
                  <th>{t('organizations.organizationStatus')}</th>
                  <th>{t('organizations.membershipRole')}</th>
                  <th>{t('organizations.membershipStatus')}</th>
                </tr>
              </thead>
              <tbody>
                {memberships.data.map((membership) => (
                  <tr key={membership.id}>
                    <td><CellText value={membership.name} /></td>
                    <td><CellText value={membership.legalName} /></td>
                    <td><StatusBadge value={membership.organizationStatus} /></td>
                    <td>{membership.role}</td>
                    <td><StatusBadge value={membership.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </Card>
    </div>
  )
}