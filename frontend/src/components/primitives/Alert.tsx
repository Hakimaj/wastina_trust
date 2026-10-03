import type { ReactNode } from 'react'

type AlertProps = {
  title?: string
  children: ReactNode
  tone?: 'info' | 'success' | 'warning' | 'danger'
}

export function Alert({ title, children, tone = 'info' }: AlertProps) {
  return (
    <div
      className={['alert', `alert--${tone}`].join(' ')}
      role={tone === 'danger' ? 'alert' : 'status'}
    >
      {title ? <strong className="alert__title">{title}</strong> : null}
      <div>{children}</div>
    </div>
  )
}
