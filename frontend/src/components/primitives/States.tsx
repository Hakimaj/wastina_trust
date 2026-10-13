import type { ReactNode } from 'react'
import { Alert } from './Alert'
import { Button } from './Button'
import { Skeleton } from './Skeleton'

export type EmptyStateProps = {
  title: string
  description?: string
  /** Primary call to action, when the user can actually do something about the empty state. */
  action?: { label: string; onClick: () => void }
  icon?: ReactNode
}

/**
 * Shown when a request succeeded but returned nothing.
 *
 * Deliberately distinct from `ErrorState`: an empty result is a valid first-time state, not a
 * failure, and should never be rendered as one.
 */
export function EmptyState({ title, description, action, icon }: EmptyStateProps) {
  return (
    <div className="state state--empty" role="status">
      {icon ? (
        <span className="state__icon" aria-hidden="true">
          {icon}
        </span>
      ) : null}
      <h3 className="state__title">{title}</h3>
      {description ? <p className="state__description">{description}</p> : null}
      {action ? (
        <Button type="button" variant="secondary" onClick={action.onClick}>
          {action.label}
        </Button>
      ) : null}
    </div>
  )
}

export type ErrorStateProps = {
  title?: string
  /** Human-readable cause, normally from `errorMessage(error, …)`. */
  message?: string
  /** Correlates the failure with backend logs. */
  requestId?: string
  onRetry?: () => void
  retryLabel?: string
  tone?: 'danger' | 'warning'
}

/** Shown when a request failed. Always names a retry path where one exists. */
export function ErrorState({
  title = 'Something went wrong',
  message,
  requestId,
  onRetry,
  retryLabel = 'Try again',
  tone = 'danger',
}: ErrorStateProps) {
  return (
    <div className="state state--error" role="alert">
      <Alert tone={tone} title={title}>
        {message ?? 'The request could not be completed.'}
      </Alert>
      {requestId ? (
        <p className="state__meta">
          Reference: <code>{requestId}</code>
        </p>
      ) : null}
      {onRetry ? (
        <Button type="button" variant="secondary" onClick={onRetry}>
          {retryLabel}
        </Button>
      ) : null}
    </div>
  )
}

export type LoadingStateProps = {
  /** Number of skeleton rows to reserve, so the layout does not jump when data arrives. */
  rows?: number
  label?: string
}

/**
 * Skeleton placeholder shown while a request is in flight.
 *
 * `aria-busy` plus visually hidden text announces the wait to assistive technology without
 * depending on the skeleton shapes being meaningful.
 */
export function LoadingState({ rows = 3, label = 'Loading…' }: LoadingStateProps) {
  return (
    <div className="state state--loading" aria-busy="true" aria-live="polite">
      <span className="visually-hidden">{label}</span>
      <div className="skeleton-stack" aria-hidden="true">
        {Array.from({ length: Math.max(1, rows) }, (_, index) => (
          <Skeleton key={index} />
        ))}
      </div>
    </div>
  )
}

/**
 * Renders exactly one of loading / error / empty / content.
 *
 * Pages use this instead of repeating the same four `if` branches, which is what previously let
 * some pages render an empty list while still loading.
 */
export type AsyncContentProps<T> = {
  isLoading: boolean
  error?: unknown
  /**
   * The fetched rows, or `undefined` while the request is in flight.
   *
   * Required (rather than optional) so that `T` is inferred from the argument
   * the caller actually passes; an optional prop would infer `unknown` and
   * leave the render callback untyped.
   */
  data: T | undefined
  /** True when `data` is present but carries nothing to show. */
  isEmpty?: (data: T) => boolean
  loadingRows?: number
  loadingLabel?: string
  errorTitle?: string
  emptyTitle?: string
  emptyDescription?: string
  emptyAction?: { label: string; onClick: () => void }
  onRetry?: () => void
  children: (data: T) => ReactNode
}

export function AsyncContent<T>({
  isLoading,
  error,
  data,
  isEmpty,
  loadingRows,
  loadingLabel,
  errorTitle,
  emptyTitle,
  emptyDescription,
  emptyAction,
  onRetry,
  children,
}: AsyncContentProps<T>) {
  if (isLoading) return <LoadingState rows={loadingRows} label={loadingLabel} />
  if (error) {
    return (
      <ErrorState
        title={errorTitle}
        message={error instanceof Error ? error.message : String(error)}
        onRetry={onRetry}
      />
    )
  }
  if (data === undefined) return <LoadingState rows={loadingRows} label={loadingLabel} />
  if (isEmpty?.(data)) {
    return (
      <EmptyState
        title={emptyTitle ?? 'Nothing to show yet'}
        description={emptyDescription}
        action={emptyAction}
      />
    )
  }
  return <>{children(data)}</>
}