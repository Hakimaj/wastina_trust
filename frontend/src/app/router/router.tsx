import { createBrowserRouter } from 'react-router-dom'
import { AuthenticatedLayout } from '../../layouts/AuthenticatedLayout'
import { PublicLayout } from '../../layouts/PublicLayout'
import { AppPlaceholder } from '../../layouts/AppPlaceholder'
import {
  AuthCallbackPage,
  LoginPage,
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
    element: <PublicLayout />,
    errorElement: <ErrorPage />,
    children: [
      {
        element: <PublicRoute />,
        children: [
          {
            path: '/',
            element: <AppPlaceholder title="Wastina Digital Trust Platform" />,
          },
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
    errorElement: <ErrorPage />,
    children: [
      {
        element: <AuthenticatedLayout />,
        children: [
          { path: '/app', element: <AppPlaceholder title="Dashboard" /> },
          { path: '/app/profile', element: <AppPlaceholder title="Profile" /> },
          { path: '/app/identity', element: <AppPlaceholder title="Identity" /> },
          { path: '/app/employment', element: <AppPlaceholder title="Employment" /> },
          { path: '/app/trust', element: <AppPlaceholder title="Trust" /> },
          {
            path: '/app/guarantees',
            element: <AppPlaceholder title="Guarantees" />,
          },
          { path: '/app/documents', element: <AppPlaceholder title="Documents" /> },
          { path: '/app/payments', element: <AppPlaceholder title="Payments" /> },
          { path: '/app/security', element: <AppPlaceholder title="Security" /> },
          { path: '/app/support', element: <AppPlaceholder title="Support" /> },
          { path: '/employer', element: <AppPlaceholder title="Employer workspace" /> },
          { path: '/guarantor', element: <AppPlaceholder title="Guarantor workspace" /> },
          { path: '/landlord', element: <AppPlaceholder title="Landlord workspace" /> },
          { path: '/admin', element: <AppPlaceholder title="Admin workspace" /> },
          { path: '/compliance', element: <AppPlaceholder title="Compliance workspace" /> },
          { path: '/finance', element: <AppPlaceholder title="Finance workspace" /> },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])
