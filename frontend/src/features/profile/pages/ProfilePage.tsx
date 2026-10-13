import { useEffect, useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Alert, Badge, Button, Card, CellText } from '../../../components/primitives'
import { FormField } from '../../../components/forms/FormField'
import { queryKeys } from '../../../api/queryKeys'
import { useAuth } from '../../auth/AuthContext'
import { useI18n } from '../../../i18n/useI18n'
import {
  employerProfileSchema,
  guarantorProfileSchema,
  workerProfileSchema,
  type EmployerProfileValues,
  type GuarantorProfileValues,
  type WorkerProfileValues,
} from '../validation'
import {
  getEmployerProfile,
  getGuarantorProfile,
  getWorkerProfile,
  saveEmployerProfile,
  saveGuarantorProfile,
  saveWorkerProfile,
  type EmployerProfile,
  type GuarantorProfile,
  type WorkerProfile,
} from '../api'

function WorkerProfilePanel({ userId }: { userId: string }) {
  const { t } = useI18n()
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState(false)
  const queryKey = queryKeys.profiles.worker(userId)
  const query = useQuery({ queryKey, queryFn: () => getWorkerProfile(userId) })
  const form = useForm<WorkerProfileValues>({
    resolver: zodResolver(workerProfileSchema),
    defaultValues: { professionName: '', locationName: '', isAvailable: true },
  })
  const mutation = useMutation({
    mutationFn: (values: WorkerProfileValues) => saveWorkerProfile(userId, values),
    onSuccess: async () => {
      setEditing(false)
      await queryClient.invalidateQueries({ queryKey })
    },
  })

  useEffect(() => {
    if (query.data) {
      form.reset({
        professionName: query.data.profession ?? '',
        locationName: query.data.location ?? '',
        isAvailable: query.data.available ?? query.data.isAvailable ?? true,
      })
    }
  }, [form, query.data])

  if (query.isLoading) return <Card title={t('profile.worker')}>
    <p role="status">{t('common.loading')}…</p>
  </Card>
  if (query.isError) return <Card title={t('profile.worker')}>
    <Alert tone="danger">{query.error.message}</Alert>
    <Button type="button" variant="secondary" onClick={() => void query.refetch()}>{t('common.retry')}</Button>
  </Card>

  const profile: WorkerProfile | null = query.data ?? null
  return (
    <Card title={t('profile.worker')} className="feature-card">
      {profile && !editing ? (
        <>
          <dl className="data-list">
            <div><dt>{t('profile.profession')}</dt><dd><CellText value={profile.profession} /></dd></div>
            <div><dt>{t('profile.location')}</dt><dd><CellText value={profile.location} /></dd></div>
            <div>
              <dt>{t('profile.availability')}</dt>
              <dd>
                <Badge tone={(profile.available ?? profile.isAvailable) ? 'success' : 'neutral'}>
                  {(profile.available ?? profile.isAvailable) ? t('status.active') : t('common.unavailable')}
                </Badge>
              </dd>
            </div>
            <div>
              <dt>{t('common.status')}</dt>
              <dd>{profile.verificationBadge ? t('status.verified') : t('status.pending')}</dd>
            </div>
          </dl>
          <Button type="button" variant="secondary" onClick={() => setEditing(true)}>{t('profile.edit')}</Button>
        </>
      ) : (
        <>
          {!profile ? <p>{t('profile.notCreated')}</p> : null}
          <form className="feature-form" onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
            <FormField id="worker-profession" label={t('profile.profession')} error={form.formState.errors.professionName?.message}>
              <input id="worker-profession" autoComplete="organization-title" {...form.register('professionName')} />
            </FormField>
            <FormField id="worker-location" label={t('profile.location')} error={form.formState.errors.locationName?.message}>
              <input id="worker-location" autoComplete="address-level2" {...form.register('locationName')} />
            </FormField>
            <label className="check-field"><input type="checkbox" {...form.register('isAvailable')} />{t('profile.availability')}</label>
            {mutation.isError ? <Alert tone="danger">{mutation.error.message}</Alert> : null}
            {mutation.isSuccess ? <Alert tone="success">{t('profile.saved')}</Alert> : null}
            <div className="form-actions">
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? t('common.loading') : profile ? t('profile.save') : t('profile.create')}
              </Button>
              {profile ? <Button type="button" variant="ghost" onClick={() => setEditing(false)}>{t('common.cancel')}</Button> : null}
            </div>
          </form>
        </>
      )}
    </Card>
  )
}

function EmployerProfilePanel({ userId }: { userId: string }) {
  const { t } = useI18n()
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState(false)
  const queryKey = queryKeys.profiles.employer(userId)
  const query = useQuery({ queryKey, queryFn: () => getEmployerProfile(userId) })
  const form = useForm<EmployerProfileValues>({
    resolver: zodResolver(employerProfileSchema),
    defaultValues: { companyName: '', locationName: '', subscriptionTier: '' },
  })
  const mutation = useMutation({
    mutationFn: (values: EmployerProfileValues) => saveEmployerProfile(userId, values),
    onSuccess: async () => {
      setEditing(false)
      await queryClient.invalidateQueries({ queryKey })
    },
  })

  useEffect(() => {
    if (query.data) form.reset({
      companyName: query.data.companyName ?? '',
      locationName: query.data.location ?? '',
      subscriptionTier: query.data.subscriptionTier ?? '',
    })
  }, [form, query.data])

  if (query.isLoading) return <Card title={t('profile.employer')}>
    <p role="status">{t('common.loading')}…</p>
  </Card>
  if (query.isError) return <Card title={t('profile.employer')}>
    <Alert tone="danger">{query.error.message}</Alert>
    <Button type="button" variant="secondary" onClick={() => void query.refetch()}>{t('common.retry')}</Button>
  </Card>

  const profile: EmployerProfile | null = query.data ?? null
  return (
    <Card title={t('profile.employer')} className="feature-card">
      {profile && !editing ? (
        <>
          <dl className="data-list">
            <div><dt>{t('profile.companyName')}</dt><dd><CellText value={profile.companyName} /></dd></div>
            <div><dt>{t('profile.location')}</dt><dd><CellText value={profile.location} /></dd></div>
            <div><dt>{t('profile.subscriptionTier')}</dt><dd><CellText value={profile.subscriptionTier} /></dd></div>
            <div><dt>{t('dashboard.employmentCount')}</dt><dd><CellText value={profile.totalHires} /></dd></div>
          </dl>
          <Button type="button" variant="secondary" onClick={() => setEditing(true)}>{t('profile.edit')}</Button>
        </>
      ) : (
        <>
          {!profile ? <p>{t('profile.notCreated')}</p> : null}
          <form className="feature-form" onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
            <FormField id="employer-company" label={t('profile.companyName')} error={form.formState.errors.companyName?.message}>
              <input id="employer-company" autoComplete="organization" {...form.register('companyName')} />
            </FormField>
            <FormField id="employer-location" label={t('profile.location')} error={form.formState.errors.locationName?.message}>
              <input id="employer-location" autoComplete="address-level2" {...form.register('locationName')} />
            </FormField>
            <FormField id="employer-tier" label={t('profile.subscriptionTier')} error={form.formState.errors.subscriptionTier?.message}>
              <select id="employer-tier" {...form.register('subscriptionTier')}>
                <option value="">{t('common.select')}</option>
                <option value="FREE">FREE</option>
                <option value="PREMIUM">PREMIUM</option>
              </select>
            </FormField>
            {mutation.isError ? <Alert tone="danger">{mutation.error.message}</Alert> : null}
            {mutation.isSuccess ? <Alert tone="success">{t('profile.saved')}</Alert> : null}
            <div className="form-actions">
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? t('common.loading') : profile ? t('profile.save') : t('profile.create')}
              </Button>
              {profile ? <Button type="button" variant="ghost" onClick={() => setEditing(false)}>{t('common.cancel')}</Button> : null}
            </div>
          </form>
        </>
      )}
    </Card>
  )
}

function GuarantorProfilePanel({ userId }: { userId: string }) {
  const { t } = useI18n()
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState(false)
  const queryKey = queryKeys.profiles.guarantor(userId)
  const query = useQuery({ queryKey, queryFn: () => getGuarantorProfile(userId) })
  const form = useForm<GuarantorProfileValues>({
    resolver: zodResolver(guarantorProfileSchema),
    defaultValues: { financialLimit: null, employerName: '' },
  })
  const mutation = useMutation({
    mutationFn: (values: GuarantorProfileValues) => saveGuarantorProfile(userId, values),
    onSuccess: async () => {
      setEditing(false)
      await queryClient.invalidateQueries({ queryKey })
    },
  })

  useEffect(() => {
    if (query.data) form.reset({
      financialLimit: query.data.financialLimit,
      employerName: query.data.employerName ?? '',
    })
  }, [form, query.data])

  if (query.isLoading) return <Card title={t('profile.guarantor')}>
    <p role="status">{t('common.loading')}…</p>
  </Card>
  if (query.isError) return <Card title={t('profile.guarantor')}>
    <Alert tone="danger">{query.error.message}</Alert>
    <Button type="button" variant="secondary" onClick={() => void query.refetch()}>{t('common.retry')}</Button>
  </Card>

  const profile: GuarantorProfile | null = query.data ?? null
  return (
    <Card title={t('profile.guarantor')} className="feature-card">
      {profile && !editing ? (
        <>
          <dl className="data-list">
            <div><dt>{t('profile.financialLimit')}</dt><dd><CellText value={profile.financialLimit} /></dd></div>
            <div><dt>{t('profile.employerName')}</dt><dd><CellText value={profile.employerName} /></dd></div>
            <div><dt>{t('profile.kycStatus')}</dt><dd><CellText value={profile.kycStatus} /></dd></div>
          </dl>
          <Button type="button" variant="secondary" onClick={() => setEditing(true)}>{t('profile.edit')}</Button>
        </>
      ) : (
        <>
          {!profile ? <p>{t('profile.notCreated')}</p> : null}
          <form className="feature-form" onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
            <FormField id="guarantor-limit" label={t('profile.financialLimit')} error={form.formState.errors.financialLimit?.message}>
              <input id="guarantor-limit" type="number" min="0" step="0.01" {...form.register('financialLimit', { setValueAs: (value) => value === '' ? null : Number(value) })} />
            </FormField>
            <FormField id="guarantor-employer" label={t('profile.employerName')} error={form.formState.errors.employerName?.message}>
              <input id="guarantor-employer" autoComplete="organization" {...form.register('employerName')} />
            </FormField>
            {mutation.isError ? <Alert tone="danger">{mutation.error.message}</Alert> : null}
            {mutation.isSuccess ? <Alert tone="success">{t('profile.saved')}</Alert> : null}
            <div className="form-actions">
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? t('common.loading') : profile ? t('profile.save') : t('profile.create')}
              </Button>
              {profile ? <Button type="button" variant="ghost" onClick={() => setEditing(false)}>{t('common.cancel')}</Button> : null}
            </div>
          </form>
        </>
      )}
    </Card>
  )
}

export function ProfilePage() {
  const auth = useAuth()
  const { t } = useI18n()
  const accountId = auth.account?.id
  return (
    <div className="workspace-page">
      <header className="page-heading">
        <p className="eyebrow">{t('common.profile')}</p>
        <h1>{t('profile.title')}</h1>
        <p>{t('profile.intro')}</p>
      </header>
      {!accountId ? <Alert tone="danger">{auth.error ?? t('dashboard.loadError')}</Alert> : (
        <div className="feature-grid">
          {auth.roles.includes('ROLE_WORKER') ? <WorkerProfilePanel userId={accountId} /> : null}
          {auth.roles.includes('ROLE_EMPLOYER') ? <EmployerProfilePanel userId={accountId} /> : null}
          {auth.roles.includes('ROLE_GUARANTOR') ? <GuarantorProfilePanel userId={accountId} /> : null}
        </div>
      )}
    </div>
  )
}