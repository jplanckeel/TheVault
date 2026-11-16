import type { InputHTMLAttributes, ReactNode } from 'react'
import { forwardRef, useId } from 'react'
import MaterialIcon from './MaterialIcon'

interface Props extends Omit<InputHTMLAttributes<HTMLInputElement>, 'size'> {
  leftIcon?: string
  rightAdornment?: ReactNode
  /** Engraved label above the field. */
  label?: string
  error?: string
  size?: 'sm' | 'md'
}

/** Recessed field (`well`), as on mobile. */
const Input = forwardRef<HTMLInputElement, Props>(function Input({ leftIcon, rightAdornment, label, error, size = 'md', className = '', id, ...rest }, ref) {
  const generated = useId()
  const inputId = id || generated
  return (
    <div className={`w-full ${className}`}>
      {label && (
        <label htmlFor={inputId} className="tv-engrave tv-label">
          {label}
        </label>
      )}
      <div className={`tv-field ${size === 'sm' ? 'tv-field-sm' : ''} ${error ? 'tv-field-error' : ''}`}>
        {leftIcon && <MaterialIcon name={leftIcon} size={20} />}
        <input id={inputId} ref={ref} aria-invalid={error ? true : undefined} {...rest} />
        {rightAdornment}
      </div>
      {error && <p className="mt-1.5 text-[13px] text-loss">{error}</p>}
    </div>
  )
})

export default Input
