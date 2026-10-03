import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { I18nContext, type I18nContextValue } from './I18nContext'
import type { LocaleCode } from './translations'
import { localeResources } from './translations'
import { formatCurrency, formatDate, formatNumber } from './formatters'

type TranslationDictionary = Record<string, unknown>

function getValueAtPath(target: TranslationDictionary, key: string): unknown {
  return key.split('.').reduce<unknown>((current, segment) => {
    if (typeof current === 'object' && current !== null && segment in current) {
      return (current as Record<string, unknown>)[segment]
    }
    return undefined
  }, target)
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<LocaleCode>(() => {
    const saved = window.localStorage.getItem('wastina.locale') as LocaleCode | null
    return saved === 'am' || saved === 'en' ? saved : 'en'
  })

  useEffect(() => {
    document.documentElement.lang = locale
    document.documentElement.dir = 'ltr'
    window.localStorage.setItem('wastina.locale', locale)
  }, [locale])

  const setLocale = useCallback((nextLocale: LocaleCode) => {
    setLocaleState(nextLocale)
  }, [])

  const t = useCallback(
    (key: string, fallback?: string): string => {
      const value = getValueAtPath(localeResources[locale] as TranslationDictionary, key)
      if (typeof value === 'string') return value
      return fallback ?? key
    },
    [locale],
  )

  const value = useMemo<I18nContextValue>(
    () => ({
      locale,
      setLocale,
      t,
      formatDate: (value) => formatDate(value, locale),
      formatNumber: (value) => formatNumber(value, locale),
      formatCurrency: (value) => formatCurrency(value, locale),
    }),
    [locale, setLocale, t],
  )

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}
