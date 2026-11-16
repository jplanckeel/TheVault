import type { ButtonHTMLAttributes, ReactNode } from 'react'
import MaterialIcon from './MaterialIcon'

/** `primary` is the single indigo action of a screen (« bouton A »); `secondary` is lavender. */
type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'
type Size = 'sm' | 'md' | 'lg'

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  children?: ReactNode
  variant?: Variant
  size?: Size
  leftIcon?: string
  rightIcon?: string
  fullWidth?: boolean
}

/** Pill button of the design system. */
export default function Button({ children, variant = 'primary', size = 'md', leftIcon, rightIcon, fullWidth, className = '', type = 'button', ...rest }: Props) {
  const icon = size === 'sm' ? 16 : 20
  return (
    <button type={type} className={`tv-btn tv-btn-${size} tv-btn-${variant} ${fullWidth ? 'tv-btn-block' : ''} ${className}`} {...rest}>
      {leftIcon && <MaterialIcon name={leftIcon} size={icon} />}
      {children}
      {rightIcon && <MaterialIcon name={rightIcon} size={icon} />}
    </button>
  )
}
