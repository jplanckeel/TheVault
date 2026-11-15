import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import IconButton from './IconButton'

interface Props {
  title: ReactNode
  /** Engraved label above the title (« Système », « Fiche »…). */
  eyebrow?: string
  /** Line under the title (counts, totals). */
  subtitle?: ReactNode
  actions?: ReactNode
  /** Shows a back key before the title. */
  back?: boolean
}

/** Screen header: back key, eyebrow, lowercase bold title, actions on the right. */
export default function PageHeader({ title, eyebrow, subtitle, actions, back }: Props) {
  const navigate = useNavigate()
  return (
    <header className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div className="flex items-start gap-3 min-w-0">
        {back && <IconButton icon="arrow_back" aria-label="Retour" onClick={() => navigate(-1)} className="mt-0.5" />}
        <div className="min-w-0 flex flex-col gap-1.5">
          {eyebrow && <span className="tv-engrave">{eyebrow}</span>}
          <h1 className="tv-title">{title}</h1>
          {subtitle && <p className="text-[13px] text-ink-muted">{subtitle}</p>}
        </div>
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </header>
  )
}
