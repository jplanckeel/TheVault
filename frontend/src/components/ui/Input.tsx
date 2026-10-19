import type { InputHTMLAttributes } from 'react'
import { forwardRef } from 'react'
import MaterialIcon from './MaterialIcon'

interface Props extends Omit<InputHTMLAttributes<HTMLInputElement>, 'size'> {
  /** Material symbol name shown on the left */
  leftIcon?: string
  /** Right-side adornment (e.g. a clear button) */
  rightAdornment?: React.ReactNode
  /** Above-field label */
  label?: string
  /** Error message under field */
  error?: string
  /** Larger input variant */
  size?: 'md' | 'lg'
  /** Glassmorphic background */
  glass?: boolean
}

const SIZES = {
  md: 'h-10 text-sm',
  lg: 'h-12 text-base',
}

const Input = forwardRef<HTMLInputElement, Props>(function Input(
  { leftIcon, rightAdornment, label, error, size = 'md', glass, className = '', id, ...rest },
  ref,
) {
  const inputId = id || rest.name
  return (
    <div className="w-full">
      {label && (
        <label
          htmlFor={inputId}
          className="text-label-bold text-vault-on-surface-variant mb-2 block"
        >
          {label}
        </label>
      )}
      <div
        className={[
          'flex items-center rounded-lg transition-colors',
          glass
            ? 'glass border border-vault-outline-variant/40'
            : 'bg-vault-surface-low border border-vault-outline-variant/40',
          'focus-within:border-vault-primary focus-within:ring-2 focus-within:ring-vault-primary/30',
          SIZES[size],
          className,
        ].join(' ')}
      >
        {leftIcon && (
          <span className="pl-3 text-vault-on-surface-variant">
            <MaterialIcon name={leftIcon} size={size === 'lg' ? 20 : 18} />
          </span>
        )}
        <input
          id={inputId}
          ref={ref}
          className="flex-1 bg-transparent border-0 outline-none px-3 py-2 text-vault-on-surface placeholder:text-vault-on-surface-variant/60"
          {...rest}
        />
        {rightAdornment && <div className="pr-2 flex items-center">{rightAdornment}</div>}
      </div>
      {error && <p className="text-xs text-vault-error mt-1.5">{error}</p>}
    </div>
  )
})

export default Input
