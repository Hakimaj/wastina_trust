import { useEffect, useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient, type UseQueryResult } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Alert, Badge, Button, Card } from '../../../components/primitives'
import { FormField } from '../../../components/forms/FormField'
import { queryKeys } from '../../../api/queryKeys'
import { useAuth } from '../../auth/AuthContext'
import type { OrganizationMembership } from '../../auth/authTypes'
import { useI18n } from '../../../i18n/useI18n'
import { employmentCreateSchema, type EmploymentCreateValues } from '../../profile/validation'
import {
  beginEmploymentReview,
  createEmployment,
  getEmploymentHistory,
  getOrganizationVerificationRequests,
  getWorkerVerificationRequests,
  requestEmploymentVerification,
  verifyEmployment,
  type EmploymentRecord,
  type EmploymentVerificationRequest,
} from '../api'

function statusTone(value: string): 'neutral' | 'success' | 'warning' | 'danger' {
  if (['ACTIVE', 'VERIFIED'].includes(value)) return 'success'
  if (['PENDING', 'CREATED', 'IN_REVIEW', 'SELF_REPORTED', 'VERIFICATION_REQUESTED'].includes(value)) return 'warning'
  if (['REJECTED', 'DISPUTED', 'EXPIRED', 'REVOKED'].includes(value)) return 'danger'
  return 'neutral'
}

function EmploymentCreateForm({
  organization,
  onCancel,
  onCreate,
  pending,
  error,
}: {
  organization: OrganizationMembership
  onCancel: () => void
  onCreate: (values: EmploymentCreateValues) => void
  pending: boolean
  error: string | null
}) {
  const { t } = useI18n()
  const form = useForm<EmploymentCreateValues>({
    resolver: zodResolver(employmentCreateSchema),
    defaultValues: {
      organizationId: organization.organizationId,
      jobTitleEn: '',
      jobTitleAm: '',
      occupationCode: '',
      department: '',
      location: '',
      employmentType: 'FULL_TIME',
      workRelationshipType: 'EMPLOYEE',
      sector: 'PRIVATE',
      startDate: '',
      endDate: '',
      currentEmployment: true,
      description: '',
    },
  })

  useEffect(() => {
    form.setValue('organizationId', organization.organizationId)
  }, [form, organization.organizationId])

  return (
    <form className="feature-form employment-form" onSubmit={form.handleSubmit(onCreate)}>
      <FormField id="employment-organization" label={t('employment.organization')} error={form.formState.errors.organizationId?.message}>
        <select id="employment-organization" value={organization.organizationId} disabled>
          <option value={organization.organizationId}>{organization.name ?? organization.organizationId}</option>
        </select>
      </FormField>
      <FormField id="employment-job-title-en" label={t('employment.jobTitleEn')} error={form.formState.errors.jobTitleEn?.message}>
        <input id="employment-job-title-en" {...form.register('jobTitleEn')} required />
      </FormField>
      <FormField id="employment-job-title-am" label={t('employment.jobTitleAm')} error={form.formState.errors.jobTitleAm?.message}>
        <input id="employment-job-title-am" {...form.register('jobTitleAm')} />
      </FormField>
      <FormField id="employment-department" label={t('employment.department')}>
        <input id="employment-department" {...form.register('department')} />
      </FormField>
      <FormField id="employment-location" label={t('employment.location')}>
        <input id="employment-location" {...form.register('location')} />
      </FormField>
      <FormField id="employment-type" label={t('employment.employmentType')} error={form.formState.errors.employmentType?.message}>
        <select id="employment-type" {...form.register('employmentType')}>
          {['FULL_TIME', 'PART_TIME', 'TEMPORARY', 'CONTRACT', 'CASUAL', 'INTERNSHIP', 'APPRENTICESHIP', 'CONSULTANT', 'SEASONAL', 'OTHER'].map((value) => <option key={value}>{value}</option>)}
        </select>
      </FormField>
      <FormField id="employment-relationship" label={t('employment.relationshipType')} error={form.formState.errors.workRelationshipType?.message}>
        <select id="employment-relationship" {...form.register('workRelationshipType')}>
          {['EMPLOYEE', 'CONTRACTOR', 'CONSULTANT', 'SELF_EMPLOYED', 'VOLUNTEER', 'INTERN', 'OTHER'].map((value) => <option key={value}>{value}</option>)}
        </select>
      </FormField>
      <FormField id="employment-sector" label={t('employment.sector')} error={form.formState.errors.sector?.message}>
        <select id="employment-sector" {...form.register('sector')}>
          {['PUBLIC', 'PRIVATE', 'NGO', 'OTHER'].map((value) => <option key={value}>{value}</option>)}
        </select>
      </FormField>
      <FormField id="employment-start-date" label={t('employment.startDate')} error={form.formState.errors.startDate?.message}>
        <input id="employment-start-date" type="date" {...form.register('startDate')} required />
      </FormField>
      <FormField id="employment-end-date" label={t('employment.endDate')} error={form.formState.errors.endDate?.message}>
        <input id="employment-end-date" type="date" disabled={form.watch('currentEmployment')} {...form.register('endDate')} />
      </FormField>
      <label className="check-field"><input type="checkbox" {...form.register('currentEmployment')} />{t('employment.current')}</label>
      <FormField id="employment-description" label={t('employment.description')}>
        <textarea id="employment-description" rows={3} {...form.register('description')} />
      </FormField>
      {error ? <Alert tone="danger">{error}</Alert> : null}
      <div className="form-actions">
        <Button type="submit" disabled={pending}>{pending ? t('common.loading') : t('employment.create')}</Button>
        <Button type="button" variant="ghost" onClick={onCancel}>{t('common.cancel')}</Button>
      </div>
    </form>
  )
}

function EmploymentTable({
  rows,
  onRequestVerification,
  pendingId,
  requestError,
}: {
  rows: EmploymentRecord[]
  onRequestVerification: (record: EmploymentRecord) => void
  pendingId: string | null
  requestError: string | null
}) {
  const { t } = useI18n()
  if (!rows.length) return <p>{t('employment.noRecords')}</p>
  return (
    <>
      {requestError ? <Alert tone="danger">{requestError}</Alert> : null}
      <div className="table-wrap">
        <table>
          <thead><tr><th>{t('employment.organization')}</th><th>{t('employment.jobTitleEn')}</th><th>{t('employment.startDate')}</th><th>{t('employment.endDate')}</th><th>{t('employment.recordStatus')}</th><th>{t('employment.verificationStatus')}</th><th>{t('common.actions')}</th></tr></thead>
          <tbody>
            {rows.map((record) => (
              <tr key={record.id}>
                <td>{record.organizationName}</td>
                <td>{record.jobTitleEn}{record.jobTitleAm ? <span className="table-subtitle">{record.jobTitleAm}</span> : null}</td>
                <td>{record.startDate}</td>
                <td>{record.currentEmployment ? '—' : record.endDate ?? '—'}</td>
                <td><Badge tone={statusTone(record.status)}>{record.status}</Badge></td>
                <td><Badge tone={statusTone(record.verificationStatus)}>{record.verificationStatus}</Badge></td>
                <td>
                  {record.verificationStatus === 'SELF_REPORTED' ? (
                    <Button type="button" variant="secondary" disabled={pendingId === record.id} onClick={() => onRequestVerification(record)}>
                      {pendingId === record.id ? t('common.loading') : t('employment.requestVerification')}
                    </Button>
                  ) : '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}

function VerificationRequestList({
  requests,
  onBeginReview,
  onVerify,
  pendingId,
  error,
  readOnly = false,
}: {
  requests: EmploymentVerificationRequest[]
  onBeginReview?: (request: EmploymentVerificationRequest) => void
  onVerify?: (request: EmploymentVerificationRequest) => void
  pendingId: string | null
  error: string | null
  readOnly?: boolean
}) {
  const { t } = useI18n()
  if (!requests.length) return <p>{t('employment.noRequests')}</p>
  return (
    <>
      {error ? <Alert tone="danger">{error}</Alert> : null}
      <div className="table-wrap">
        <table>
          <thead><tr><th>{t('employment.jobTitleEn')}</th><th>{t('employment.organization')}</th><th>{t('employment.verificationStatus')}</th><th>{t('common.actions')}</th></tr></thead>
          <tbody>
            {requests.map((request) => (
              <tr key={request.id}>
                <td>{request.jobTitleEn}</td><td>{request.organizationName}</td>
                <td><Badge tone={statusTone(request.status)}>{request.status}</Badge></td>
                <td>
                  {!readOnly && ['CREATED', 'SENT', 'DELIVERED', 'VIEWED'].includes(request.status) ? (
                    <Button type="button" variant="secondary" disabled={pendingId === request.id} onClick={() => onBeginReview?.(request)}>{t('employment.beginReview')}</Button>
                  ) : null}
                  {!readOnly && request.status === 'IN_REVIEW' ? (
                    <Button type="button" disabled={pendingId === request.id} onClick={() => onVerify?.(request)}>{t('employment.verify')}</Button>
                  ) : null}
                  {readOnly || !['CREATED', 'SENT', 'DELIVERED', 'VIEWED', 'IN_REVIEW'].includes(request.status) ? '—' : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}

export function EmploymentPage() {
  const auth = useAuth()
  const { t } = useI18n()
  const queryClient = useQueryClient()
  const [creating, setCreating] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [actionSuccess, setActionSuccess] = useState<string | null>(null)
  const [pendingId, setPendingId] = useState<string | null>(null)
  const userId = auth.account?.id ?? ''
  const workerEnabled = auth.roles.includes('ROLE_WORKER')
  const employerEnabled = auth.roles.includes('ROLE_EMPLOYER')
  const organizations = auth.organizations.filter((membership) => membership.status === 'ACTIVE')
  const currentOrganizationId = auth.activeOrganization?.organizationId ?? organizations[0]?.organizationId ?? ''

  const employment = useQuery({
    queryKey: queryKeys.employment.history(),
    queryFn: () => getEmploymentHistory(),
    enabled: Boolean(userId && workerEnabled),
  }) satisfies UseQueryResult<EmploymentRecord[]>
  const workerRequests = useQuery({
    queryKey: queryKeys.employment.verificationRequests.worker(),
    queryFn: () => getWorkerVerificationRequests(),
    enabled: Boolean(userId && workerEnabled),
  }) satisfies UseQueryResult<EmploymentVerificationRequest[]>
  const employerInbox = useQuery({
    queryKey: queryKeys.employment.verificationRequests.organization(currentOrganizationId),
    queryFn: () => getOrganizationVerificationRequests(currentOrganizationId),
    enabled: Boolean(userId && employerEnabled && currentOrganizationId),
  }) satisfies UseQueryResult<EmploymentVerificationRequest[]>

  const createMutation = useMutation({
    mutationFn: (values: EmploymentCreateValues) => createEmployment({
      ...values,
      endDate: values.currentEmployment ? null : values.endDate || null,
    }),
    onSuccess: async () => {
      setCreating(false)
      setActionError(null)
      setActionSuccess(t('employment.created'))
      await queryClient.invalidateQueries({ queryKey: queryKeys.employment.all })
    },
    onError: (error: Error) => { setActionError(error.message); setActionSuccess(null) },
  })

  const requestMutation = useMutation({
    mutationFn: (record: EmploymentRecord) => requestEmploymentVerification(record.id),
    onMutate: (record) => { setPendingId(record.id); setActionError(null); setActionSuccess(null) },
    onSuccess: async () => {
      setActionSuccess(t('employment.verificationRequested'))
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.employment.all }),
        queryClient.invalidateQueries({ queryKey: queryKeys.employment.verificationRequests.all }),
      ])
    },
    onError: (error: Error) => setActionError(error.message),
    onSettled: () => setPendingId(null),
  })

  const reviewMutation = useMutation({
    mutationFn: (request: EmploymentVerificationRequest) => beginEmploymentReview(request.organizationId, request.id),
    onMutate: (request) => { setPendingId(request.id); setActionError(null); setActionSuccess(null) },
    onSuccess: async () => {
      setActionSuccess(t('employment.reviewStarted'))
      await queryClient.invalidateQueries({ queryKey: queryKeys.employment.verificationRequests.all })
    },
    onError: (error: Error) => setActionError(error.message),
    onSettled: () => setPendingId(null),
  })

  const verifyMutation = useMutation({
    mutationFn: (request: EmploymentVerificationRequest) => verifyEmployment(request.organizationId, request.id),
    onMutate: (request) => { setPendingId(request.id); setActionError(null); setActionSuccess(null) },
    onSuccess: async () => {
      setActionSuccess(t('employment.verified'))
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.employment.verificationRequests.all }),
        queryClient.invalidateQueries({ queryKey: queryKeys.employment.all }),
      ])
    },
    onError: (error: Error) => setActionError(error.message),
    onSettled: () => setPendingId(null),
  })

  return (
    <div className="workspace-page">
      <header className="page-heading">
        <p className="eyebrow">{t('navigation.employment')}</p>
        <h1>{t('employment.title')}</h1>
        <p>{t('employment.intro')}</p>
      </header>

      {actionSuccess ? <Alert tone="success">{actionSuccess}</Alert> : null}
      {actionError ? <Alert tone="danger">{actionError}</Alert> : null}

      {workerEnabled ? (
        <Card title={t('employment.history')} className="feature-card">
          {employment.isLoading ? <p role="status">{t('common.loading')}…</p> : null}
          {employment.isError ? <Alert tone="danger">{employment.error.message}</Alert> : null}
          {employment.isSuccess ? <EmploymentTable rows={employment.data} onRequestVerification={(record) => requestMutation.mutate(record)} pendingId={pendingId} requestError={requestMutation.error?.message ?? null} /> : null}
          {organizations.length === 0 ? <Alert tone="warning">{t('employment.chooseOrganization')}</Alert> : null}
          {organizations.length > 0 ? <Button type="button" onClick={() => setCreating((open) => !open)}>{creating ? t('common.cancel') : t('employment.add')}</Button> : null}
          {creating && organizations.length > 0 ? (
            <EmploymentCreateForm
              organization={organizations.find((organization) => organization.organizationId === currentOrganizationId) ?? organizations[0]}
              onCancel={() => setCreating(false)}
              onCreate={(values) => createMutation.mutate(values)}
              pending={createMutation.isPending}
              error={createMutation.error?.message ?? null}
            />
          ) : null}
          {workerRequests.isError ? <Alert tone="danger">{workerRequests.error.message}</Alert> : null}
          {workerRequests.isSuccess ? <div className="stack-section"><h2>{t('employment.verificationRequests')}</h2><VerificationRequestList requests={workerRequests.data} pendingId={null} error={null} readOnly /></div> : null}
        </Card>
      ) : null}

      {employerEnabled ? (
        <Card title={t('employment.employerInbox')} className="feature-card">
          {organizations.length === 0 ? <p>{t('organizations.empty')}</p> : null}
          {employerInbox.isLoading && currentOrganizationId ? <p role="status">{t('common.loading')}…</p> : null}
          {employerInbox.isError ? <Alert tone="danger">{employerInbox.error.message}</Alert> : null}
          {employerInbox.isSuccess ? (
            <VerificationRequestList
              requests={employerInbox.data}
              onBeginReview={(request) => reviewMutation.mutate(request)}
              onVerify={(request) => verifyMutation.mutate(request)}
              pendingId={pendingId}
              error={reviewMutation.error?.message ?? verifyMutation.error?.message ?? null}
            />
          ) : null}
        </Card>
      ) : null}

      {!workerEnabled && !employerEnabled ? <Alert>{t('profile.unavailable')}</Alert> : null}
    </div>
  )
}