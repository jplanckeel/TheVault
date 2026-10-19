import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api'
import type { Item, PriceSnapshot } from '../types'
import {
  Card,
  Chip,
  EmptyState,
  GameCard,
  Input,
  Section,
  Skeleton,
} from '../components/ui'

const CATEGORIES = [
  { key: 'game', label: 'Jeux', icon: 'sports_esports' },
  { key: 'console', label: 'Consoles', icon: 'memory' },
  { key: 'accessory', label: 'Accessoires', icon: 'headphones' },
] as const

function bestPrice(itemId: string, condition: string, priceMap: Map<string, number>): number {
  return (
    priceMap.get(`${itemId}:${condition}`) ??
    priceMap.get(`${itemId}:cib`) ??
    priceMap.get(`${itemId}:loose`) ??
    priceMap.get(`${itemId}:new`) ??
    0
  )
}

function CategorySummary({
  icon,
  label,
  count,
  total,
  active,
  onClick,
}: {
  icon: string
  label: string
  count: number
  total: number
  active: boolean
  onClick: () => void
}) {
  return (
    <Card
      onClick={onClick}
      padding="md"
      radius="2xl"
      className={[
        'flex-1 min-w-[180px] cursor-pointer transition-all',
        active
          ? 'border-vault-primary glow-purple'
          : 'hover:border-vault-primary/40 hover:-translate-y-0.5',
      ].join(' ')}
    >
      <div className="flex items-center gap-2 mb-3">
        <div className="h-8 w-8 rounded-lg bg-vault-primary/15 text-vault-primary flex items-center justify-center">
          <span className="material-symbols-outlined" style={{ fontSize: 18 }}>
            {icon}
          </span>
        </div>
        <span className="text-label-bold text-vault-on-surface-variant">{label}</span>
      </div>
      <div className="font-display text-2xl font-extrabold text-vault-on-surface tabular-nums">
        {total > 0
          ? total.toLocaleString('en-US', {
              style: 'currency',
              currency: 'EUR',
              maximumFractionDigits: 0,
            })
          : '—'}
      </div>
      <p className="text-xs text-vault-on-surface-variant mt-1">
        {count} item{count > 1 ? 's' : ''}
      </p>
    </Card>
  )
}

export default function CataloguePage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [items, setItems] = useState<Item[]>([])
  const [snapshots, setSnapshots] = useState<PriceSnapshot[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState(searchParams.get('q') || '')
  const [activeType, setActiveType] = useState<string>('all')

  useEffect(() => {
    Promise.all([api.items.list(), api.prices.latest()])
      .then(([is, ss]) => {
        setItems(is)
        setSnapshots(ss)
      })
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false))
  }, [])

  const priceMap = useMemo(() => {
    const m = new Map<string, number>()
    for (const s of snapshots) m.set(`${s.item_id}:${s.condition}`, s.price_median)
    return m
  }, [snapshots])

  const totals = useMemo(() => {
    const t: Record<string, { count: number; sum: number }> = {}
    for (const c of CATEGORIES) t[c.key] = { count: 0, sum: 0 }
    for (const i of items) {
      const e = t[i.type]
      if (!e) continue
      e.count += 1
      e.sum += bestPrice(i.id, i.condition, priceMap)
    }
    return t
  }, [items, priceMap])

  const q = query.trim().toLowerCase()
  const filtered = useMemo(() => {
    let xs = items
    if (activeType !== 'all') xs = xs.filter((i) => i.type === activeType)
    if (q) {
      xs = xs.filter(
        (i) =>
          i.title.toLowerCase().includes(q) ||
          (i.platform || '').toLowerCase().includes(q) ||
          (i.genres || '').toLowerCase().includes(q),
      )
    }
    return xs
  }, [items, activeType, q])

  function updateQuery(v: string) {
    setQuery(v)
    if (v.trim()) setSearchParams({ q: v.trim() })
    else setSearchParams({})
  }

  /* ---- Render ---- */

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton variant="text" lines={2} className="max-w-md" />
        <div className="flex gap-4">
          <Skeleton variant="card" className="flex-1" />
          <Skeleton variant="card" className="flex-1" />
          <Skeleton variant="card" className="flex-1" />
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <Card padding="md" className="border-vault-error/40">
        <p className="text-vault-error text-sm">Erreur : {error}</p>
      </Card>
    )
  }

  return (
    <div className="space-y-6 animate-fade-in-up">
      <div>
        <p className="text-label-bold text-vault-primary mb-2">Marketplace</p>
        <h1 className="text-display-lg text-vault-on-surface">Catalogue</h1>
        <p className="mt-2 text-vault-on-surface-variant">
          Toute la base de données : {items.length.toLocaleString()} items.
        </p>
      </div>

      {/* Summary cards */}
      {items.length > 0 && (
        <div className="flex gap-4 flex-wrap">
          <CategorySummary
            icon="grid_view"
            label="All Items"
            count={items.length}
            total={Object.values(totals).reduce((a, b) => a + b.sum, 0)}
            active={activeType === 'all'}
            onClick={() => setActiveType('all')}
          />
          {CATEGORIES.map(({ key, label, icon }) => (
            <CategorySummary
              key={key}
              icon={icon}
              label={label}
              count={totals[key].count}
              total={totals[key].sum}
              active={activeType === key}
              onClick={() => setActiveType(key)}
            />
          ))}
        </div>
      )}

      {/* Search */}
      <Input
        leftIcon="search"
        placeholder="Search games, consoles, accessories..."
        value={query}
        onChange={(e) => updateQuery(e.target.value)}
        size="lg"
        glass
        rightAdornment={
          query ? (
            <button
              onClick={() => updateQuery('')}
              className="text-vault-on-surface-variant hover:text-vault-on-surface px-2 transition-colors"
              aria-label="Clear"
            >
              <span className="material-symbols-outlined" style={{ fontSize: 18 }}>
                close
              </span>
            </button>
          ) : undefined
        }
      />

      {q && (
        <p className="text-sm text-vault-on-surface-variant">
          {filtered.length} résultat{filtered.length > 1 ? 's' : ''} pour
          <Chip className="ml-2" tone="primary">{q}</Chip>
        </p>
      )}

      {/* Results */}
      {items.length === 0 ? (
        <EmptyState
          icon="storefront"
          title="Aucun item dans le catalogue"
          description="Lance un scrape eBay ou un sync IGDB pour commencer."
        />
      ) : filtered.length === 0 ? (
        <EmptyState
          icon="search_off"
          title="Aucun résultat"
          description={`Aucun item ne correspond à « ${q} ».`}
          cta={{ label: 'Effacer la recherche', icon: 'restart_alt', onClick: () => updateQuery('') }}
        />
      ) : activeType === 'all' && !q ? (
        // Show by sections when no filter
        <div className="space-y-10">
          {CATEGORIES.map(({ key, label, icon }) => {
            const section = items.filter((i) => i.type === key)
            if (section.length === 0) return null
            return (
              <Section
                key={key}
                title={
                  <span className="flex items-center gap-2">
                    <span className="material-symbols-outlined text-vault-primary" style={{ fontSize: 22 }}>
                      {icon}
                    </span>
                    {label}
                    <span className="text-vault-on-surface-variant text-sm font-normal ml-1">
                      ({section.length})
                    </span>
                  </span>
                }
                action={
                  <button
                    onClick={() => setActiveType(key)}
                    className="text-sm font-semibold text-vault-primary hover:text-vault-primary-dim transition-colors"
                  >
                    View all →
                  </button>
                }
              >
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
                  {section.slice(0, 5).map((item) => (
                    <GameCard
                      key={item.id}
                      item={item}
                      price={bestPrice(item.id, item.condition, priceMap) || undefined}
                      onClick={() => navigate(`/items/${item.id}`)}
                    />
                  ))}
                </div>
              </Section>
            )
          })}
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {filtered.map((item) => (
            <GameCard
              key={item.id}
              item={item}
              price={bestPrice(item.id, item.condition, priceMap) || undefined}
              onClick={() => navigate(`/items/${item.id}`)}
            />
          ))}
        </div>
      )}
    </div>
  )
}
