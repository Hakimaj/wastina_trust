import { useState } from 'react'
import { useMutation, useQuery, useQueryClient, type UseQueryResult } from '@tanstack/react-query'
import { Alert, Badge, Button, Card } from '../../../components/primitives'
import { FormField } from '../../../components/forms/FormField'
import { queryKeys } from '../../../api/queryKeys'
import { useAuth } from '../../auth/AuthContext'
import { useI18n } from '../../../i18n/useI18n'
import { getIdentityVerifications, startIdentityVerification, type IdentityVerification } from '../api'

function statusTone(status: string): 'neutral' | 'success' | 'warning' | 'danger' {
  if (['VERIFIED', 'SUCCEEDED'].includes(status)) return 'success'
  if (['PENDING', 'VERIFICATION_PENDING', 'STARTED'].includes(status)) return 'warning'
  if (['FAILED', 'VERIFICATION_FAILED', 'CANCELLED', 'EXPIRED'].includes(status)) return 'danger'
  return 'neutral'
}

export function IdentityPage() {
  const auth = useAuth()
  const { t, locale } = useI18n()
  const queryClient = useQueryClient()
  const [purpose, setPurpose] = useState('')
  const [policyVersion, setPolicyVersion] = useState('')
  const [consented, setConsented] = useState(false)
  const verifications = useQuery({
    queryKey: queryKeys.identity.verifications(),
    queryFn: () => getIdentityVerifications(),
  }) satisfies UseQueryResult<IdentityVerification[]>
  const mutation = useMutation({
    mutationFn: () => startIdentityVerification({
      purpose: purpose.trim(),
      policyVersion: policyVersion.trim(),
      locale,
    }),
    onSuccess: async () => {
      setConsented(false)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.identity.verifications() }),
        auth.refreshSession(),
      ])
    },
  })

  function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!consented || !purpose.trim() || !policyVersion.trim()) return
    mutation.mutate()
  }

  return (
    <div className="workspace-page">
      <header className="page-heading">
        <p className="eyebrow">{t('common.status')}</p>
        <h1>{t('identity.title')}</h1>
        <p>{t('identity.intro')}</p>
      </header>

      <div className="feature-grid">
        <Card title={t('identity.currentStatus')} className="feature-card">
          <dl className="data-list">
            <div><dt>{t('dashboard.identityStatus')}</dt><dd><Badge tone={statusTone(auth.account?.identityStatus ?? 'UNVERIFIED')}>{auth.account?.identityStatus ?? 'UNAVAILABLE'}</Badge></dd></div>
            <div><dt>{t('dashboard.accountId')}</dt><dd className="reference">{auth.account?.id ?? '—'}</dd></div>
          </dl>
          {verifications.isError ? <Alert tone="danger">{verifications.error.message}</Alert> : null}
          {mutation.isSuccess ? <Alert tone="success">{t('identity.started')}</Alert> : null}
          {mutation.isError ? <Alert tone="danger">{mutation.error.message}</Alert> : null}
          <Alert tone="warning">{t('identity.localProvider')}</Alert>
        </Card>

        <Card title={t('identity.start')} className="feature-card">
          <form className="feature-form" onSubmit={submit}>
            <FormField id="identity-purpose" label={t('identity.purpose')}>
              <input id="identity-purpose" value={purpose} onChange={(event) => setPurpose(event.target.value)} required />
            </FormField>
            <FormField id="identity-policy-version" label={t('identity.policyVersion')}>
              <input id="identity-policy-version" value={policyVersion} onChange={(event) => setPolicyVersion(event.target.value)} required />
            </FormField>
            <label className="check-field">
              <input type="checkbox" checked={consented} onChange={(event) => setConsented(event.target.checked)} />
              <span>{t('identity.consent')}</span>
            </label>
            {!consented ? <p className="field-hint">{t('identity.consentRequired')}</p> : null}
            <Button type="submit" disabled={mutation.isPending || !consented || !purpose.trim() || !policyVersion.trim()}>
              {mutation.isPending ? t('identity.starting') : t('identity.start')}
            </Button>
          </form>
        </Card>

        <Card title={t('identity.history')} className="feature-card feature-card--wide">
          {verifications.isLoading ? <p role="status">{t('common.loading')}…</p> : null}
          {verifications.isError ? <Alert tone="danger">{verifications.error.message}</Alert> : null}
          {verifications.isSuccess && verifications.data.length === 0 ? <p>{t('identity.noHistory')}</p> : null}
          {verifications.isSuccess && verifications.data.length > 0 ? (
            <div className="table-wrap">
              <table>
                <thead><tr><th>{t('identity.provider')}</th><th>{t('identity.verificationStatus')}</th><th>{t('dashboard.identityStatus')}</th><th>{t('identity.assurance')}</th><th>{t('identity.initiated')}</th></tr></thead>
                <tbody>
                  {verifications.data.map((verification) => (
                    <tr key={verification.id}>
                      <td>{verification.provider}</td>
                      <td><Badge tone={statusTone(verification.status)}>{verification.status}</Badge></td>
                      <td>{verification.identityStatus}</td>
                      <td>{verification.assuranceLevel ?? '—'}</td>
                      <td>{new Date(verification.initiatedAt).toLocaleString(locale === 'am' ? 'am-ET' : 'en')}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </Card>
      </div>
    </div>
  )
}