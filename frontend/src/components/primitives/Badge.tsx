import type { ReactNode } from 'react'

export type BadgeTone = 'neutral' | 'success' | 'warning' | 'danger' | 'info'

type BadgeProps = {
  children: ReactNode
  tone?: BadgeTone
  /**
   * Conveys the badge's meaning to assistive technology when the visible text is an enum value
   * rather than a sentence. Omit for plain status text that already reads correctly.
   */
  srLabel?: string
}

export function Badge({ children, tone = 'neutral', srLabel }: BadgeProps) {
  return (
    <span className={['badge', `badge--${tone}`].join(' ')}>
      {srLabel ? <span className="visually-hidden">{srLabel}: </span> : null}
      {children}
    </span>
  )
}