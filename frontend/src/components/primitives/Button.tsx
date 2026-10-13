import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { forwardRef } from 'react'

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant
  icon?: ReactNode
  /**
   * Renders a spinner, sets `aria-busy` and blocks interaction. Prevents a double submit from
   * creating a duplicate record.
   */
  loading?: boolean
  /** Accessible name when the visible label is hidden or replaced by an icon. */
  srLabel?: string
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    { variant = 'primary', icon, loading = false, srLabel, children, className = '', disabled, ...props },
    ref,
  ) => (
    <button
      ref={ref}
      className={['button', `button--${variant}`, className].filter(Boolean).join(' ')}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      aria-label={srLabel}
      {...props}
    >
      {loading ? <span className="button__spinner" aria-hidden="true" /> : null}
      {icon && !loading ? <span className="button__icon">{icon}</span> : null}
      {children}
    </button>
  ),
)

Button.displayName = 'Button'
