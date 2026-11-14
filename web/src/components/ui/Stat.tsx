import type { ReactNode } from 'react'
import Card from './Card'
import Chip from './Chip'
import MaterialIcon from './MaterialIcon'

interface Props {
  /** Small uppercase label, e.g. "TOTAL ITEMS" */
  label: string
  /** Big main value (already formatted) */
  value: ReactNode
  /** Optional delta with sign — green if positive, red if negative */
  delta?: { value: string; positive: boolean }
  /** Optional icon on the left of the label */
  icon?: string
  /** Sub-text under value */
  hint?: ReactNode
  className?: string
  /** Accent the value with the gradient text */
  accent?: boolean
  /** Card glow */
  glow?: boolean
  /** Makes the card clickable (e.g. opens the matching collection filter). */
  onClick?: () => void
}

/**
 * KPI / Stat card — mirrors `StatCard` in the mobile app. Used on the dashboard
 * counters and on the item detail page (prices per condition).
 */
export default function Stat({
  label,
  value,
  delta,
  icon,
  hint,
  className = '',
  accent,
  glow,
  onClick,
}: Props) {
  const interactive = onClick
    ? 'cursor-pointer transition-colors hover:border-vault-primary/60 focus-ring'
    : ''
  return (
    <Card
      padding="md"
      radius="2xl"
      glow={glow}
      className={`${interactive} ${className}`}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onKeyDown={onClick ? (e) => (e.key === 'Enter' || e.key === ' ') && onClick() : undefined}
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-center gap-2 min-w-0">
          {icon && (
            <div className="h-9 w-9 rounded-lg bg-vault-primary/15 text-vault-primary flex items-center justify-center shrink-0">
              <MaterialIcon name={icon} size={18} />
            </div>
          )}
          <p className="text-label-bold text-vault-on-surface-variant truncate">{label}</p>
        </div>
        {delta && (
          <Chip
            tone={delta.positive ? 'success' : 'error'}
            size="sm"
            icon={delta.positive ? 'trending_up' : 'trending_down'}
          >
            {delta.value}
          </Chip>
        )}
      </div>
      <div className="mt-3">
        <div
          className={[
            'font-display font-extrabold tracking-tight text-3xl md:text-4xl tabular-nums',
            accent ? 'text-gradient-primary' : 'text-vault-on-surface',
          ].join(' ')}
        >
          {value}
        </div>
        {hint && (
          <div className="mt-1.5 text-xs text-vault-on-surface-variant">{hint}</div>
        )}
      </div>
    </Card>
  )
}
