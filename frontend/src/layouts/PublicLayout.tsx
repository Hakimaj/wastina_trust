import { Outlet } from 'react-router-dom'

export function PublicLayout() {
  return (
    <main className="page-frame">
      <Outlet />
    </main>
  )
}
