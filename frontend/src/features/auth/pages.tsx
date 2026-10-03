import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { AppPlaceholder } from '../../layouts/AppPlaceholder'
import { useAuth } from './AuthContext'
import { consumeReturnPath } from './redirects'

const provisioningSchema = z.object({
  email: z.string().email('Enter a valid email address.'),
  phoneNumber: z.string().min(1, 'Phone number is required.'),
})
type ProvisioningValues = z.infer<typeof provisioningSchema>

export function LoginPage() {
  const auth = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  if (auth.status === 'authenticated') return <Navigate to="/app" replace />
  if (auth.status === 'provisioning')
    return <Navigate to="/onboarding" replace />

  return (
    <section className="system-page">
      <p className="eyebrow">Wastina authentication</p>
      <h1>Sign in to Wastina</h1>
      <p>
        Continue to the secure identity provider to access your Wastina account.
      </p>
      {error ? <p role="alert">{error}</p> : null}
      <button
        type="button"
        onClick={() =>
          void auth
            .login(`${location.pathname}${location.search}`)
            .catch(() => setError('Sign-in could not be started.'))
        }
      >
        Continue with Wastina
      </button>
      <button
        type="button"
        className="link-button"
        onClick={() =>
          void auth
            .register('/app')
            .catch(() => setError('Registration could not be started.'))
        }
      >
        Create an account
      </button>
      <button
        type="button"
        className="link-button"
        onClick={() => navigate('/')}
      >
        Return home
      </button>
    </section>
  )
}

export function AuthCallbackPage() {
  const auth = useAuth()
  const [destination] = useState(consumeReturnPath)
  if (auth.status === 'initializing')
    return <AppPlaceholder title="Completing secure sign-in..." />
  if (auth.status === 'authenticated')
    return <Navigate to={destination} replace />
  if (auth.status === 'provisioning')
    return <Navigate to="/onboarding" replace />
  if (auth.status === 'session-expired')
    return <Navigate to="/session-expired" replace />
  return (
    <AppPlaceholder title={auth.error ?? 'Sign-in could not be completed'} />
  )
}

export function ProvisioningPage() {
  const auth = useAuth()
  const form = useForm<ProvisioningValues>({
    resolver: zodResolver(provisioningSchema),
    defaultValues: { email: auth.identity?.email ?? '', phoneNumber: '' },
  })

  if (auth.status === 'authenticated') return <Navigate to="/app" replace />

  return (
    <section className="system-page">
      <p className="eyebrow">Account setup</p>
      <h1>Prepare your Wastina account</h1>
      <p>
        Your identity provider is connected. Wastina needs these account fields
        before access can begin.
      </p>
      {auth.error ? <p role="alert">{auth.error}</p> : null}
      <form
        onSubmit={form.handleSubmit((values) =>
          auth.provisionAccount(values.email, values.phoneNumber),
        )}
        noValidate
      >
        <label htmlFor="provision-email">Email</label>
        <input
          id="provision-email"
          type="email"
          {...form.register('email')}
          aria-invalid={Boolean(form.formState.errors.email)}
        />
        {form.formState.errors.email ? (
          <p role="alert">{form.formState.errors.email.message}</p>
        ) : null}
        <label htmlFor="provision-phone">Phone number</label>
        <input
          id="provision-phone"
          type="tel"
          {...form.register('phoneNumber')}
          aria-invalid={Boolean(form.formState.errors.phoneNumber)}
        />
        {form.formState.errors.phoneNumber ? (
          <p role="alert">{form.formState.errors.phoneNumber.message}</p>
        ) : null}
        <button type="submit" disabled={form.formState.isSubmitting}>
          {form.formState.isSubmitting ? 'Preparing account...' : 'Continue'}
        </button>
      </form>
    </section>
  )
}

export function AccountStatePage() {
  const auth = useAuth()
  return (
    <AppPlaceholder title={auth.error ?? 'Account access is unavailable'} />
  )
}
