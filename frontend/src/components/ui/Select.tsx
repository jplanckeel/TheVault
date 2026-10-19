import type { SelectHTMLAttributes } from 'react'
import { forwardRef } from 'react'

interface Option {
  value: string
  label: string
}

interface Props extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'size'> {
  options: Option[]
  label?: string
  size?: 'md' | 'lg'
}

const SIZES = {
  md: 'h-10 text-sm',
  lg: 'h-12 text-base',
}

const Select = forwardRef<HTMLSelectElement, Props>(function Select(
  { options, label, size = 'md', className = '', id, ...rest },
  ref,
) {
  const selectId = id || rest.name
  return (
    <div className="w-full">
      {label && (
        <label
          htmlFor={selectId}
          className="text-label-bold text-vault-on-surface-variant mb-2 block"
        >
          {label}
        </label>
      )}
      <div
        className={[
          'relative flex items-center rounded-lg transition-colors',
          'bg-vault-surface-low border border-vault-outline-variant/40',
          'focus-within:border-vault-primary focus-within:ring-2 focus-within:ring-vault-primary/30',
          SIZES[size],
          className,
        ].join(' ')}
      >
        <select
          ref={ref}
          id={selectId}
          className="appearance-none bg-transparent w-full px-3 py-2 pr-9 text-vault-on-surface outline-none cursor-pointer"
          {...rest}
        >
          {options.map((o) => (
            <option key={o.value} value={o.value} className="bg-vault-surface-low">
              {o.label}
            </option>
          ))}
        </select>
        <span className="material-symbols-outlined absolute right-2 pointer-events-none text-vault-on-surface-variant" style={{ fontSize: 18 }}>
          expand_more
        </span>
      </div>
    </div>
  )
})

export default Select
