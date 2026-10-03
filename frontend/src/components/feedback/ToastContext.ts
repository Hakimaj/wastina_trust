import { createContext } from 'react'

export type ToastTone = 'success' | 'info' | 'warning' | 'danger'
export type ToastContextValue = {
  notify: (message: string, tone?: ToastTone) => number
}

export const ToastContext = createContext<ToastContextValue | undefined>(undefined)
