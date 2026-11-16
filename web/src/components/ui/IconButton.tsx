import type { ButtonHTMLAttributes } from 'react'
import MaterialIcon from './MaterialIcon'

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  icon: string
  /** Accessible name — icon buttons always have one. */
  'aria-label': string
  size?: 'sm' | 'md'
  variant?: 'key' | 'ghost' | 'lavender'
}

/** Square key (radius-md) holding one icon. */
export default function IconButton({ icon, size = 'md', variant = 'key', className = '', type = 'button', ...rest }: Props) {
  return (
    <button type={type} className={`tv-iconbtn tv-iconbtn-${variant} ${size === 'sm' ? 'tv-iconbtn-sm' : ''} ${className}`} {...rest}>
      <MaterialIcon name={icon} size={size === 'sm' ? 18 : 22} />
    </button>
  )
}
