export const supportedLocales = ['en', 'am'] as const
export type SupportedLocale = (typeof supportedLocales)[number]
