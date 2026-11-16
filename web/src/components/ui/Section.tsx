import type { ReactNode } from 'react'

interface Props {
  title: ReactNode
  /** Small engraved label above the title. */
  eyebrow?: string
  description?: ReactNode
  action?: ReactNode
  children: ReactNode
  className?: string
}

/** Section with the « Plus fortes hausses » header style: lowercase bold title, link on the right. */
export default function Section({ title, eyebrow, description, action, children, className = '' }: Props) {
  return (
    <section className={className}>
      <div className="flex items-end justify-between gap-3 mb-3">
        <div className="min-w-0 flex flex-col gap-1">
          {eyebrow && <span className="tv-engrave">{eyebrow}</span>}
          <h2 className="tv-section">{title}</h2>
          {description && <p className="text-[13px] text-ink-muted">{description}</p>}
        </div>
        {action && <div className="shrink-0">{action}</div>}
      </div>
      {children}
    </section>
  )
}
