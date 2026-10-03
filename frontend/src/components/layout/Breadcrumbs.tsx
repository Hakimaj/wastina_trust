import { Link } from 'react-router-dom'

const labelMap: Record<string, string> = {
  app: 'Dashboard',
  profile: 'Profile',
  identity: 'Identity',
  employment: 'Employment',
  trust: 'Trust',
  guarantees: 'Guarantees',
  documents: 'Documents',
  payments: 'Payments',
  security: 'Security',
  support: 'Support',
  employer: 'Employer',
  guarantor: 'Guarantor',
  landlord: 'Landlord',
  compliance: 'Compliance',
  finance: 'Finance',
  admin: 'Admin',
}

export function Breadcrumbs() {
  const pathname = typeof window !== 'undefined' ? window.location.pathname : '/'
  const segments = pathname.split('/').filter(Boolean)
  const items = segments.reduce<Array<{ label: string; path: string }>>(
    (acc, segment, index) => {
      const path = `/${segments.slice(0, index + 1).join('/')}`
      const label = labelMap[segment] ?? segment.replace(/-/g, ' ')
      acc.push({ label: label.charAt(0).toUpperCase() + label.slice(1), path })
      return acc
    },
    [],
  )

  if (!items.length) return null

  return (
    <nav className="breadcrumbs" aria-label="Breadcrumb">
      {items.map((item, index) => {
        const isCurrent = index === items.length - 1
        return (
          <span key={item.path} className="breadcrumb-item">
            {isCurrent ? (
              <span aria-current="page">{item.label}</span>
            ) : (
              <Link to={item.path}>{item.label}</Link>
            )}
            {!isCurrent ? <span className="breadcrumb-separator">/</span> : null}
          </span>
        )
      })}
    </nav>
  )
}
