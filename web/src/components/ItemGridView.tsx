import { useMemo, useState, type ReactNode } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import type { Item } from '../types'
import { EmptyState, FilterChip, GameCard, Input, Skeleton } from './ui'
import { buildCollectionView, ITEM_TYPES, SORT_MODES, type ItemType, type SortMode } from '../lib/collectionView'
import { bestPrice, totalValue, type PriceMap } from '../lib/prices'
import { formatEuro } from '../lib/format'
import { itemPath } from '../lib/routes'

interface Props {
  title: string
  items: Item[]
  prices: PriceMap
  loading: boolean
  /** Empty state when the list itself is empty (not because of filters). */
  empty: { icon: string; title: string; description: string; cta?: { label: string; icon?: string; onClick: () => void } }
  /** Buttons shown on the right of the header. */
  actions?: ReactNode
  /** Shows the wishlist star instead of the condition badge. */
  wishlist?: boolean
}

function parseType(value: string | null): ItemType {
  return ITEM_TYPES.some((t) => t.value === value) ? (value as ItemType) : 'game'
}

/**
 * Filterable grid shared by Collection, Wishlist and Explorer — same layout and
 * filters as the mobile Collection screen. The selected type lives in `?type=`.
 */
export default function ItemGridView({ title, items, prices, loading, empty, actions, wishlist }: Props) {
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const type = parseType(params.get('type'))
  const [query, setQuery] = useState(params.get('q') ?? '')
  const [platform, setPlatform] = useState<string | null>(null)
  const [sort, setSort] = useState<SortMode>('recent')
  // Header search navigates to `?q=` — keep the field in sync when already on the page.
  const urlQuery = params.get('q')
  const [syncedUrlQuery, setSyncedUrlQuery] = useState(urlQuery)
  if (urlQuery !== syncedUrlQuery) {
    setSyncedUrlQuery(urlQuery)
    if (urlQuery !== null) setQuery(urlQuery)
  }

  const view = useMemo(
    () => buildCollectionView(items, { type, query, platform, sort }, prices),
    [items, type, query, platform, sort, prices],
  )

  function selectType(next: ItemType) {
    setPlatform(null)
    setParams((p) => {
      p.set('type', next)
      return p
    })
  }

  if (loading) return <GridSkeleton />

  const filtered = query.trim() !== '' || platform !== null
  return (
    <div className="space-y-6 animate-fade-in-up">
      <header className="flex flex-col md:flex-row md:items-end md:justify-between gap-4">
        <div>
          <h1 className="text-display-lg text-vault-on-surface">{title}</h1>
          <p className="mt-1 text-vault-on-surface-variant tabular-nums">
            {items.length.toLocaleString('fr-FR')} objets · {formatEuro(totalValue(items, prices))} estimés
          </p>
        </div>
        {actions && <div className="flex gap-2 flex-wrap">{actions}</div>}
      </header>

      <div className="space-y-3">
        <div className="flex flex-wrap gap-2">
          {ITEM_TYPES.map((t) => (
            <FilterChip key={t.value} label={t.label} count={view.countByType[t.value]} selected={type === t.value} onClick={() => selectType(t.value)} />
          ))}
        </div>
        <div className="flex flex-col lg:flex-row gap-3 lg:items-center">
          <div className="w-full lg:max-w-md">
            <Input
            leftIcon="search"
            placeholder={`Rechercher dans ${ITEM_TYPES.find((t) => t.value === type)?.label.toLowerCase()}…`}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            />
          </div>
          <div className="flex gap-2 overflow-x-auto shrink-0">
            {SORT_MODES.map((s) => (
              <FilterChip key={s.value} label={s.label} selected={sort === s.value} onClick={() => setSort(s.value)} />
            ))}
          </div>
        </div>
        {view.platforms.length > 1 && (
          <div className="flex gap-2 overflow-x-auto pb-1">
            <FilterChip label="Toutes" selected={platform === null} onClick={() => setPlatform(null)} />
            {view.platforms.map((p) => (
              <FilterChip key={p} label={p} selected={platform === p} onClick={() => setPlatform(p)} />
            ))}
          </div>
        )}
      </div>

      {view.items.length === 0 ? (
        filtered ? (
          <EmptyState
            icon="filter_alt_off"
            title="Aucun résultat"
            description="Essayez une autre recherche ou plateforme."
            cta={{ label: 'Réinitialiser', icon: 'restart_alt', onClick: () => { setQuery(''); setPlatform(null) } }}
          />
        ) : (
          <EmptyState icon={empty.icon} title={empty.title} description={empty.description} cta={empty.cta} />
        )
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-5 gap-4">
          {view.items.map((item) => (
            <GameCard
              key={item.id}
              item={item}
              price={bestPrice(item, prices)}
              wishlist={wishlist}
              onClick={() => navigate(itemPath(item))}
            />
          ))}
        </div>
      )}
    </div>
  )
}

function GridSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton variant="text" lines={2} className="max-w-md" />
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-5 gap-4">
        {Array.from({ length: 10 }).map((_, i) => (
          <Skeleton key={i} className="aspect-[3/4]" />
        ))}
      </div>
    </div>
  )
}
