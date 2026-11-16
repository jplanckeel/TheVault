import { useMemo, useState } from 'react'
import type { PriceSnapshot } from '../types'
import { PriceChart, PriceDelta, SegmentedControl, SourceBadge } from './ui'
import type { ChartPoint } from './ui'
import { PRICE_CONDITIONS, type PriceCondition } from '../lib/condition'
import { formatEuro, formatRelative } from '../lib/format'

const RANGES = [
  { value: '1M', label: '1M', days: 30 },
  { value: '6M', label: '6M', days: 180 },
  { value: '1A', label: '1A', days: 365 },
  { value: 'ALL', label: 'Tout', days: Number.POSITIVE_INFINITY },
] as const
type RangeKey = (typeof RANGES)[number]['value']

const DAY = 24 * 60 * 60 * 1000

interface Props {
  prices: PriceSnapshot[]
  /** eBay search terms for « Voir les annonces ». */
  searchQuery: string
  /** Condition shown first (the item's own one when it has a quote). */
  initial?: PriceCondition
}

/** eBay quote of an item: one segment per condition, the latest median and its curve. */
export default function PricePanel({ prices, searchQuery, initial }: Props) {
  const byCondition = useMemo(() => {
    const map = new Map<PriceCondition, PriceSnapshot[]>()
    for (const p of [...prices].sort((a, b) => a.scraped_at.localeCompare(b.scraped_at))) {
      map.set(p.condition, [...(map.get(p.condition) ?? []), p])
    }
    return map
  }, [prices])
  const firstQuoted = PRICE_CONDITIONS.find((c) => byCondition.has(c.value))?.value ?? 'cib'
  const [condition, setCondition] = useState<PriceCondition>(initial && byCondition.has(initial) ? initial : firstQuoted)
  const [range, setRange] = useState<RangeKey>('6M')
  const [now] = useState(() => Date.now())

  const history = useMemo(() => byCondition.get(condition) ?? [], [byCondition, condition])
  const latest = history[history.length - 1]
  const chart: ChartPoint[] = useMemo(() => {
    const days = RANGES.find((r) => r.value === range)?.days ?? Number.POSITIVE_INFINITY
    const byDay = new Map<string, number>()
    for (const p of history) {
      if (new Date(p.scraped_at).getTime() < now - days * DAY) continue
      byDay.set(p.scraped_at.slice(0, 10), p.price_median)
    }
    return [...byDay.entries()].map(([day, value]) => ({
      label: new Date(day).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' }),
      value,
    }))
  }, [history, range, now])
  const change = useMemo(() => {
    const recent = history.filter((p) => new Date(p.scraped_at).getTime() >= now - 30 * DAY)
    if (recent.length < 2 || recent[0].price_median <= 0) return null
    return ((recent[recent.length - 1].price_median - recent[0].price_median) / recent[0].price_median) * 100
  }, [history, now])

  const ebayUrl = `https://www.ebay.fr/sch/i.html?_nkw=${encodeURIComponent(searchQuery)}`
  return (
    <section className="tv-panel p-5 flex flex-col gap-4" aria-label="Cote eBay">
      <div className="flex items-center justify-between gap-3">
        <h2 className="tv-section">Cote</h2>
        {latest && <SourceBadge updated={formatRelative(latest.scraped_at)}>eBay</SourceBadge>}
      </div>

      {prices.length === 0 ? (
        <p className="text-[15px] text-ink-muted">
          Pas encore de cote pour cet objet. Le serveur relève régulièrement les annonces eBay des objets du catalogue.
        </p>
      ) : (
        <>
          <SegmentedControl
            size="lg"
            label="État coté"
            options={PRICE_CONDITIONS.map((c) => {
              const last = byCondition.get(c.value)?.at(-1)
              return { value: c.value, label: c.label, sub: last ? formatEuro(last.price_median) : '—' }
            })}
            value={condition}
            onChange={setCondition}
          />
          {latest ? (
            <div className="flex flex-wrap items-end justify-between gap-3">
              <div className="flex flex-col gap-1">
                <span className="tv-display-xl" style={{ fontSize: 32, lineHeight: '36px' }}>
                  {formatEuro(latest.price_median, 2)}
                </span>
                <span className="tv-serial">
                  MIN {formatEuro(latest.price_min)} · MAX {formatEuro(latest.price_max)} · {latest.sample_count} ANNONCE{latest.sample_count > 1 ? 'S' : ''}
                </span>
              </div>
              {change !== null && <PriceDelta value={change} period="30 j" pill />}
            </div>
          ) : (
            <p className="text-[15px] text-ink-muted">Pas de cote pour cet état.</p>
          )}
          {chart.length >= 2 ? (
            <div className="tv-well p-2 pt-3">
              <PriceChart data={chart} height={200} />
            </div>
          ) : (
            <div className="tv-well px-4 py-6 text-center text-[13px] text-ink-muted">La courbe se dessine à chaque nouveau relevé.</div>
          )}
          <SegmentedControl label="Période" options={RANGES.map((r) => ({ value: r.value, label: r.label }))} value={range} onChange={setRange} />
          <p className="text-[12px] text-ink-faint">
            {latest?.source === 'ebay_pal'
              ? 'Médiane des annonces en cours sur eBay France et Allemagne (prix demandés, lots et objets pour pièces exclus).'
              : 'Médiane des annonces eBay relevées pour cet état.'}
          </p>
        </>
      )}
      <a href={ebayUrl} target="_blank" rel="noopener noreferrer" className="tv-btn tv-btn-sm tv-btn-ghost self-start">
        Voir les annonces sur eBay
        <span className="material-symbols-outlined" style={{ fontSize: 16 }} aria-hidden="true">
          open_in_new
        </span>
      </a>
    </section>
  )
}
