import type { SelectHTMLAttributes } from 'react'
import { forwardRef, useId } from 'react'
import MaterialIcon from './MaterialIcon'

interface Props extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'size'> {
  options: { value: string; label: string }[]
  label?: string
  size?: 'sm' | 'md'
}

/** Native select in a recessed field. */
const Select = forwardRef<HTMLSelectElement, Props>(function Select({ options, label, size = 'md', className = '', id, ...rest }, ref) {
  const generated = useId()
  const selectId = id || generated
  return (
    <div className={`w-full ${className}`}>
      {label && (
        <label htmlFor={selectId} className="tv-engrave tv-label">
          {label}
        </label>
      )}
      <div className={`tv-field relative ${size === 'sm' ? 'tv-field-sm' : ''}`}>
        <select ref={ref} id={selectId} className="pr-6" {...rest}>
          {options.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
        <MaterialIcon name="expand_more" size={20} className="absolute right-3 pointer-events-none" />
      </div>
    </div>
  )
})

export default Select
