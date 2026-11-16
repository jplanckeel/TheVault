import type { HTMLAttributes, ReactNode } from 'react'

interface Props extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode
  /** `raised` panel on the shell, or recessed `well` (selectors, chart screen). */
  tone?: 'raised' | 'well'
  padding?: 'none' | 'sm' | 'md' | 'lg'
}

const PADDING = { none: 'p-0', sm: 'p-3', md: 'p-4', lg: 'p-5 md:p-6' }

/** Flat panel with large radius (`radius-lg`). */
export default function Card({ children, tone = 'raised', padding = 'md', className = '', ...rest }: Props) {
  const base = tone === 'well' ? 'tv-well' : 'tv-panel'
  return (
    <div className={`${base} ${PADDING[padding]} ${className}`} {...rest}>
      {children}
    </div>
  )
}
