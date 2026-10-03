import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AuthContext } from '../features/auth/AuthContext'
import { ProtectedRoute, RoleGuard } from '../features/auth/guards'
import type { AuthContextValue } from '../features/auth/AuthContext'

function authValue(overrides: Partial<AuthContextValue> = {}): AuthContextValue {
  return {
    status: 'authenticated',
    identity: null,
    account: null,
    roles: ['ROLE_WORKER'],
    organizations: [],
    activeRole: 'ROLE_WORKER',
    activeOrganization: null,
    scopes: [],
    error: null,
    login: async () => undefined,
    register: async () => undefined,
    logout: async () => undefined,
    refreshSession: async () => undefined,
    provisionAccount: async () => undefined,
    setActiveRole: () => undefined,
    setActiveOrganization: () => undefined,
    hasRole: (role) => ['ROLE_WORKER'].includes(role),
    hasAnyRole: (roles) => roles.includes('ROLE_WORKER'),
    hasAllRoles: (roles) => roles.every((role) => role === 'ROLE_WORKER'),
    hasPermission: () => false,
    hasAnyPermission: () => false,
    ...overrides,
  }
}

function renderGuard(element: React.ReactNode, value: Partial<AuthContextValue> = {}) {
  return render(
    <AuthContext.Provider value={authValue(value)}>
      <MemoryRouter initialEntries={['/app']}>
        <Routes>
          <Route element={element}>
            <Route path="/app" element={<h1>Protected content</h1>} />
          </Route>
          <Route path="/login" element={<h1>Login route</h1>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('route authorization foundation', () => {
  it('redirects unauthenticated users to login', () => {
    renderGuard(<ProtectedRoute />, { status: 'unauthenticated' })
    expect(screen.getByRole('heading', { name: 'Login route' })).toBeInTheDocument()
  })

  it('preserves authenticated state for a forbidden role', () => {
    renderGuard(<RoleGuard allowedRoles={['ROLE_ADMIN']} />)
    expect(screen.getByRole('heading', { name: 'Access unavailable' })).toBeInTheDocument()
  })

  it('renders protected content for an allowed session', () => {
    renderGuard(<RoleGuard allowedRoles={['ROLE_WORKER']} />)
    expect(screen.getByRole('heading', { name: 'Protected content' })).toBeInTheDocument()
  })
})
