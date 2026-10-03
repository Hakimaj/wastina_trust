import type { ReactNode } from 'react'

type PageHeaderProps = {
  title?: string
  description?: string
  actions?: ReactNode
}

export function PageHeader({ title, description, actions }: PageHeaderProps) {
  if (!title && !description && !actions) return null

  return (
    <header className="page-header">
      <div className="page-header__text">
        {title ? <h1 className="page-header__title">{title}</h1> : null}
        {description ? <p className="page-header__description">{description}</p> : null}
      </div>
      {actions ? <div className="page-header__actions">{actions}</div> : null}
    </header>
  )
}
