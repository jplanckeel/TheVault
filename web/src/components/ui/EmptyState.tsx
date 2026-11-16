import type { ReactNode } from 'react'
import Button from './Button'
import { SnesDots } from './Brand'

interface Props {
  /** Kept for callers; empty states are signed by the SNES diamond. */
  icon?: string
  title: string
  description?: ReactNode
  cta?: { label: string; onClick?: () => void; icon?: string }
  className?: string
}

/** Empty or error state: the diamond, a short title, one action. */
export default function EmptyState({ title, description, cta, className = '' }: Props) {
  return (
    <div className={`tv-panel flex flex-col items-center text-center px-6 py-12 ${className}`}>
      <SnesDots size={36} className="mb-5" />
      <h3 className="tv-section">{title}</h3>
      {description && <p className="mt-2 max-w-md text-[15px] text-ink-muted">{description}</p>}
      {cta && (
        <Button className="mt-6" leftIcon={cta.icon} onClick={cta.onClick}>
          {cta.label}
        </Button>
      )}
    </div>
  )
}
