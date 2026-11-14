import type { ButtonHTMLAttributes } from 'react'
import MaterialIcon from './MaterialIcon'

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  icon: string
  size?: 'sm' | 'md' | 'lg'
  /** Show a small dot indicator (e.g. unread notifications) */
  badge?: boolean
  variant?: 'ghost' | 'subtle'
}

const SIZES = {
  sm: { box: 'h-8 w-8', icon: 18 },
  md: { box: 'h-10 w-10', icon: 20 },
  lg: { box: 'h-12 w-12', icon: 22 },
}

const VARIANTS = {
  ghost: 'bg-transparent hover:bg-vault-surface-high/60 text-vault-on-surface-variant hover:text-vault-on-surface',
  subtle: 'bg-vault-surface-high text-vault-on-surface hover:bg-vault-surface-highest',
}

/**
 * Square button containing a single icon — for header actions (bell, settings)
 * and chart navigation (chevrons).
 */
export default function IconButton({
  icon,
  size = 'md',
  badge,
  variant = 'ghost',
  className = '',
  ...rest
}: Props) {
  const { box, icon: iconSize } = SIZES[size]
  return (
    <button
      className={[
        'relative inline-flex items-center justify-center rounded-lg transition-colors focus-ring',
        box,
        VARIANTS[variant],
        className,
      ].join(' ')}
      {...rest}
    >
      <MaterialIcon name={icon} size={iconSize} />
      {badge && (
        <span className="absolute top-1.5 right-1.5 h-2 w-2 rounded-full bg-vault-primary glow-purple" />
      )}
    </button>
  )
}
