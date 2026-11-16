import type { FormEvent, ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Logo } from '../ui'

interface Props {
  title: string
  subtitle: string
  error?: string
  onSubmit: (e: FormEvent) => void
  children: ReactNode
  footer: ReactNode
}

/** Centered panel used by the login and sign-up screens. */
export default function AuthShell({ title, subtitle, error, onSubmit, children, footer }: Props) {
  return (
    <main className="min-h-screen bg-shell flex items-center justify-center p-4">
      <div className="w-full max-w-sm flex flex-col gap-6">
        <Link to="/catalogue" className="self-center" aria-label="TheVault">
          <Logo size={44} tagline />
        </Link>
        <form onSubmit={onSubmit} className="tv-panel p-6 flex flex-col gap-4" noValidate>
          <div className="flex flex-col gap-1">
            <h1 className="tv-title">{title}</h1>
            <p className="text-[13px] text-ink-muted">{subtitle}</p>
          </div>
          {error && (
            <p role="alert" className="flex items-center gap-2 text-[14px] font-semibold text-loss">
              <span className="tv-led tv-led-power" aria-hidden="true" />
              {error}
            </p>
          )}
          {children}
        </form>
        <p className="text-center text-[14px] text-ink-muted">{footer}</p>
      </div>
    </main>
  )
}
