import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { Alert } from '../../components/primitives'
import { useDevelopmentAuth } from './DevelopmentAuthProvider'
import { useAuth } from './AuthContext'
import { consumeReturnPath } from './redirects'
import { config } from '../../app/config'

const provisioningSchema = z.object({
  email: z.string().email('Enter a valid email address.'),
  phoneNumber: z.string().trim().min(1, 'Phone number is required.'),
})
type ProvisioningValues = z.infer<typeof provisioningSchema>

/** Local JWT mode: pick one of the personas the backend has seeded. */
function DevelopmentLogin() {
  const auth = useAuth()
  const { personas, login, pendingPersona } = useDevelopmentAuth()
  const location = useLocation()
  const [error, setError] = useState<string | null>(null)

  if (auth.status === 'authenticated') return <Navigate to="/app" replace />
  if (auth.status === 'provisioning') return <Navigate to="/onboarding" replace />

  async function start(persona: string) {
    setError(null)
    await login(persona, `${location.pathname}${location.search}`)
    if (auth.error) setError(auth.error)
  }

  return (
    <section className="system-page">
      <p className="eyebrow">{config.appName}</p>
      <h1>Choose a development account</h1>
      <p>
        The backend is running in local JWT mode, so no identity provider is needed. Pick one of the
        seeded personas to sign in as. The access token is issued by the backend and held in memory
        only.
      </p>

      {error ? (
        <Alert tone="danger" title="Sign-in failed">
          {error}
        </Alert>
      ) : null}

      <ul className="persona-list">
        {personas.map((persona) => (
          <li key={persona.persona}>
            <button
              type="button"
              className="persona-button"
              disabled={pendingPersona !== null}
              onClick={() => void start(persona.persona)}
            >
              <span className="persona-button__name">{persona.persona}</span>
              <span className="persona-button__meta">{persona.role}</span>
              <span className="persona-button__meta">{persona.email}</span>
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}

/** Keycloak / OIDC mode: hand off to the authorization server. */
function OidcLogin() {
  const auth = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  if (auth.status === 'authenticated') return <Navigate to="/app" replace />
  if (auth.status === 'provisioning') return <Navigate to="/onboarding" replace />

  return (
    <section className="system-page">
      <p className="eyebrow">{config.appName}</p>
      <h1>Sign in to Wastina</h1>
      <p>Continue to the secure identity provider to access your Wastina account.</p>
      {error ? <Alert tone="danger">{error}</Alert> : null}
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
      <button type="button" className="link-button" onClick={() => navigate('/')}>
        Return home
      </button>
    </section>
  )
}

export function LoginPage() {
  return config.authMode === 'dev-jwt' ? <DevelopmentLogin /> : <OidcLogin />
}

export function AuthCallbackPage() {
  const auth = useAuth()
  const destination = consumeReturnPath()

  if (auth.status === 'initializing') {
    return (
      <section className="system-page" role="status">
        <h1>Completing secure sign-in…</h1>
      </section>
    )
  }
  if (auth.status === 'authenticated') return <Navigate to={destination} replace />
  if (auth.status === 'provisioning') return <Navigate to="/onboarding" replace />
  if (auth.status === 'session-expired') return <Navigate to="/session-expired" replace />

  return (
    <section className="system-page" role="alert">
      <h1>Sign-in could not be completed</h1>
      <p>{auth.error ?? 'The identity provider did not return a usable session.'}</p>
    </section>
  )
}

export function ProvisioningPage() {
  const auth = useAuth()
  const form = useForm<ProvisioningValues>({
    resolver: zodResolver(provisioningSchema),
    defaultValues: { email: auth.identity?.email ?? '', phoneNumber: '' },
  })

  if (auth.status === 'authenticated') return <Navigate to="/app" replace />

  const submit = form.handleSubmit(async (values) => {
    try {
      await auth.provisionAccount(values.email, values.phoneNumber)
    } catch {
      // The provider has already recorded a message; nothing further to do here.
    }
  })

  return (
    <section className="system-page">
      <p className="eyebrow">Account setup</p>
      <h1>Prepare your Wastina account</h1>
      <p>
        You are authenticated, but Wastina needs these contact details before you can use the
        platform.
      </p>

      {auth.error ? <Alert tone="danger">{auth.error}</Alert> : null}

      <form className="feature-form" onSubmit={submit} noValidate>
        <div>
          <label htmlFor="provision-email">Email address</label>
          <input
            id="provision-email"
            type="email"
            autoComplete="email"
            {...form.register('email')}
            aria-invalid={Boolean(form.formState.errors.email)}
            aria-describedby={
              form.formState.errors.email ? 'provision-email-error' : undefined
            }
          />
          {form.formState.errors.email ? (
            <p id="provision-email-error" role="alert">
              {form.formState.errors.email.message}
            </p>
          ) : null}
        </div>

        <div>
          <label htmlFor="provision-phone">Phone number</label>
          <input
            id="provision-phone"
            type="tel"
            autoComplete="tel"
            placeholder="+251900000000"
            {...form.register('phoneNumber')}
            aria-invalid={Boolean(form.formState.errors.phoneNumber)}
            aria-describedby={
              form.formState.errors.phoneNumber ? 'provision-phone-error' : undefined
            }
          />
          {form.formState.errors.phoneNumber ? (
            <p id="provision-phone-error" role="alert">
              {form.formState.errors.phoneNumber.message}
            </p>
          ) : null}
        </div>

        <div className="form-actions">
          <button type="submit" disabled={form.formState.isSubmitting}>
            {form.formState.isSubmitting ? 'Preparing account…' : 'Continue'}
          </button>
        </div>
      </form>
    </section>
  )
}