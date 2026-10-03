import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { ToastContext, type ToastContextValue, type ToastTone } from './ToastContext'
import { useI18n } from '../../i18n/useI18n'

type ToastMessage = { id: number; message: string; tone: ToastTone }

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastMessage[]>([])
  const nextId = useRef(0)
  const timers = useRef(new Map<number, number>())
  const { t } = useI18n()

  const dismiss = useCallback((id: number) => {
    const timer = timers.current.get(id)
    if (timer !== undefined) window.clearTimeout(timer)
    timers.current.delete(id)
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const notify = useCallback((message: string, tone: ToastTone = 'success') => {
    const id = ++nextId.current
    setToasts((current) => [...current, { id, message, tone }])
    const timer = window.setTimeout(() => dismiss(id), 5000)
    timers.current.set(id, timer)
    return id
  }, [dismiss])

  useEffect(() => () => {
    timers.current.forEach((timer) => window.clearTimeout(timer))
    timers.current.clear()
  }, [])

  const value = useMemo<ToastContextValue>(() => ({ notify }), [notify])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-region" aria-label={t('common.notifications')} aria-live="polite" aria-relevant="additions">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast toast--${toast.tone}`} role={toast.tone === 'danger' ? 'alert' : 'status'}>
            <span>{toast.message}</span>
            <button
              type="button"
              className="toast__dismiss"
              aria-label={t('common.dismissNotification')}
              onClick={() => dismiss(toast.id)}
            >
              <span aria-hidden="true">×</span>
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
