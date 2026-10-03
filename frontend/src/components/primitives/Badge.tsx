import type { ReactNode } from 'react'

type BadgeProps = {
  children: ReactNode
  tone?: 'neutral' | 'success' | 'warning' | 'danger'
}

export function Badge({ children, tone = 'neutral' }: BadgeProps) {
  return <span className={['badge', `badge--${tone}`].join(' ')}>{children}</span>
}
