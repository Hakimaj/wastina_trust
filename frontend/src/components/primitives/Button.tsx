import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { forwardRef } from 'react'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'ghost'
  icon?: ReactNode
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = 'primary', icon, children, className = '', ...props }, ref) => (
    <button
      ref={ref}
      className={['button', `button--${variant}`, className].filter(Boolean).join(' ')}
      {...props}
    >
      {icon ? <span className="button__icon">{icon}</span> : null}
      {children}
    </button>
  ),
)

Button.displayName = 'Button'
