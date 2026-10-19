import { useMemo, useState } from 'react'
import type { PriceSnapshot } from '../../types'
import { Card, Chip, Section, Stat } from './index'
import PriceChart from './PriceChart'
import type { MultiSeriesPoint } from './PriceChart'
import MaterialIcon from './MaterialIcon'
import Button from './Button'

const RANGES = [
  { key: '1M', days: 30 },
  { key: '6M', days: 180 },
  { key: '1Y', days: 365 },
  { key: 'ALL', days: Number.POSITIVE_INFINITY },
] as const
type RangeKey = (typeof RANGES)[number]['key']

const CONDITION_SERIES = [
  { key: 'loose', label: 'Loose', color: '#60A5FA' },
  { key: 'cib', label: 'CIB', color: '#d8b9ff' },
  { key: 'new', label: 'New', color: '#34D399' },
]

const SOURCE_STYLES: Record<string, { color: string; abbrev: string }> = {
  ebay: { color: '#E53238', abbrev: 'EB' },
}

function fmt$(n: number) {
  return n.toLocaleString('fr-FR', { style: 'currency', currency: 'EUR', minimumFractionDigits: 2 })
}

function fmtDate(s: string) {
  return new Date(s).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

function ConditionPriceCard({ snapshots }: { snapshots: PriceSnapshot[] }) {
  if (snapshots.length === 0) return null
  const latest = snapshots[0]
  return (
    <Card padding="md" radius="xl">
      <div className="flex items-center justify-between mb-2">
        <Chip
          tone={latest.condition === 'new' ? 'success' : latest.condition === 'cib' ? 'primary' : 'info'}
          variant="soft"
          size="sm"
        >
          {latest.condition.toUpperCase()}
        </Chip>
        <span className="text-[10px] tracking-widest font-bold uppercase text-vault-on-surface-variant">
          {latest.sample_count} sales
        </span>
      </div>
      <p className="font-display text-2xl font-extrabold text-vault-on-surface tabular-nums">
        {fmt$(latest.price_median)}
      </p>
      <div className="mt-2 flex justify-between text-xs text-vault-on-surface-variant">
        <span>min {fmt$(latest.price_min)}</span>
        <span>max {fmt$(latest.price_max)}</span>
      </div>
    </Card>
  )
}

interface Props {
  prices: PriceSnapshot[]
  searchQuery?: string
}

export default function PriceSection({ prices, searchQuery }: Props) {
  const [range, setRange] = useState<RangeKey>('6M')
  // Reference time for the range filter, captured once (render must stay pure).
  const [now] = useState(() => Date.now())

  const cibSnaps = useMemo(() => prices.filter((p) => p.condition === 'cib'), [prices])
  const looseSnaps = useMemo(() => prices.filter((p) => p.condition === 'loose'), [prices])
  const newSnaps = useMemo(() => prices.filter((p) => p.condition === 'new'), [prices])

  const stats = useMemo(() => {
    if (prices.length === 0) return { lowestAsk: 0, averageSale: 0, ath: 0, lowestDelta: '0.0%' }
    const allMin = prices.map((p) => p.price_min).filter((n) => n > 0)
    const allMax = prices.map((p) => p.price_max).filter((n) => n > 0)
    const allMed = prices.map((p) => p.price_median).filter((n) => n > 0)
    const lowestAsk = allMin.length ? Math.min(...allMin) : 0
    const averageSale = allMed.length ? allMed.reduce((a, b) => a + b, 0) / allMed.length : 0
    const ath = allMax.length ? Math.max(...allMax) : 0
    const sortedCib = [...cibSnaps].sort((a, b) => new Date(a.scraped_at).getTime() - new Date(b.scraped_at).getTime())
    let lowestDelta = '0.0%'
    if (sortedCib.length >= 2) {
      const a = sortedCib[sortedCib.length - 2].price_median
      const b = sortedCib[sortedCib.length - 1].price_median
      if (a > 0) {
        const pct = ((b - a) / a) * 100
        lowestDelta = `${pct >= 0 ? '+' : ''}${pct.toFixed(1)}%`
      }
    }
    return { lowestAsk, averageSale, ath, lowestDelta }
  }, [prices, cibSnaps])

  const multiChartData: MultiSeriesPoint[] = useMemo(() => {
    if (prices.length === 0) return []
    const days = RANGES.find((r) => r.key === range)?.days ?? Number.POSITIVE_INFINITY
    const cutoff = now - days * 24 * 60 * 60 * 1000
    const byDay = new Map<string, MultiSeriesPoint>()
    for (const p of prices) {
      const ts = new Date(p.scraped_at).getTime()
      if (ts < cutoff) continue
      const day = new Date(p.scraped_at).toISOString().slice(0, 10)
      const b = byDay.get(day) ?? { label: fmtDate(day) }
      b[p.condition] = p.price_median
      byDay.set(day, b)
    }
    return Array.from(byDay.entries()).sort(([a], [b]) => a.localeCompare(b)).map(([, b]) => b)
  }, [prices, range, now])

  const marketListings = useMemo(() => {
    const bySource = new Map<string, PriceSnapshot>()
    const sorted = [...prices].sort((a, b) => new Date(b.scraped_at).getTime() - new Date(a.scraped_at).getTime())
    for (const p of sorted) {
      const k = (p.source || 'ebay').toLowerCase()
      if (!bySource.has(k)) bySource.set(k, p)
    }
    return Array.from(bySource.entries()).slice(0, 4)
  }, [prices])

  if (prices.length === 0) {
    return (
      <div className="rounded-xl border border-vault-outline-variant/30 p-6 text-center">
        <p className="text-sm text-vault-on-surface-variant">Aucune donnée de prix. Lance un scrape eBay pour récupérer les prix.</p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {/* KPI */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Stat
          label="Lowest Ask"
          value={stats.lowestAsk > 0 ? fmt$(stats.lowestAsk) : '—'}
          delta={stats.lowestDelta !== '0.0%' ? { value: stats.lowestDelta, positive: !stats.lowestDelta.startsWith('-') } : undefined}
          hint={`From last ${prices.length} snapshots`}
          icon="trending_down"
        />
        <Stat
          label="Average Sale"
          value={stats.averageSale > 0 ? fmt$(stats.averageSale) : '—'}
          hint={`Based on ${prices.reduce((acc, p) => acc + p.sample_count, 0)} sales`}
          icon="bar_chart"
        />
        <Stat
          label="All-Time High"
          value={stats.ath > 0 ? fmt$(stats.ath) : '—'}
          hint={prices.length > 0 ? `Recorded ${new Date(Math.max(...prices.map((p) => new Date(p.scraped_at).getTime()))).toLocaleDateString('en-US', { month: 'short', year: 'numeric' })}` : '—'}
          icon="rocket_launch"
        />
      </div>

      {/* Conditions */}
      <div>
        <h2 className="text-headline-sm text-vault-on-surface mb-3">Prices by condition</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {looseSnaps.length > 0 && <ConditionPriceCard snapshots={looseSnaps} />}
          {cibSnaps.length > 0 && <ConditionPriceCard snapshots={cibSnaps} />}
          {newSnaps.length > 0 && <ConditionPriceCard snapshots={newSnaps} />}
        </div>
      </div>

      {/* Chart + Market Listings */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <Card padding="md" radius="2xl" className="lg:col-span-2">
          <Section
            title="Value Analytics"
            description="Aggregate price index across snapshots"
            action={
              <div className="flex items-center gap-1 rounded-lg bg-vault-surface-high p-1">
                {RANGES.map((r) => (
                  <button
                    key={r.key}
                    onClick={() => setRange(r.key)}
                    className={[
                      'px-3 h-7 rounded-md text-xs font-bold tabular-nums transition-colors focus-ring',
                      range === r.key ? 'bg-vault-primary text-vault-on-primary' : 'text-vault-on-surface-variant hover:text-vault-on-surface',
                    ].join(' ')}
                  >
                    {r.key}
                  </button>
                ))}
              </div>
            }
          >
            {multiChartData.length === 0 ? (
              <p className="text-sm text-vault-on-surface-variant py-8 text-center">Pas assez de snapshots pour cette période.</p>
            ) : (
              <PriceChart data={[]} multiData={multiChartData} series={CONDITION_SERIES} height={280} showXAxis currency="€" />
            )}
          </Section>
        </Card>

        <Card padding="md" radius="2xl">
          <Section
            title="Market Listings"
            action={marketListings.length > 0 ? <span className="text-label-bold text-vault-primary">{marketListings.length} sources</span> : undefined}
          >
            {marketListings.map(([source, snap]) => {
              const style = SOURCE_STYLES[source] ?? { color: '#978da0', abbrev: source.slice(0, 2).toUpperCase() }
              const isEbay = source === 'ebay'
              const ebayUrl = isEbay && searchQuery
                ? `https://www.ebay.fr/sch/i.html?_nkw=${encodeURIComponent(searchQuery)}&_sacat=0`
                : undefined
              const content = (
                <div className="flex items-center gap-3 py-3 border-b border-vault-outline-variant/15 last:border-0">
                  <div className="h-9 w-9 rounded-lg flex items-center justify-center text-xs font-bold text-white shrink-0" style={{ background: isEbay ? '#fff' : style.color }}>
                    {isEbay ? <img src="/logos/ebay.svg" alt="eBay" className="h-6 w-6 object-contain" /> : style.abbrev}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold text-vault-on-surface capitalize">{source}</p>
                    <p className="text-[10px] tracking-widest font-bold uppercase text-vault-on-surface-variant">{snap.condition.toUpperCase()}</p>
                  </div>
                  <div className="text-right shrink-0">
                    <p className="text-base font-bold text-vault-on-surface tabular-nums">{fmt$(snap.price_median)}</p>
                    <p className="text-[10px] text-vault-on-surface-variant">{snap.sample_count} listings</p>
                  </div>
                  {ebayUrl && <MaterialIcon name="open_in_new" size={14} className="text-vault-on-surface-variant" />}
                </div>
              )
              return ebayUrl
                ? <a key={source} href={ebayUrl} target="_blank" rel="noopener noreferrer" className="block no-underline hover:bg-vault-surface-low/40 rounded-lg px-2 -mx-2 transition-colors">{content}</a>
                : <div key={source}>{content}</div>
            })}
            <Button variant="ghost" fullWidth size="sm" rightIcon="arrow_forward" className="mt-4">
              View all sources
            </Button>
          </Section>
        </Card>
      </div>
    </div>
  )
}
