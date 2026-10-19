import type { ButtonHTMLAttributes, ReactNode } from 'react'
import MaterialIcon from './MaterialIcon'

type Variant = 'primary' | 'ghost' | 'subtle' | 'danger'
type Size = 'sm' | 'md' | 'lg'

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  children?: ReactNode
  variant?: Variant
  size?: Size
  /** Material symbol name to render before label */
  leftIcon?: string
  /** Material symbol name to render after label */
  rightIcon?: string
  /** Make button full-width */
  fullWidth?: boolean
}

const VARIANTS: Record<Variant, string> = {
  primary:
    'bg-vault-primary text-vault-on-primary hover:bg-vault-primary/90 hover:glow-purple active:scale-[0.98]',
  ghost:
    'border border-vault-outline-variant/60 text-vault-on-surface bg-transparent hover:bg-vault-surface-high/60 hover:border-vault-primary/50',
  subtle:
    'bg-vault-surface-high text-vault-on-surface hover:bg-vault-surface-highest',
  danger:
    'bg-vault-error/15 text-vault-error border border-vault-error/30 hover:bg-vault-error/25',
}

const SIZES: Record<Size, string> = {
  sm: 'h-8 px-3 text-xs gap-1.5',
  md: 'h-10 px-4 text-sm gap-2',
  lg: 'h-12 px-6 text-base gap-2.5',
}

/**
 * Premium button with primary (filled) / ghost (outlined) / subtle / danger variants.
 * Always rounded-lg, always with focus ring and active scale.
 */
export default function Button({
  children,
  variant = 'primary',
  size = 'md',
  leftIcon,
  rightIcon,
  fullWidth,
  className = '',
  disabled,
  ...rest
}: Props) {
  return (
    <button
      className={[
        'inline-flex items-center justify-center font-semibold rounded-lg',
        'transition-all duration-150 ease-out',
        'focus-ring',
        'disabled:opacity-40 disabled:cursor-not-allowed disabled:active:scale-100',
        SIZES[size],
        VARIANTS[variant],
        fullWidth ? 'w-full' : '',
        className,
      ].join(' ')}
      disabled={disabled}
      {...rest}
    >
      {leftIcon && <MaterialIcon name={leftIcon} size={size === 'sm' ? 16 : 18} />}
      {children}
      {rightIcon && <MaterialIcon name={rightIcon} size={size === 'sm' ? 16 : 18} />}
    </button>
  )
}
