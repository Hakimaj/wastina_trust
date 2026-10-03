import type { ReactNode } from 'react'
import { Breadcrumbs } from './Breadcrumbs'
import { PageHeader } from './PageHeader'

type PageContainerProps = {
  title?: string
  description?: string
  children: ReactNode
  actions?: ReactNode
  showBreadcrumbs?: boolean
}

export function PageContainer({
  title,
  description,
  children,
  actions,
  showBreadcrumbs = true,
}: PageContainerProps) {
  return (
    <div className="page-container">
      {showBreadcrumbs ? <Breadcrumbs /> : null}
      {title || description || actions ? (
        <PageHeader title={title} description={description} actions={actions} />
      ) : null}
      <div className="page-content">{children}</div>
    </div>
  )
}
