const RETURN_PATH_KEY = 'wastina.auth.return-path'

export function isSafeReturnPath(
  value: string | null | undefined,
): value is string {
  return Boolean(
    value &&
    value.startsWith('/') &&
    !value.startsWith('//') &&
    !value.includes('://'),
  )
}

export function saveReturnPath(value: string | null | undefined) {
  if (!isSafeReturnPath(value)) return
  sessionStorage.setItem(RETURN_PATH_KEY, value)
}

export function consumeReturnPath(): string {
  const value = sessionStorage.getItem(RETURN_PATH_KEY)
  sessionStorage.removeItem(RETURN_PATH_KEY)
  return isSafeReturnPath(value) ? value : '/app'
}
