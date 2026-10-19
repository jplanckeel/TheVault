interface Props {
  className?: string
  /** Predefined shape */
  variant?: 'text' | 'box' | 'circle' | 'card'
  /** Number of rows for "text" variant */
  lines?: number
}

/**
 * Loading skeleton with subtle pulse on a glass background.
 * Use directly: <Skeleton variant="card" /> or compose with className.
 */
export default function Skeleton({ className = '', variant = 'box', lines = 3 }: Props) {
  if (variant === 'text') {
    return (
      <div className={`space-y-2 ${className}`}>
        {Array.from({ length: lines }).map((_, i) => (
          <div
            key={i}
            className="h-3 rounded bg-vault-surface-high animate-pulse"
            style={{ width: `${100 - i * 12}%` }}
          />
        ))}
      </div>
    )
  }
  if (variant === 'circle') {
    return (
      <div
        className={`rounded-full bg-vault-surface-high animate-pulse ${className}`}
        style={{ width: 40, height: 40 }}
      />
    )
  }
  if (variant === 'card') {
    return (
      <div className={`rounded-xl bg-vault-surface-low border border-vault-outline-variant/30 p-5 animate-pulse ${className}`}>
        <div className="h-3 w-24 rounded bg-vault-surface-high mb-4" />
        <div className="h-8 w-40 rounded bg-vault-surface-high mb-2" />
        <div className="h-3 w-32 rounded bg-vault-surface-high" />
      </div>
    )
  }
  return <div className={`rounded-lg bg-vault-surface-high animate-pulse ${className}`} />
}
