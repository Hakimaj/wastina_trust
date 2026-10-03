import { createContext } from 'react'
import type { LocaleCode } from './translations'

export type I18nContextValue = {
  locale: LocaleCode
  setLocale: (locale: LocaleCode) => void
  t: (key: string, fallback?: string) => string
  formatDate: (value: string | Date) => string
  formatNumber: (value: number) => string
  formatCurrency: (value: number) => string
}

export const I18nContext = createContext<I18nContextValue | undefined>(undefined)
