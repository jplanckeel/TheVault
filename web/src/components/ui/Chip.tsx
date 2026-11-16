import type { ReactNode } from 'react'
import MaterialIcon from './MaterialIcon'

type Tone = 'neutral' | 'primary' | 'secondary' | 'success' | 'error' | 'warning' | 'info'

interface Props {
  children: ReactNode
  tone?: Tone
  icon?: string
  className?: string
  title?: string
}

const DOT: Partial<Record<Tone, string>> = {
  success: 'var(--led-sync)',
  error: 'var(--led-power)',
  warning: 'var(--snes-b)',
  info: 'var(--indigo)',
}

/**
 * Small status tag (radius-xs, like the platform and condition tags). Status is told by a
 * LED dot plus the label, never by colour alone.
 */
export default function Chip({ children, tone = 'neutral', icon, className = '', title }: Props) {
  const fill = tone === 'primary' ? 'bg-indigo-soft text-indigo-ink' : tone === 'secondary' ? 'bg-lavender text-on-lavender' : 'bg-keycap text-ink'
  return (
    <span
      title={title}
      className={`inline-flex items-center gap-1.5 h-6 px-2 rounded-xs text-[11px] font-semibold whitespace-nowrap ${fill} ${className}`}
    >
      {DOT[tone] && <span className="h-1.5 w-1.5 rounded-full shrink-0" style={{ background: DOT[tone] }} aria-hidden="true" />}
      {icon && <MaterialIcon name={icon} size={14} />}
      {children}
    </span>
  )
}
