import type { ReactNode } from 'react'
import { Badge } from './Badge'

/**
 * Maps a backend enum value onto a visual tone.
 *
 * Semantic grouping, not a per-value mapping: any value the backend adds later still lands in a
 * sensible tone instead of defaulting to "neutral unknown". Unknown values never throw.
 */
const SUCCESS = new Set([
  'ACTIVE',
  'VERIFIED',
  'SUCCEEDED',
  'COMPLETED',
  'APPROVED',
  'AVAILABLE',
  'CONFIRMED',
  'PARTIALLY_VERIFIED',
  'CURRENT',
])

const WARNING = new Set([
  'PENDING',
  'VERIFICATION_PENDING',
  'VERIFICATION_REQUIRED',
  'IN_REVIEW',
  'VIEWED',
  'DELIVERED',
  'SENT',
  'CREATED',
  'STARTED',
  'PAUSED',
  'EXPIRED',
])

const DANGER = new Set([
  'REJECTED',
  'FAILED',
  'VERIFICATION_FAILED',
  'VERIFICATION_REVOKED',
  'REVOKED',
  'SUSPENDED',
  'DISPUTED',
  'DEACTIVATED',
  'INACTIVE',
])

const INFO = new Set([
  'CREATED',
  'DRAFT',
  'OPEN',
  'PROVISIONING',
  'SELF_REPORTED',
  'UNVERIFIED',
  'NOT_AVAILABLE',
])

const NEUTRAL = new Set(['ENDED', 'LEFT', 'ARCHIVED', 'CANCELLED', 'CLOSED'])

export type StatusTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral'

export function statusTone(value: string | null | undefined): StatusTone {
  if (!value) return 'neutral'
  const normalized = value.toUpperCase()
  if (SUCCESS.has(normalized)) return 'success'
  if (WARNING.has(normalized)) return 'warning'
  if (DANGER.has(normalized)) return 'danger'
  if (INFO.has(normalized)) return 'info'
  if (NEUTRAL.has(normalized)) return 'neutral'
  return 'neutral'
}

/** `PARTIALLY_VERIFIED` -> `Partially verified`. Used when no translation exists. */
export function humanizeStatus(value: string | null | undefined): string {
  if (!value) return 'Unknown'
  const words = value.toLowerCase().split(/[_\s-]+/).filter(Boolean)
  if (words.length === 0) return value
  return words
    .map((word, index) => (index === 0 ? word[0].toUpperCase() + word.slice(1) : word))
    .join(' ')
}

export type StatusBadgeProps = {
  value: string | null | undefined
  label?: string
  tone?: StatusTone
}

/**
 * Badge for a backend status/enum value.
 *
 * When a `label` is not supplied the value is humanized. Translations should be passed as `label`
 * so English and Amharic stay consistent across every screen that renders the same status.
 */
export function StatusBadge({ value, label, tone }: StatusBadgeProps) {
  if (value === null || value === undefined || value === '') return null
  return (
    <Badge tone={tone ?? statusTone(value)}>
      {label ?? humanizeStatus(value)}
    </Badge>
  )
}

/**
 * Table cell helper.
 *
 * Kept here so every table formats missing values identically — a blank cell is never a valid
 * rendering, which previously made "no value" indistinguishable from "not yet loaded".
 */
export function CellText({
  value,
  fallback = '—',
}: {
  value: string | number | null | undefined
  fallback?: string
}) {
  if (value === null || value === undefined || value === '') return <>{fallback}</>
  return <>{value}</>
}

export type DefinitionListProps = {
  items: { label: string; value: ReactNode; key?: string }[]
}

/**
 * Read-only key/value summary.
 *
 * Uses a description list so assistive technology associates each label with its value; a table of
 * divs reads as an unlabelled grid.
 */
export function DefinitionList({ items }: DefinitionListProps) {
  return (
    <dl className="definition-list">
      {items.map((item) => (
        <div className="definition-list__row" key={item.key ?? item.label}>
          <dt className="definition-list__label">{item.label}</dt>
          <dd className="definition-list__value">{item.value}</dd>
        </div>
      ))}
    </dl>
  )
}