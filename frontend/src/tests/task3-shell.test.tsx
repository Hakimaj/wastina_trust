import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AppShell } from '../components/layout/AppShell'
import { I18nProvider } from '../i18n/I18nProvider'
import { AuthContext } from '../features/auth/AuthContext'
import type { AuthContextValue } from '../features/auth/AuthContext'
import { getVisibleNavigationItems } from '../components/navigation/navigationConfig'

const authValue: AuthContextValue = {
  status: 'authenticated',
  identity: {
    subject: 'user-1',
    email: 'user@example.com',
    name: 'Jane Example',
    preferredUsername: 'jane',
    claims: {},
  },
  account: {
    id: 'account-1',
    accountStatus: 'ACTIVE',
    identityStatus: 'VERIFIED',
    roles: ['ROLE_WORKER', 'ROLE_EMPLOYER'],
    email: 'user@example.com',
    phoneNumber: null,
    identitySubject: 'subject-1',
    verifiedAt: null,
    createdAt: null,
    updatedAt: null,
    enabled: true,
  },
  roles: ['ROLE_WORKER', 'ROLE_EMPLOYER'],
  organizations: [],
  activeRole: 'ROLE_WORKER',
  activeOrganization: null,
  scopes: ['openid'],
  error: null,
  login: async () => {},
  register: async () => {},
  logout: async () => {},
  refreshSession: async () => {},
  provisionAccount: async () => {},
  setActiveRole: () => {},
  setActiveOrganization: () => {},
  hasRole: () => true,
  hasAnyRole: () => true,
  hasAllRoles: () => true,
  hasPermission: () => true,
  hasAnyPermission: () => true,
}

describe('Task 3 shell foundation', () => {
  afterEach(cleanup)

  beforeEach(() => {
    window.localStorage.clear()
    document.documentElement.lang = ''
  })

  it('shows only navigation entries backed by registered routes', () => {
    // `trust` is read-only and available to every account; the verification
    // inbox requires a membership that can verify employment, which this
    // account does not have.
    const employerNavigation = getVisibleNavigationItems(['ROLE_EMPLOYER'])
    expect(employerNavigation.map((item) => item.id)).toEqual([
      'dashboard', 'profile', 'identity', 'employment', 'organizations', 'trust',
    ])

    const verifierNavigation = getVisibleNavigationItems(
      ['ROLE_EMPLOYER'],
      [
        {
          id: 'm1',
          organizationId: 'org-1',
          name: 'Employer Org',
          legalName: null,
          organizationStatus: 'VERIFIED',
          role: 'VERIFIER',
          status: 'ACTIVE',
          joinedAt: null,
          endedAt: null,
          canVerifyEmployment: true,
          canManageMemberships: false,
        },
      ],
    )
    expect(verifierNavigation.map((item) => item.id)).toContain('verifications')
  })

  it('renders the app shell with the shared Wastina header and nav', () => {
    render(
      <MemoryRouter initialEntries={['/app']}>
        <AuthContext.Provider value={authValue}>
          <I18nProvider>
            <AppShell>
              <div>Shell content</div>
            </AppShell>
          </I18nProvider>
        </AuthContext.Provider>
      </MemoryRouter>,
    )

    expect(screen.getByRole('banner')).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: /primary navigation/i })).toBeInTheDocument()
    // The brand mark "W" is hidden from screen readers; the app name is in the brand text
    expect(screen.getByText('Wastina')).toBeInTheDocument()
  })

  it('updates the html language when the locale changes', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <AuthContext.Provider value={authValue}>
          <I18nProvider>
            <AppShell>
              <div>Shell content</div>
            </AppShell>
          </I18nProvider>
        </AuthContext.Provider>
      </MemoryRouter>,
    )

    expect(document.documentElement.lang).toBe('en')
    await user.selectOptions(screen.getByRole('combobox', { name: 'Language' }), 'am')
    await waitFor(() => expect(document.documentElement.lang).toBe('am'))
    expect(screen.getByRole('link', { name: 'ዳሽቦርድ' })).toBeInTheDocument()
  })

  it('provides skip navigation and mobile menu state', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/app']}>
        <AuthContext.Provider value={authValue}>
          <I18nProvider><AppShell><div>Shell content</div></AppShell></I18nProvider>
        </AuthContext.Provider>
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'Skip to main content' })).toHaveAttribute('href', '#main-content')
    const menuButton = screen.getByRole('button', { name: 'Open navigation menu' })
    await user.click(menuButton)
    expect(menuButton).toHaveAttribute('aria-expanded', 'true')
    expect(screen.getByRole('navigation', { name: 'Primary navigation' })).toHaveAttribute('id', 'primary-navigation')
  })
})
