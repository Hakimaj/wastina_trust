export type Locale = 'en' | 'am'

export type SessionSnapshot = {
  userId: string
  accountStatus: string
  identityStatus: string
  roles: string[]
}

export type QueryKeyFactory = readonly [string, ...(readonly unknown[])]
