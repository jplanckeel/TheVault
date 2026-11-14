import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import MaterialIcon from './MaterialIcon'

type ToastTone = 'success' | 'error' | 'info'

interface Toast {
  id: number
  tone: ToastTone
  message: string
}

interface Ctx {
  push: (message: string, tone?: ToastTone) => void
}

const ToastCtx = createContext<Ctx | null>(null)

const TONES: Record<ToastTone, { icon: string; kicker: string; bg: string }> = {
  success: { icon: 'check', kicker: 'Fait', bg: '#2B9A55' },
  error: { icon: 'priority_high', kicker: 'Erreur', bg: '#D9362E' },
  info: { icon: 'info', kicker: 'Info', bg: '#6E5CE6' },
}

/** Console-trophy style notifications (pill, bottom of the screen). */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const push = useCallback((message: string, tone: ToastTone = 'info') => {
    setToasts((prev) => [...prev, { id: Date.now() + Math.random(), tone, message }])
  }, [])
  const remove = useCallback((id: number) => setToasts((prev) => prev.filter((t) => t.id !== id)), [])

  return (
    <ToastCtx.Provider value={{ push }}>
      {children}
      <div className="fixed inset-x-4 bottom-28 md:bottom-6 md:left-auto md:right-6 z-[200] flex flex-col items-center md:items-end gap-2 pointer-events-none" aria-live="polite">
        {toasts.map((t) => (
          <ToastItem key={t.id} toast={t} onDone={() => remove(t.id)} />
        ))}
      </div>
    </ToastCtx.Provider>
  )
}

function ToastItem({ toast, onDone }: { toast: Toast; onDone: () => void }) {
  useEffect(() => {
    const timer = setTimeout(onDone, 4000)
    return () => clearTimeout(timer)
  }, [onDone])
  const tone = TONES[toast.tone]
  return (
    <div role="status" className="tv-toast tv-enter pointer-events-auto">
      <span className="tv-toast-medal" style={{ background: tone.bg }}>
        <MaterialIcon name={tone.icon} size={20} />
      </span>
      <span className="flex flex-col gap-0.5 min-w-0">
        <span className="tv-toast-kicker">{tone.kicker}</span>
        <span className="text-[14px] font-semibold leading-snug">{toast.message}</span>
      </span>
    </div>
  )
}

// eslint-disable-next-line react-refresh/only-export-components -- hook colocated with its provider
export function useToast(): Ctx {
  return useContext(ToastCtx) ?? { push: () => {} }
}
