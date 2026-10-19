import type { ReactNode } from 'react'
import MaterialIcon from './MaterialIcon'

type Tone = 'neutral' | 'primary' | 'secondary' | 'success' | 'error' | 'warning' | 'info'
type Size = 'sm' | 'md'

interface Props {
  children: ReactNode
  tone?: Tone
  size?: Size
  /** Material symbol name shown before label */
  icon?: string
  /** Filled background (vs ghost border) */
  variant?: 'soft' | 'solid' | 'outline'
  className?: string
  onClick?: () => void
}

const TONES: Record<Tone, { soft: string; solid: string; outline: string }> = {
  neutral: {
    soft: 'bg-vault-surface-highest/70 text-vault-on-surface-variant',
    solid: 'bg-vault-surface-highest text-vault-on-surface',
    outline: 'border border-vault-outline-variant text-vault-on-surface-variant',
  },
  primary: {
    soft: 'bg-vault-primary/15 text-vault-primary',
    solid: 'bg-vault-primary text-vault-on-primary',
    outline: 'border border-vault-primary/50 text-vault-primary',
  },
  secondary: {
    soft: 'bg-vault-secondary-container/40 text-vault-secondary',
    solid: 'bg-vault-secondary-container text-vault-on-secondary-container',
    outline: 'border border-vault-secondary/50 text-vault-secondary',
  },
  success: {
    soft: 'bg-vault-success/15 text-vault-success',
    solid: 'bg-vault-success text-black',
    outline: 'border border-vault-success/50 text-vault-success',
  },
  error: {
    soft: 'bg-vault-error/15 text-vault-error',
    solid: 'bg-vault-error text-black',
    outline: 'border border-vault-error/50 text-vault-error',
  },
  warning: {
    soft: 'bg-vault-warning/15 text-vault-warning',
    solid: 'bg-vault-warning text-black',
    outline: 'border border-vault-warning/50 text-vault-warning',
  },
  info: {
    soft: 'bg-vault-info/15 text-vault-info',
    solid: 'bg-vault-info text-black',
    outline: 'border border-vault-info/50 text-vault-info',
  },
}

const SIZES: Record<Size, string> = {
  sm: 'h-5 px-2 text-[10px] gap-1',
  md: 'h-6 px-2.5 text-xs gap-1.5',
}

/**
 * Pill-shaped indicator. Use for condition badges (CIB, MINT, SEALED),
 * delta percentages (+14.2%), platform tags (PS5, RETRO), etc.
 */
export default function Chip({
  children,
  tone = 'neutral',
  size = 'md',
  icon,
  variant = 'soft',
  className = '',
  onClick,
}: Props) {
  const tonal = TONES[tone][variant]
  const Tag = onClick ? 'button' : 'span'
  return (
    <Tag
      onClick={onClick}
      className={[
        'inline-flex items-center rounded-full font-semibold tracking-wide whitespace-nowrap',
        SIZES[size],
        tonal,
        onClick ? 'cursor-pointer hover:opacity-90 focus-ring' : '',
        className,
      ].join(' ')}
    >
      {icon && <MaterialIcon name={icon} size={size === 'sm' ? 12 : 14} />}
      {children}
    </Tag>
  )
}
