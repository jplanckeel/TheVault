import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { GameCover } from './Cover'

interface Props {
  title: string
  meta?: ReactNode
  trailing?: ReactNode
  cover?: { title: string; platform?: string; src?: string }
  to?: string
  href?: string
  onClick?: () => void
  selected?: boolean
}

/** List row: small cover, title, meta, mono trailing value. Interactive when it has a target. */
export default function ListRow({ title, meta, trailing, cover, to, href, onClick, selected }: Props) {
  const body = (
    <>
      {cover && (
        <span className="w-10 shrink-0">
          <GameCover size="xs" title={cover.title} platform={cover.platform} src={cover.src} />
        </span>
      )}
      <span className="tv-row-body">
        <span className="tv-row-title">{title}</span>
        {meta && <span className="tv-row-meta">{meta}</span>}
      </span>
      {trailing != null && <span className="tv-row-trailing">{trailing}</span>}
    </>
  )
  const cls = `tv-row ${to || href || onClick ? 'is-interactive' : ''} ${selected ? 'is-selected' : ''}`
  if (to) return <Link to={to} className={cls}>{body}</Link>
  if (href) return <a href={href} target="_blank" rel="noopener noreferrer" className={cls}>{body}</a>
  if (onClick) return <button type="button" onClick={onClick} className={cls}>{body}</button>
  return <div className={cls}>{body}</div>
}
