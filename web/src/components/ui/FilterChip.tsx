interface Props {
  label: string
  selected: boolean
  onClick: () => void
  /** Optional counter shown after the label. */
  count?: number
}

/**
 * Selectable pill used for filters (type, platform, sort).
 * Mirrors `VaultFilterChip` in the mobile app.
 */
export default function FilterChip({ label, selected, onClick, count }: Props) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={selected}
      className={[
        'h-9 px-4 rounded-full border text-sm font-semibold whitespace-nowrap transition-colors focus-ring',
        selected
          ? 'bg-vault-primary/15 border-vault-primary/60 text-vault-primary'
          : 'border-vault-outline-variant/60 text-vault-on-surface-variant hover:text-vault-on-surface hover:border-vault-outline',
      ].join(' ')}
    >
      {label}
      {count !== undefined && <span className="ml-1.5 tabular-nums opacity-70">· {count}</span>}
    </button>
  )
}
