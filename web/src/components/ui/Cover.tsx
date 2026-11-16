import { useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { familyOf, shortPlatform } from '../../lib/platforms'
import type { Condition } from '../../lib/condition'
import { ConditionTag } from './Tags'

interface CoverProps {
  title: string
  platform?: string
  src?: string
  condition?: Condition | null
  size?: 'xs' | 'm' | 'l'
  className?: string
}

/**
 * Cartridge-shaped cover: rounded top, chamfered bottom corners, manufacturer band.
 * Without an image (or when it fails), the title is set on the blank cover.
 */
export function GameCover({ title, platform, src, condition, size = 'm', className = '' }: CoverProps) {
  const [failed, setFailed] = useState(false)
  const image = src && !failed ? src : undefined
  return (
    <div className={`tv-cover tv-cover-${size} ${className}`} role="img" aria-label={`Jaquette : ${title}`}>
      {image && <img src={image} alt="" loading="lazy" className="tv-cover-img" onError={() => setFailed(true)} />}
      <span className={`tv-cover-band tv-fam-${familyOf(platform)}`} aria-hidden="true" />
      {condition && size !== 'xs' && <ConditionTag condition={condition} overlay compact={size === 'm'} />}
      {!image && (
        <span className="tv-cover-title" aria-hidden="true">
          {title}
        </span>
      )}
    </div>
  )
}

interface CardProps {
  title: string
  platform: string
  /** Detail route; without it the card is static (public share page). */
  to?: string
  src?: string
  condition?: Condition | null
  /** Formatted value, already prefixed with « ≈ » when indicative. */
  value?: string
  /** Extra line under the meta row (variation, owner count…). */
  footer?: ReactNode
}

/** Cover + title + platform + value — the building block of every grid. */
export function CollectionCard({ title, platform, to, src, condition, value, footer }: CardProps) {
  const body = (
    <>
      <GameCover title={title} platform={platform} src={src} condition={condition} />
      <span className="tv-ccard-title">{title}</span>
      <span className="tv-ccard-meta">
        <span className="tv-ccard-plat">{platform ? shortPlatform(platform) : '—'}</span>
        <span className="tv-ccard-value">{value ?? '—'}</span>
      </span>
      {footer}
    </>
  )
  return to ? (
    <Link to={to} className="tv-ccard">
      {body}
    </Link>
  ) : (
    <div className="tv-ccard cursor-default">{body}</div>
  )
}
