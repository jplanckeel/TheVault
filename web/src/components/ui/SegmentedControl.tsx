export interface SegmentOption<T extends string> {
  value: T
  label: string
  /** Mono sub-value (a price under « Complet »…). */
  sub?: string
}

interface Props<T extends string> {
  options: SegmentOption<T>[]
  value: T
  onChange: (value: T) => void
  label: string
  size?: 'md' | 'lg'
  className?: string
}

/** Recessed rail with a raised keycap on the active segment. */
export default function SegmentedControl<T extends string>({ options, value, onChange, label, size = 'md', className = '' }: Props<T>) {
  return (
    <div role="radiogroup" aria-label={label} className={`tv-seg ${size === 'lg' ? 'tv-seg-lg' : ''} ${className}`}>
      {options.map((o) => (
        <button
          key={o.value}
          type="button"
          role="radio"
          aria-checked={o.value === value}
          onClick={() => onChange(o.value)}
          className={`tv-seg-item ${o.value === value ? 'is-active' : ''}`}
        >
          <span className="tv-seg-label">{o.label}</span>
          {o.sub && <span className="tv-seg-sub">{o.sub}</span>}
        </button>
      ))}
    </div>
  )
}
