import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../AuthContext'
import type { Item, PriceSnapshot } from '../types'
import {
  Button,
  Card,
  Chip,
  EmptyState,
  IconButton,
  MaterialIcon,
  PriceChart,
  Section,
  Skeleton,
  Stat,
  useToast,
} from '../components/ui'
import type { ChartPoint, MultiSeriesPoint } from '../components/ui'
import BrandPlaceholder from '../components/BrandPlaceholder'

/* -----------------------------------------------------------------------------
 * Helpers
 * --------------------------------------------------------------------------- */

const RANGES = [
  { key: '1M', days: 30 },
  { key: '6M', days: 180 },
  { key: '1Y', days: 365 },
  { key: 'ALL', days: Number.POSITIVE_INFINITY },
] as const

type RangeKey = (typeof RANGES)[number]['key']

function fmtDate(s: string) {
  return new Date(s).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

function fmt$(n: number) {
  return n.toLocaleString('fr-FR', { style: 'currency', currency: 'EUR', minimumFractionDigits: 2 })
}

const SOURCE_STYLES: Record<string, { color: string; abbrev: string }> = {
  ebay: { color: '#E53238', abbrev: 'EB' },
  pricecharting: { color: '#3B82F6', abbrev: 'PC' },
  mercari: { color: '#FBBF24', abbrev: 'MZ' },
}

/* -----------------------------------------------------------------------------
 * Sub-components
 * --------------------------------------------------------------------------- */

function MarketListingRow({
  source,
  price,
  label,
  hint,
  searchQuery,
}: {
  source: string
  price: number
  label: string
  hint: string
  searchQuery?: string
}) {
  const style = SOURCE_STYLES[source.toLowerCase()] ?? { color: '#978da0', abbrev: source.slice(0, 2).toUpperCase() }
  const isEbay = source.toLowerCase() === 'ebay'
  const ebayUrl = searchQuery
    ? `https://www.ebay.fr/sch/i.html?_nkw=${encodeURIComponent(searchQuery)}&_sacat=0`
    : undefined

  const content = (
    <div className={`flex items-center gap-3 py-3 border-b border-vault-outline-variant/15 last:border-0 ${ebayUrl ? 'hover:bg-vault-surface-low/40 rounded-lg px-2 -mx-2 transition-colors cursor-pointer' : ''}`}>
      <div
        className="h-9 w-9 rounded-lg flex items-center justify-center text-xs font-bold text-white shrink-0 overflow-hidden"
        style={{ background: isEbay ? '#fff' : style.color }}
      >
        {isEbay ? (
          <img src="/logos/ebay.svg" alt="eBay" className="h-6 w-6 object-contain" />
        ) : (
          style.abbrev
        )}
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-semibold text-vault-on-surface capitalize">{source}</p>
        <p className="text-[10px] tracking-widest font-bold uppercase text-vault-on-surface-variant">{label}</p>
      </div>
      <div className="text-right shrink-0">
        <p className="text-base font-bold text-vault-on-surface tabular-nums">{fmt$(price)}</p>
        <p className="text-[10px] text-vault-on-surface-variant">{hint}</p>
      </div>
      {ebayUrl && (
        <span className="text-vault-on-surface-variant">
          <MaterialIcon name="open_in_new" size={14} />
        </span>
      )}
    </div>
  )

  if (ebayUrl) {
    return (
      <a href={ebayUrl} target="_blank" rel="noopener noreferrer" className="block no-underline">
        {content}
      </a>
    )
  }
  return content
}

function ConditionPriceCard({ snapshots }: { snapshots: PriceSnapshot[] }) {
  if (snapshots.length === 0) return null
  const latest = snapshots[0]
  const conditionLabel = latest.condition.toUpperCase()
  return (
    <Card padding="md" radius="xl">
      <div className="flex items-center justify-between mb-2">
        <Chip
          tone={latest.condition === 'new' ? 'success' : latest.condition === 'cib' ? 'primary' : 'info'}
          variant="soft"
          size="sm"
        >
          {conditionLabel}
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

/* -----------------------------------------------------------------------------
 * Main
 * --------------------------------------------------------------------------- */

export default function ItemDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const toast = useToast()
  const { user, isAdmin } = useAuth()

  const [item, setItem] = useState<Item | null>(null)
  const [prices, setPrices] = useState<PriceSnapshot[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [range, setRange] = useState<RangeKey>('6M')
  const [adding, setAdding] = useState(false)
  const [inCollection, setInCollection] = useState(false)
  const [savingCover, setSavingCover] = useState(false)
  const coverInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (!id) return
    setLoading(true)
    Promise.all([api.items.get(id), api.items.prices(id)])
      .then(([i, p]) => {
        setItem(i)
        setPrices(p)
      })
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false))
  }, [id])

  // Probe whether the item is already in the user's collection.
  useEffect(() => {
    if (!user || !id) return
    api.collection
      .list()
      .then((items) => setInCollection(items.some((i) => i.id === id)))
      .catch(() => {})
  }, [user, id])

  const cibSnaps = useMemo(() => prices.filter((p) => p.condition === 'cib'), [prices])
  const looseSnaps = useMemo(() => prices.filter((p) => p.condition === 'loose'), [prices])
  const newSnaps = useMemo(() => prices.filter((p) => p.condition === 'new'), [prices])

  const stats = useMemo(() => {
    if (prices.length === 0) {
      return { lowestAsk: 0, averageSale: 0, ath: 0, lowestDelta: '0.0%' }
    }
    const allMin = prices.map((p) => p.price_min).filter((n) => n > 0)
    const allMax = prices.map((p) => p.price_max).filter((n) => n > 0)
    const allMed = prices.map((p) => p.price_median).filter((n) => n > 0)
    const lowestAsk = allMin.length ? Math.min(...allMin) : 0
    const averageSale = allMed.length ? allMed.reduce((a, b) => a + b, 0) / allMed.length : 0
    const ath = allMax.length ? Math.max(...allMax) : 0
    // Delta: compare last two CIB snapshots if available
    const sortedCib = [...cibSnaps].sort(
      (a, b) => new Date(a.scraped_at).getTime() - new Date(b.scraped_at).getTime(),
    )
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

  const CONDITION_SERIES = [
    { key: 'loose', label: 'Loose', color: '#60A5FA' },
    { key: 'cib', label: 'CIB', color: '#d8b9ff' },
    { key: 'new', label: 'New', color: '#34D399' },
  ]

  const multiChartData: MultiSeriesPoint[] = useMemo(() => {
    if (prices.length === 0) return []
    const days = RANGES.find((r) => r.key === range)?.days ?? Number.POSITIVE_INFINITY
    const cutoff = Date.now() - days * 24 * 60 * 60 * 1000
    const byDay = new Map<string, MultiSeriesPoint>()
    for (const p of prices) {
      const ts = new Date(p.scraped_at).getTime()
      if (ts < cutoff) continue
      const day = new Date(p.scraped_at).toISOString().slice(0, 10)
      const b = byDay.get(day) ?? { label: fmtDate(day) }
      b[p.condition] = p.price_median
      byDay.set(day, b)
    }
    return Array.from(byDay.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([, b]) => b)
  }, [prices, range])

  // Keep chartData for backward compat (unused but type-safe)
  const chartData: ChartPoint[] = []
  void chartData

  /** Latest snapshot per source, for the right-side market listings. */
  const marketListings = useMemo(() => {
    const bySource = new Map<string, PriceSnapshot>()
    const sorted = [...prices].sort(
      (a, b) => new Date(b.scraped_at).getTime() - new Date(a.scraped_at).getTime(),
    )
    for (const p of sorted) {
      const k = (p.source || 'ebay').toLowerCase()
      if (!bySource.has(k)) bySource.set(k, p)
    }
    return Array.from(bySource.entries()).slice(0, 4)
  }, [prices])

  async function handleUploadCover(file: File) {
    if (!item) return
    setSavingCover(true)
    try {
      const { cover_url } = await api.items.uploadCover(item.id, file)
      setItem({ ...item, cover_url })
      toast.push('Cover mise à jour', 'success')
    } catch (e) {
      toast.push(`Erreur : ${String(e)}`, 'error')
    } finally {
      setSavingCover(false)
    }
  }

  async function handleAddToVault() {
    if (!item) return
    if (!user) {
      navigate('/login')
      return
    }
    setAdding(true)
    try {
      await api.collection.add(item.id)
      setInCollection(true)
      toast.push(`${item.title} ajouté à la vault`, 'success')
    } catch (e) {
      toast.push(`Erreur : ${String(e)}`, 'error')
    } finally {
      setAdding(false)
    }
  }

  /* ---- Render ---- */

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-72 rounded-2xl" />
        <div className="grid grid-cols-3 gap-4">
          <Skeleton variant="card" />
          <Skeleton variant="card" />
          <Skeleton variant="card" />
        </div>
      </div>
    )
  }

  if (error || !item) {
    return (
      <EmptyState
        icon="error"
        title="Item introuvable"
        description={error || 'Cet item n\'existe pas ou a été supprimé.'}
        cta={{ label: 'Retour au catalogue', icon: 'arrow_back', onClick: () => navigate('/catalogue') }}
      />
    )
  }

  const heroBg = item.cover_url
    ? `linear-gradient(135deg, rgba(15,15,18,0.85) 0%, rgba(15,15,18,0.55) 60%, rgba(15,15,18,0.95) 100%), url(${item.cover_url.replace(/t_cover_big(_2x)?/, 't_1080p')})`
    : 'linear-gradient(135deg, #1f1f22 0%, #2a2a2d 100%)'

  return (
    <div className="space-y-6 animate-fade-in-up">
      {/* Back link */}
      <button
        onClick={() => navigate(-1)}
        className="flex items-center gap-1.5 text-sm text-vault-on-surface-variant hover:text-vault-primary transition-colors focus-ring rounded"
      >
        <MaterialIcon name="arrow_back" size={16} />
        <span>Retour</span>
      </button>

      {/* Hero */}
      <div
        className="relative rounded-2xl overflow-hidden border border-vault-outline-variant/30"
        style={{
          background: heroBg,
          backgroundSize: 'cover',
          backgroundPosition: 'center',
        }}
      >
        <div className="relative p-8 md:p-12 flex gap-8 items-start">
          {/* Vignette cover */}
          <div className="relative shrink-0 w-32 h-40 rounded-xl overflow-hidden border border-vault-outline-variant/30 hidden md:block group">
            {item.cover_url ? (
              <img src={item.cover_url} alt={item.title} className="w-full h-full object-cover" />
            ) : (
              <BrandPlaceholder platform={item.platform} className="w-full h-full" />
            )}
            {isAdmin && (
              <button
                onClick={() => coverInputRef.current?.click()}
                className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-white text-xs font-medium"
              >
                ✏️
              </button>
            )}
          </div>

          <div className="flex-1 min-w-0">
          <div className="flex flex-wrap gap-2 mb-4">
            {item.genres &&
              item.genres
                .split(',')
                .slice(0, 2)
                .map((g) => (
                  <Chip key={g} tone="neutral" variant="soft" size="sm">
                    {g.trim()}
                  </Chip>
                ))}
            <Chip tone="primary" variant="solid" size="sm" icon="local_fire_department">
              TRENDING #1
            </Chip>
          </div>
          <h1 className="text-display-lg text-vault-on-surface mb-3">{item.title}</h1>
          {item.summary && (
            <p className="max-w-2xl text-vault-on-surface-variant text-base line-clamp-4">{item.summary}</p>
          )}

          <div className="mt-6 flex flex-wrap items-center gap-4">
            <Button
              size="lg"
              leftIcon={inCollection ? 'check_circle' : 'lock'}
              onClick={handleAddToVault}
              disabled={adding || inCollection}
              variant={inCollection ? 'subtle' : 'primary'}
            >
              {inCollection ? 'In your Vault' : adding ? 'Adding…' : 'Add to Vault'}
            </Button>
            <div className="flex items-center gap-2">
              <div className="flex -space-x-2">
                {[1, 2, 3].map((i) => (
                  <div
                    key={i}
                    className="h-8 w-8 rounded-full border-2 border-vault-bg bg-gradient-to-br from-vault-primary to-vault-primary-deep flex items-center justify-center text-[10px] font-bold text-vault-on-primary"
                  >
                    +
                  </div>
                ))}
              </div>
              <span className="text-sm text-vault-on-surface-variant">
                <span className="font-bold text-vault-on-surface">In Active Vaults</span>
              </span>
            </div>
            {isAdmin && (
              <IconButton
                icon="edit"
                variant="subtle"
                aria-label="Modifier la cover"
                onClick={() => coverInputRef.current?.click()}
              />
            )}
          </div>
          </div>

          {isAdmin && (
            <input
              ref={coverInputRef}
              type="file"
              accept="image/jpeg,image/png,image/webp"
              hidden
              onChange={(e) => {
                const f = e.target.files?.[0]
                if (f) handleUploadCover(f)
                e.target.value = ''
              }}
            />
          )}
          {savingCover && (
            <p className="mt-2 text-xs text-vault-primary animate-pulse">Upload en cours…</p>
          )}
        </div>
      </div>

      {/* KPI cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Stat
          label="Lowest Ask"
          value={stats.lowestAsk > 0 ? fmt$(stats.lowestAsk) : '—'}
          delta={
            stats.lowestDelta !== '0.0%'
              ? { value: stats.lowestDelta, positive: !stats.lowestDelta.startsWith('-') }
              : undefined
          }
          hint={prices.length > 0 ? `From last ${prices.length} snapshots` : 'No data yet'}
          icon="trending_down"
        />
        <Stat
          label="Average Sale"
          value={stats.averageSale > 0 ? fmt$(stats.averageSale) : '—'}
          hint={
            prices.length > 0
              ? `Based on ${prices.reduce((acc, p) => acc + p.sample_count, 0)} sales`
              : 'No sales recorded'
          }
          icon="bar_chart"
        />
        <Stat
          label="All-Time High"
          value={stats.ath > 0 ? fmt$(stats.ath) : '—'}
          hint={
            prices.length > 0
              ? `Recorded ${new Date(
                  Math.max(...prices.map((p) => new Date(p.scraped_at).getTime())),
                ).toLocaleDateString('en-US', { month: 'short', year: 'numeric' })}`
              : '—'
          }
          icon="rocket_launch"
        />
      </div>

      {/* Conditions detail */}
      {prices.length > 0 && (
        <div>
          <h2 className="text-headline-sm text-vault-on-surface mb-3">Prices by condition</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {looseSnaps.length > 0 && <ConditionPriceCard snapshots={looseSnaps} />}
            {cibSnaps.length > 0 && <ConditionPriceCard snapshots={cibSnaps} />}
            {newSnaps.length > 0 && <ConditionPriceCard snapshots={newSnaps} />}
          </div>
        </div>
      )}

      {/* Value Analytics + Market Listings */}
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
                      range === r.key
                        ? 'bg-vault-primary text-vault-on-primary'
                        : 'text-vault-on-surface-variant hover:text-vault-on-surface',
                    ].join(' ')}
                  >
                    {r.key}
                  </button>
                ))}
              </div>
            }
          >
            {multiChartData.length === 0 ? (
              <p className="text-sm text-vault-on-surface-variant py-8 text-center">
                Pas encore assez de snapshots pour cette période.
              </p>
            ) : (
              <PriceChart data={[]} multiData={multiChartData} series={CONDITION_SERIES} height={280} showXAxis currency="€" />
            )}
          </Section>
        </Card>

        <Card padding="md" radius="2xl">
          <Section
            title="Market Listings"
            action={
              marketListings.length > 0 ? (
                <span className="text-label-bold text-vault-primary">{marketListings.length} sources</span>
              ) : undefined
            }
          >
            {marketListings.length === 0 ? (
              <p className="text-sm text-vault-on-surface-variant py-4">
                Aucune annonce live. Lance un scrape pour rafraîchir.
              </p>
            ) : (
              <div>
                {marketListings.map(([source, snap]) => (
                  <MarketListingRow
                    key={source}
                    source={source}
                    price={snap.price_median}
                    label={snap.condition.toUpperCase()}
                    hint={`${snap.sample_count} listings`}
                    searchQuery={`${item.title} ${item.platform || ''}`}
                  />
                ))}
              </div>
            )}
            <Button variant="ghost" fullWidth size="sm" rightIcon="arrow_forward" className="mt-4">
              View all sources
            </Button>
          </Section>
        </Card>
      </div>
    </div>
  )
}
