import { useRef, type KeyboardEvent } from 'react'

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

const STEP: Record<string, number> = { ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 }

/**
 * Recessed rail with a raised keycap on the active segment. Behaves as a radio group:
 * one tab stop, arrow keys move and select, Home / End jump to the ends.
 */
export default function SegmentedControl<T extends string>({ options, value, onChange, label, size = 'md', className = '' }: Props<T>) {
  const refs = useRef<(HTMLButtonElement | null)[]>([])
  const current = Math.max(
    0,
    options.findIndex((o) => o.value === value),
  )

  function onKeyDown(e: KeyboardEvent) {
    let next: number | null = null
    if (e.key in STEP) next = (current + STEP[e.key] + options.length) % options.length
    else if (e.key === 'Home') next = 0
    else if (e.key === 'End') next = options.length - 1
    if (next === null) return
    e.preventDefault()
    onChange(options[next].value)
    refs.current[next]?.focus()
  }

  return (
    <div role="radiogroup" aria-label={label} onKeyDown={onKeyDown} className={`tv-seg ${size === 'lg' ? 'tv-seg-lg' : ''} ${className}`}>
      {options.map((o, i) => (
        <button
          key={o.value}
          ref={(el) => {
            refs.current[i] = el
          }}
          type="button"
          role="radio"
          aria-checked={i === current}
          tabIndex={i === current ? 0 : -1}
          onClick={() => onChange(o.value)}
          className={`tv-seg-item ${i === current ? 'is-active' : ''}`}
        >
          <span className="tv-seg-label">{o.label}</span>
          {o.sub && <span className="tv-seg-sub">{o.sub}</span>}
        </button>
      ))}
    </div>
  )
}
