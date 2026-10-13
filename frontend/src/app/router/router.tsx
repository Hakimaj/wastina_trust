import { createBrowserRouter, Navigate } from 'react-router-dom'
import { AuthSessionBoundary } from './AuthSessionBoundary'
import { AuthenticatedLayout } from '../../layouts/AuthenticatedLayout'
import { PublicLayout } from '../../layouts/PublicLayout'
import { DashboardPage } from '../../features/dashboard/DashboardPage'
import { ProfilePage } from '../../features/profile/pages/ProfilePage'
import { IdentityPage } from '../../features/identity/pages/IdentityPage'
import { EmploymentPage } from '../../features/employment/pages/EmploymentPage'
import { OrganizationsPage } from '../../features/organizations/pages/OrganizationsPage'
import { TrustPage } from '../../features/trust/pages/TrustPage'
import {
  VerificationDetailPage,
  VerificationsPage,
} from '../../features/verifications/pages'
import {
  AuthCallbackPage,
  LoginPage,
  MembershipGuard,
  ProtectedRoute,
  ProvisioningPage,
  ProvisioningRoute,
  PublicRoute,
} from '../../features/auth'
import {
  ErrorPage,
  NotFoundPage,
  SessionExpiredPage,
  UnauthorizedPage,
} from '../../features/system/pages'

export const router = createBrowserRouter([
  {
    element: <AuthSessionBoundary />,
    children: [
      {
        element: <PublicLayout />,
        children: [
          {
            element: <PublicRoute />,
            children: [
              { path: '/', element: <Navigate to="/app" replace /> },
              { path: '/login', element: <LoginPage /> },
              { path: '/register', element: <LoginPage /> },
              { path: '/auth/callback', element: <AuthCallbackPage /> },
              { path: '/unauthorized', element: <UnauthorizedPage /> },
              { path: '/session-expired', element: <SessionExpiredPage /> },
            ],
          },
          {
            element: <ProvisioningRoute />,
            children: [{ path: '/onboarding', element: <ProvisioningPage /> }],
          },
        ],
      },
      {
        element: <ProtectedRoute />,
        children: [
          {
            element: <AuthenticatedLayout />,
            errorElement: <ErrorPage />,
            children: [
              { path: '/app', element: <DashboardPage /> },
              { path: '/app/profile', element: <ProfilePage /> },
              { path: '/app/identity', element: <IdentityPage /> },
              { path: '/app/employment', element: <EmploymentPage /> },
              { path: '/app/organizations', element: <OrganizationsPage /> },
              { path: '/app/trust', element: <TrustPage /> },
              {
                // Route guard is mounted here in addition to the backend checks: the
                // verification workspace is only meaningful for a membership that can
                // verify employment.
                element: <MembershipGuard />,
                children: [
                  { path: '/app/verifications', element: <VerificationsPage /> },
                  {
                    path: '/app/verifications/:requestId',
                    element: <VerificationDetailPage />,
                  },
                ],
              },
            ],
          },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])