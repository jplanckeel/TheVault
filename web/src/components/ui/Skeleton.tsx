interface Props {
  className?: string
  variant?: 'text' | 'box' | 'card'
  lines?: number
}

/** Loading placeholder in the recessed colour. */
export default function Skeleton({ className = '', variant = 'box', lines = 3 }: Props) {
  if (variant === 'text') {
    return (
      <div className={`space-y-2 ${className}`} aria-hidden="true">
        {Array.from({ length: lines }).map((_, i) => (
          <div key={i} className="tv-skel h-3" style={{ width: `${100 - i * 12}%` }} />
        ))}
      </div>
    )
  }
  if (variant === 'card') return <div className={`tv-skel rounded-lg h-32 ${className}`} aria-hidden="true" />
  return <div className={`tv-skel ${className}`} aria-hidden="true" />
}
