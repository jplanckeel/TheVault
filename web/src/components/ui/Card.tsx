import type { HTMLAttributes, ReactNode } from 'react'

interface Props extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode
  /** Glassmorphic background */
  glass?: boolean
  /** Outer purple glow */
  glow?: boolean
  /** Padding scale */
  padding?: 'none' | 'sm' | 'md' | 'lg'
  /** Radius scale */
  radius?: 'md' | 'lg' | 'xl' | '2xl'
  className?: string
}

const PADDING = {
  none: '',
  sm: 'p-4',
  md: 'p-5',
  lg: 'p-6',
}

const RADIUS = {
  md: 'rounded-md',
  lg: 'rounded-lg',
  xl: 'rounded-xl',
  '2xl': 'rounded-2xl',
}

/**
 * Premium card container — base building block of the UI.
 * Default: solid surface-low with subtle border. With `glass`, becomes glassmorphic.
 */
export default function Card({
  children,
  glass,
  glow,
  padding = 'md',
  radius = 'xl',
  className = '',
  ...rest
}: Props) {
  const base = glass
    ? 'glass border border-vault-outline-variant/40'
    : 'bg-vault-surface-low border border-vault-outline-variant/30'

  return (
    <div
      className={`${base} ${RADIUS[radius]} ${PADDING[padding]} ${
        glow ? 'glow-purple' : ''
      } ${className}`}
      {...rest}
    >
      {children}
    </div>
  )
}
