import type { ReactNode } from 'react'

type CardProps = {
  title?: string
  children: ReactNode
  className?: string
}

export function Card({ title, children, className = '' }: CardProps) {
  return (
    <section className={['card', className].filter(Boolean).join(' ')}>
      {title ? <h3 className="card__title">{title}</h3> : null}
      <div className="card__body">{children}</div>
    </section>
  )
}
