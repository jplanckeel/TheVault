import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Item } from '../types'

const CATEGORIES = [
  { key: 'game',      label: 'Jeux',        icon: '🎮' },
  { key: 'console',   label: 'Consoles',     icon: '🕹️' },
  { key: 'accessory', label: 'Accessoires',  icon: '🎧' },
] as const

function PageHeader() {
  return (
    <div className="flex items-start justify-between mb-8">
      <div>
        <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-2">
          Backoffice
        </p>
        <h1 className="text-2xl font-semibold text-vault-on-surface">Catalogue</h1>
      </div>
    </div>
  )
}

function SummaryCard({
  icon, label, count, total,
}: {
  icon: string; label: string; count: number; total: number
}) {
  return (
    <div className="rounded-xl border border-vault-outline-variant bg-vault-surface p-5 flex-1 min-w-[160px]">
      <div className="flex items-center gap-2 mb-3">
        <span className="text-lg">{icon}</span>
        <span className="text-xs font-semibold uppercase tracking-widest text-vault-on-surface-variant">
          {label}
        </span>
      </div>
      <p className="text-2xl font-bold text-vault-primary mb-1">
        {total.toFixed(2)} €
      </p>
      <p className="text-xs text-vault-on-surface-variant">{count} item{count > 1 ? 's' : ''}</p>
    </div>
  )
}

function ItemCard({ item, onClick }: { item: Item; onClick: () => void }) {
  return (
    <div
      onClick={onClick}
      className="bg-vault-surface border border-vault-outline-variant rounded-xl overflow-hidden cursor-pointer transition-all hover:border-vault-primary hover:shadow-[0_0_0_1px_#BA9EFF] flex gap-4 p-4"
    >
      {/* Cover thumbnail */}
      <div className="shrink-0 w-14 h-20 rounded-lg overflow-hidden bg-vault-surface-highest flex items-center justify-center">
        {item.cover_url ? (
          <img src={item.cover_url} alt={item.title} className="w-full h-full object-cover" />
        ) : (
          <span className="text-2xl opacity-30">🎮</span>
        )}
      </div>

      <div className="flex-1 min-w-0">
        <div className="flex items-start justify-between gap-2 mb-2">
          <h3 className="text-sm font-semibold text-vault-on-surface leading-snug">
            {item.title}
          </h3>
          {item.region && (
            <span className="shrink-0 text-xs px-2 py-0.5 rounded-full bg-vault-secondary-container text-vault-secondary-text font-medium">
              {item.region}
            </span>
          )}
        </div>
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-xs text-vault-on-surface-variant">{item.platform}</span>
          {item.release_year > 0 && (
            <>
              <span className="text-vault-outline text-xs">·</span>
              <span className="text-xs text-vault-on-surface-variant">{item.release_year}</span>
            </>
          )}
        </div>
        {item.genres && (
          <p className="text-xs text-vault-on-surface-variant mt-1 truncate">{item.genres}</p>
        )}
        {item.status && (
          <div className="mt-2 pt-2 border-t border-vault-outline-variant">
            <span className="text-xs text-vault-on-surface-variant">{item.status}</span>
          </div>
        )}
      </div>
    </div>
  )
}

// Returns the best price for an item given its collection condition.
// Tries to match the item's own condition first, then falls back to any available price.
function bestPrice(
  itemId: string,
  condition: string,
  priceMap: Map<string, number>,
): number {
  return priceMap.get(`${itemId}:${condition}`)
    ?? priceMap.get(`${itemId}:cib`)
    ?? priceMap.get(`${itemId}:loose`)
    ?? priceMap.get(`${itemId}:new`)
    ?? 0
}

export default function CataloguePage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<Item[]>([])
  const [priceMap, setPriceMap] = useState<Map<string, number>>(new Map())
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState('')

  useEffect(() => {
    Promise.all([api.items.list(), api.prices.latest()])
      .then(([items, snapshots]) => {
        setItems(items)
        const map = new Map<string, number>()
        for (const s of snapshots) {
          map.set(`${s.item_id}:${s.condition}`, s.price_median)
        }
        setPriceMap(map)
      })
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <p className="text-vault-on-surface-variant animate-pulse">Chargement…</p>
    </div>
  )

  if (error) return (
    <div className="rounded-xl bg-vault-surface border border-vault-error/30 p-4">
      <p className="text-vault-error text-sm">Erreur : {error}</p>
    </div>
  )

  const q = query.trim().toLowerCase()
  const filtered = q
    ? items.filter((item) =>
        item.title.toLowerCase().includes(q) ||
        item.platform.toLowerCase().includes(q) ||
        item.type.toLowerCase().includes(q)
      )
    : items

  // Totals per category
  const totals = Object.fromEntries(
    CATEGORIES.map(({ key }) => {
      const categoryItems = items.filter((i) => i.type === key)
      const sum = categoryItems.reduce(
        (acc, i) => acc + bestPrice(i.id, i.condition, priceMap),
        0,
      )
      return [key, { count: categoryItems.length, sum }]
    }),
  ) as Record<string, { count: number; sum: number }>

  return (
    <div>
      <PageHeader />

      {/* Summary cards */}
      {items.length > 0 && (
        <div className="flex gap-4 flex-wrap mb-8">
          {CATEGORIES.map(({ key, label, icon }) => (
            <SummaryCard
              key={key}
              icon={icon}
              label={label}
              count={totals[key].count}
              total={totals[key].sum}
            />
          ))}
        </div>
      )}

      {/* Search bar */}
      <div className="relative mb-6">
        <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-vault-on-surface-variant pointer-events-none">
          🔍
        </span>
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Rechercher par titre, plateforme…"
          className="w-full bg-vault-surface border border-vault-outline-variant rounded-xl pl-10 pr-4 py-2.5 text-sm text-vault-on-surface placeholder:text-vault-on-surface-variant focus:outline-none focus:border-vault-primary transition-colors"
        />
        {query && (
          <button
            onClick={() => setQuery('')}
            className="absolute right-3.5 top-1/2 -translate-y-1/2 text-vault-on-surface-variant hover:text-vault-on-surface transition-colors text-xs"
          >
            ✕
          </button>
        )}
      </div>

      {items.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-64 text-center">
          <p className="text-vault-on-surface-variant">Aucun item pour l'instant.</p>
          <p className="text-xs text-vault-outline mt-2">Lance un scrape pour commencer.</p>
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-64 text-center">
          <p className="text-vault-on-surface-variant">Aucun résultat pour « {query} ».</p>
        </div>
      ) : (
        <>
          {q && (
            <p className="text-xs text-vault-on-surface-variant mb-4">
              {filtered.length} résultat{filtered.length > 1 ? 's' : ''}
            </p>
          )}
          {CATEGORIES.map(({ key, label, icon }) => {
            const section = filtered.filter((i) => i.type === key)
            if (section.length === 0) return null
            return (
              <div key={key} className="mb-10">
                <div className="flex items-center gap-2 mb-4">
                  <span>{icon}</span>
                  <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase">
                    {label}
                  </p>
                  <span className="text-xs text-vault-outline ml-1">({section.length})</span>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {section.map((item) => (
                    <ItemCard
                      key={item.id}
                      item={item}
                      onClick={() => navigate(`/items/${item.id}`)}
                    />
                  ))}
                </div>
              </div>
            )
          })}
        </>
      )}
    </div>
  )
}
