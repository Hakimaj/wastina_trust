type LogPayload = Record<string, unknown> | string | undefined

function write(
  level: 'info' | 'warn' | 'error',
  message: string,
  payload?: LogPayload,
) {
  if (import.meta.env.DEV) console[level](message, payload)
}

export const logger = {
  info: (message: string, payload?: LogPayload) =>
    write('info', message, payload),
  warn: (message: string, payload?: LogPayload) =>
    write('warn', message, payload),
  error: (message: string, payload?: LogPayload) =>
    write('error', message, payload),
}
