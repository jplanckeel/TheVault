import { familyOf } from '../../lib/platforms'

interface Props {
  label: string
  selected: boolean
  onClick: () => void
  count?: number
  /** Adds the manufacturer dot of a platform. */
  platform?: string
}

/** Selectable pill (inked when selected), with an optional mono counter. */
export default function FilterChip({ label, selected, onClick, count, platform }: Props) {
  return (
    <button type="button" onClick={onClick} aria-pressed={selected} className={`tv-chip ${selected ? 'is-selected' : ''}`}>
      {platform && <span className={`tv-swatch tv-fam-${familyOf(platform)}`} aria-hidden="true" />}
      {label}
      {count !== undefined && <span className="tv-chip-count">{count.toLocaleString('fr-FR')}</span>}
    </button>
  )
}
