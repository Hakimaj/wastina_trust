import { Outlet } from 'react-router-dom'
import { AppShell } from '../components/layout/AppShell'

export function AuthenticatedLayout() {
  return (
    <AppShell>
      <Outlet />
    </AppShell>
  )
}
