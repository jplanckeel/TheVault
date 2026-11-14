import type { CSSProperties } from 'react'

interface Props {
  /** Material Symbols name, e.g. "dashboard", "add", "favorite" */
  name: string
  /** Size in px (default 20) */
  size?: number
  /** Filled variant */
  filled?: boolean
  className?: string
  style?: CSSProperties
}

/**
 * Wrapper for Google Material Symbols Outlined.
 * Font is loaded globally in index.html.
 */
export default function MaterialIcon({ name, size = 20, filled, className = '', style }: Props) {
  return (
    <span
      className={`material-symbols-outlined${filled ? ' filled' : ''} ${className}`}
      style={{ fontSize: size, ...style }}
      aria-hidden="true"
    >
      {name}
    </span>
  )
}
