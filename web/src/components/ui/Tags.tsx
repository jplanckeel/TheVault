import type { ReactNode } from 'react'
import { familyOf, shortPlatform } from '../../lib/platforms'
import { conditionLabel, conditionPips, type Condition } from '../../lib/condition'

/** Platform tag: manufacturer dot, mono name, optional region. */
export function PlatformBadge({ platform, region, short = true, onWell }: { platform: string; region?: string; short?: boolean; onWell?: boolean }) {
  return (
    <span className={`tv-plat ${onWell ? 'tv-plat-on-well' : ''}`}>
      <span className={`tv-plat-band tv-fam-${familyOf(platform)}`} aria-hidden="true" />
      {short ? shortPlatform(platform) : platform}
      {region && <span className="tv-plat-region">{region}</span>}
    </span>
  )
}

/** 4-segment completeness gauge: Loose 1 · Complet 2 · Neuf 3 · Scellé 4. */
export function ConditionTag({ condition, overlay, compact }: { condition: Condition; overlay?: boolean; compact?: boolean }) {
  const on = conditionPips(condition)
  return (
    <span className={`tv-cond ${overlay ? 'tv-cond-overlay' : ''}`} aria-label={`État : ${conditionLabel(condition)}`}>
      <span className="tv-segs" aria-hidden="true">
        {[1, 2, 3, 4].map((i) => (
          <span key={i} className={`tv-seg4 ${i <= on ? 'is-on' : ''}`} />
        ))}
      </span>
      {!compact && conditionLabel(condition)}
    </span>
  )
}

/** Where a value comes from (« eBay », « IGDB ») and how fresh it is. */
export function SourceBadge({ children, updated, stale }: { children: ReactNode; updated?: string; stale?: boolean }) {
  return (
    <span className="tv-source">
      <span className={`tv-led ${stale ? 'tv-led-power' : 'tv-led-sync'}`} style={{ width: 6, height: 6 }} aria-hidden="true" />
      {children}
      {updated && <span className="tv-source-time">{updated}</span>}
    </span>
  )
}

/** Price change, always with its arrow (▲ gain, ▼ loss). */
export function PriceDelta({ value, period, pill, className = '' }: { value: number; period?: string; pill?: boolean; className?: string }) {
  const up = value >= 0
  return (
    <span className={`tv-delta ${up ? 'is-up' : 'is-down'} ${pill ? 'tv-delta-pill' : ''} ${className}`}>
      <span aria-hidden="true">{up ? '▲' : '▼'}</span>
      <span>{formatPercent(value)}</span>
      {period && <span className="tv-delta-period">{period}</span>}
    </span>
  )
}

function formatPercent(value: number) {
  return `${value >= 0 ? '+' : '−'}${Math.abs(value).toLocaleString('fr-FR', { maximumFractionDigits: 1 })} %`
}
