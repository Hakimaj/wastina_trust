import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router-dom'
import { AuthProvider } from '../features/auth'
import { I18nProvider } from '../i18n/I18nProvider'
import { ToastProvider } from '../components/feedback/ToastProvider'
import { queryClient } from './providers/queryClient'
import { router } from './router/router'

export function Application() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <I18nProvider>
          <ToastProvider>
            <RouterProvider router={router} />
          </ToastProvider>
        </I18nProvider>
      </AuthProvider>
    </QueryClientProvider>
  )
}
