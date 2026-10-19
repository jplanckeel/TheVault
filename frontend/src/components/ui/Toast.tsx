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

const TONE_STYLES: Record<ToastTone, { icon: string; cls: string }> = {
  success: { icon: 'check_circle', cls: 'border-vault-success/40 text-vault-success' },
  error: { icon: 'error', cls: 'border-vault-error/40 text-vault-error' },
  info: { icon: 'info', cls: 'border-vault-primary/40 text-vault-primary' },
}

/**
 * Minimal toast system, no external dep.
 * Wrap your app with <ToastProvider> and call useToast().push("Saved!", "success").
 */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const push = useCallback((message: string, tone: ToastTone = 'info') => {
    setToasts((prev) => [...prev, { id: Date.now() + Math.random(), tone, message }])
  }, [])

  const remove = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  return (
    <ToastCtx.Provider value={{ push }}>
      {children}
      <div className="fixed bottom-4 right-4 z-[200] flex flex-col gap-2 pointer-events-none">
        {toasts.map((t) => (
          <ToastItem key={t.id} toast={t} onDone={() => remove(t.id)} />
        ))}
      </div>
    </ToastCtx.Provider>
  )
}

function ToastItem({ toast, onDone }: { toast: Toast; onDone: () => void }) {
  useEffect(() => {
    const timer = setTimeout(onDone, 3500)
    return () => clearTimeout(timer)
  }, [onDone])

  const style = TONE_STYLES[toast.tone]
  return (
    <div
      role="status"
      className={[
        'pointer-events-auto flex items-center gap-3 min-w-[260px] max-w-sm',
        'rounded-lg px-4 py-3 glass-active animate-fade-in-up',
        style.cls,
      ].join(' ')}
    >
      <MaterialIcon name={style.icon} size={20} />
      <span className="text-sm text-vault-on-surface">{toast.message}</span>
    </div>
  )
}

export function useToast(): Ctx {
  const ctx = useContext(ToastCtx)
  if (!ctx) {
    // Graceful no-op fallback if used outside the provider.
    return { push: (m: string) => console.log('[toast]', m) }
  }
  return ctx
}
