import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router-dom'
import { queryClient } from './providers/queryClient'
import { I18nProvider } from '../i18n/I18nProvider'
import { ToastProvider } from '../components/feedback/ToastProvider'
import { router } from './router/router'

export function Application() {
  return (
    <QueryClientProvider client={queryClient}>
      <I18nProvider>
        <ToastProvider>
          <RouterProvider router={router} />
        </ToastProvider>
      </I18nProvider>
    </QueryClientProvider>
  )
}
