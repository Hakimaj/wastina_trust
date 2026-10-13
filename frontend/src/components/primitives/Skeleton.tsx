import type { HTMLAttributes } from 'react'

/**
 * Loading placeholder block.
 *
 * Purely decorative: every list that uses skeletons pairs it with `LoadingState`, which carries the
 * accessible announcement, so the shape itself is hidden from assistive technology.
 */
export function Skeleton({ className = '', ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={['skeleton', className].filter(Boolean).join(' ')}
      aria-hidden="true"
      {...props}
    />
  )
}