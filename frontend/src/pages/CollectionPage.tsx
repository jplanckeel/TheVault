import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Item, PriceSnapshot } from '../types'
import {
  Button,
  Chip,
  EmptyState,
  GameCard,
  Select,
  Skeleton,
} from '../components/ui'
import CsvImportModal from '../components/CsvImportModal'

type SortMode = 'value-desc' | 'value-asc' | 'recent' | 'title'

const SORT_OPTIONS = [
  { value: 'value-desc', label: 'Sort: Value (High to Low)' },
  { value: 'value-asc', label: 'Sort: Value (Low to High)' },
  { value: 'recent', label: 'Sort: Most Recent' },
  { value: 'title', label: 'Sort: Title (A→Z)' },
]

function bestPrice(itemId: string, condition: string, priceMap: Map<string, number>): number {
  return (
    priceMap.get(`${itemId}:${condition}`) ??
    priceMap.get(`${itemId}:cib`) ??
    priceMap.get(`${itemId}:loose`) ??
    priceMap.get(`${itemId}:new`) ??
    0
  )
}

export default function CollectionPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<Item[]>([])
  const [snapshots, setSnapshots] = useState<PriceSnapshot[]>([])
  const [loading, setLoading] = useState(true)
  const [activePlatform, setActivePlatform] = useState<string>('all')
  const [activeCondition, setActiveCondition] = useState<string>('all')
  const [sort, setSort] = useState<SortMode>('value-desc')
  const [csvOpen, setCsvOpen] = useState(false)

  function loadCollection() {
    return Promise.all([api.collection.list(), api.prices.latest()])
      .then(([c, s]) => {
        setItems(c)
        setSnapshots(s)
      })
      .catch(() => {})
  }

  useEffect(() => {
    loadCollection().finally(() => setLoading(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const priceMap = useMemo(() => {
    const m = new Map<string, number>()
    for (const s of snapshots) m.set(`${s.item_id}:${s.condition}`, s.price_median)
    return m
  }, [snapshots])

  const platforms = useMemo(() => {
    const set = new Set<string>()
    for (const i of items) if (i.platform) set.add(i.platform)
    return Array.from(set).slice(0, 6) // top 6 to keep the chip row readable
  }, [items])

  const conditions = useMemo(() => {
    const set = new Set<string>()
    for (const i of items) if (i.condition) set.add(i.condition)
    return Array.from(set)
  }, [items])

  const filtered = useMemo(() => {
    let xs = items
    if (activePlatform !== 'all') xs = xs.filter((i) => i.platform === activePlatform)
    if (activeCondition !== 'all') xs = xs.filter((i) => i.condition === activeCondition)
    return [...xs].sort((a, b) => {
      const pa = bestPrice(a.id, a.condition, priceMap)
      const pb = bestPrice(b.id, b.condition, priceMap)
      switch (sort) {
        case 'value-desc':
          return pb - pa
        case 'value-asc':
          return pa - pb
        case 'recent':
          return new Date(b.created_at).getTime() - new Date(a.created_at).getTime()
        case 'title':
          return a.title.localeCompare(b.title)
      }
    })
  }, [items, activePlatform, activeCondition, sort, priceMap])

  const totalValue = useMemo(
    () => items.reduce((acc, i) => acc + bestPrice(i.id, i.condition, priceMap), 0),
    [items, priceMap],
  )

  /* ---- Render ---- */

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton variant="text" lines={2} className="max-w-md" />
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {Array.from({ length: 10 }).map((_, i) => (
            <Skeleton key={i} className="aspect-[3/4]" />
          ))}
        </div>
      </div>
    )
  }

  return (
    <>
    <div className="space-y-6 animate-fade-in-up">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4">
        <div>
          <h1 className="text-display-lg text-vault-on-surface">My Collection</h1>
          <p className="mt-2 text-vault-on-surface-variant">
            <span className="font-bold text-vault-on-surface tabular-nums">{items.length.toLocaleString()}</span>{' '}
            Items in your Vault
            <span className="mx-2 opacity-40">•</span>
            <span className="font-bold text-vault-primary tabular-nums">
              {totalValue.toLocaleString('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 })}
            </span>{' '}
            Est. Value
          </p>
        </div>
        <div className="flex gap-2 shrink-0 flex-wrap">
          <Button variant="ghost" leftIcon="upload_file" size="md" onClick={() => setCsvOpen(true)}>
            Import CSV
          </Button>
          <Button variant="ghost" leftIcon="filter_list" size="md">
            Advanced Filters
          </Button>
          <Select
            value={sort}
            onChange={(e) => setSort(e.target.value as SortMode)}
            options={SORT_OPTIONS}
            size="md"
            className="w-64"
          />
        </div>
      </div>

      {/* Filters */}
      {items.length > 0 && (
        <div className="flex flex-wrap items-center gap-2">
          <Chip
            tone={activePlatform === 'all' ? 'primary' : 'neutral'}
            variant={activePlatform === 'all' ? 'solid' : 'soft'}
            onClick={() => setActivePlatform('all')}
          >
            All Items
          </Chip>
          {platforms.map((p) => (
            <Chip
              key={p}
              tone={activePlatform === p ? 'primary' : 'neutral'}
              variant={activePlatform === p ? 'solid' : 'soft'}
              onClick={() => setActivePlatform(p)}
            >
              {p}
            </Chip>
          ))}
          {conditions.length > 0 && (
            <span className="mx-2 h-5 w-px bg-vault-outline-variant/40" />
          )}
          <Chip
            tone={activeCondition === 'all' ? 'secondary' : 'neutral'}
            variant={activeCondition === 'all' ? 'solid' : 'soft'}
            onClick={() => setActiveCondition('all')}
          >
            Any condition
          </Chip>
          {conditions.map((c) => (
            <Chip
              key={c}
              tone={activeCondition === c ? 'secondary' : 'neutral'}
              variant={activeCondition === c ? 'solid' : 'soft'}
              onClick={() => setActiveCondition(c)}
            >
              {c.toUpperCase()}
            </Chip>
          ))}
        </div>
      )}

      {/* Grid */}
      {items.length === 0 ? (
        <div className="space-y-4">
          <EmptyState
            icon="inventory_2"
            title="Ta collection est vide"
            description="Importe ton CSV GameEye, ou ajoute manuellement ton premier item depuis IGDB."
            cta={{ label: 'Import GameEye CSV', icon: 'upload_file', onClick: () => setCsvOpen(true) }}
          />
          <p className="text-center text-sm text-vault-on-surface-variant">
            ou{' '}
            <button
              onClick={() => navigate('/add')}
              className="text-vault-primary font-semibold hover:underline focus-ring rounded"
            >
              ajouter manuellement →
            </button>
          </p>
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState
          icon="filter_alt_off"
          title="Aucun résultat"
          description="Aucun item ne correspond à ces filtres."
          cta={{
            label: 'Réinitialiser les filtres',
            icon: 'restart_alt',
            onClick: () => {
              setActivePlatform('all')
              setActiveCondition('all')
            },
          }}
        />
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

      <CsvImportModal
        open={csvOpen}
        onClose={() => setCsvOpen(false)}
        onComplete={() => loadCollection()}
      />
    </div>
    </>
  )
}
