import type { ReactNode } from 'react'

interface Props {
  label: string
  value: ReactNode
  hint?: ReactNode
  onClick?: () => void
  className?: string
}

/** Counter tile: big Archivo number over a small label. Clickable when it opens a filter. */
export default function Stat({ label, value, hint, onClick, className = '' }: Props) {
  const content = (
    <>
      <span className="tv-stat-value">{value}</span>
      <span className="tv-stat-label">{label}</span>
      {hint && <span className="text-[12px] text-ink-faint">{hint}</span>}
    </>
  )
  return onClick ? (
    <button type="button" onClick={onClick} className={`tv-stat ${className}`}>
      {content}
    </button>
  ) : (
    <div className={`tv-stat ${className}`}>{content}</div>
  )
}
