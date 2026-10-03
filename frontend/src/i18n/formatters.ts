import type { LocaleCode } from './translations'

export function formatDate(value: string | Date, locale: LocaleCode): string {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return value.toString()

  return new Intl.DateTimeFormat(locale === 'am' ? 'am-ET' : 'en-ET', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(date)
}

export function formatNumber(value: number, locale: LocaleCode): string {
  return new Intl.NumberFormat(locale === 'am' ? 'am-ET' : 'en-US').format(value)
}

export function formatCurrency(value: number, locale: LocaleCode): string {
  return new Intl.NumberFormat(locale === 'am' ? 'am-ET' : 'en-US', {
    style: 'currency',
    currency: 'ETB',
    maximumFractionDigits: 2,
  }).format(value)
}
