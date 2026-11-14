import { useMemo, useState, type ReactNode } from 'react'
import { useSearchParams } from 'react-router-dom'
import type { Item } from '../types'
import { CollectionCard, EmptyState, FilterChip, Input, PageHeader, PlatformTile, Section, SegmentedControl, Skeleton } from './ui'
import { buildCollectionView, ITEM_TYPES, SORT_MODES, type ItemType, type SortMode } from '../lib/collectionView'
import { bestPrice, totalValue, type PriceMap } from '../lib/prices'
import { formatEuro, formatEuroOrDash } from '../lib/format'
import { parseCondition } from '../lib/condition'
import { itemPath } from '../lib/routes'

interface Props {
  title: string
  eyebrow: string
  items: Item[]
  prices: PriceMap
  loading: boolean
  /** Empty state when the list itself is empty (not because of filters). */
  empty: { title: string; description: string; cta?: { label: string; icon?: string; onClick: () => void } }
  actions?: ReactNode
}

/** `?platform=*` shows every item of the type at once. */
const ALL = '*'

function parseType(value: string | null): ItemType {
  return ITEM_TYPES.some((t) => t.value === value) ? (value as ItemType) : 'game'
}

const UNIT: Record<ItemType, string> = { game: 'jeux', console: 'consoles', accessory: 'accessoires' }

/**
 * Collection, Envies and Explorer: type selector, then platform rows (as on mobile),
 * then the cartridge grid of a platform. Type, platform and search live in the URL.
 */
export default function ItemGridView({ title, eyebrow, items, prices, loading, empty, actions }: Props) {
  const [params, setParams] = useSearchParams()
  const type = parseType(params.get('type'))
  const platform = params.get('platform')
  const query = params.get('q') ?? ''
  const [sort, setSort] = useState<SortMode>('recent')

  const view = useMemo(
    () => buildCollectionView(items, { type, query, platform: platform && platform !== ALL ? platform : null, sort }, prices),
    [items, type, query, platform, sort, prices],
  )

  function update(changes: Record<string, string | null>) {
    setParams(
      (p) => {
        for (const [k, v] of Object.entries(changes)) {
          if (v === null || v === '') p.delete(k)
          else p.set(k, v)
        }
        return p
      },
      { replace: 'q' in changes },
    )
  }

  if (loading) return <GridSkeleton />

  const total = totalValue(items, prices)
  const header = (
    <PageHeader
      eyebrow={eyebrow}
      title={title}
      subtitle={
        <span className="tv-price font-medium">
          {items.length.toLocaleString('fr-FR')} objets{total > 0 ? ` · ${formatEuro(total)}` : ''}
        </span>
      }
      actions={actions}
    />
  )

  if (items.length === 0) {
    return (
      <div className="tv-enter">
        {header}
        <EmptyState title={empty.title} description={empty.description} cta={empty.cta} />
      </div>
    )
  }

  const showGrid = platform !== null || query.trim() !== ''
  const unit = UNIT[type]
  return (
    <div className="tv-enter flex flex-col gap-5">
      {header}
      <SegmentedControl
        label="Type d'objet"
        options={ITEM_TYPES.map((t) => ({ value: t.value, label: `${t.label} · ${view.countByType[t.value]}` }))}
        value={type}
        onChange={(t) => update({ type: t, platform: null })}
        className="md:max-w-xl"
      />
      <div className="flex flex-col lg:flex-row gap-3 lg:items-center">
        <Input
          className="lg:max-w-md"
          leftIcon="search"
          placeholder={`Rechercher dans les ${ITEM_TYPES.find((t) => t.value === type)?.label.toLowerCase()}…`}
          value={query}
          onChange={(e) => update({ q: e.target.value })}
          aria-label="Rechercher"
        />
        {showGrid && (
          <div className="flex gap-2 overflow-x-auto scrollbar-none p-1 -m-1">
            {SORT_MODES.map((s) => (
              <FilterChip key={s.value} label={s.label} selected={sort === s.value} onClick={() => setSort(s.value)} />
            ))}
          </div>
        )}
      </div>

      {!showGrid ? (
        <Section
          title="Plateformes"
          action={
            <button type="button" className="tv-link" onClick={() => update({ platform: ALL })}>
              Tout afficher
            </button>
          }
        >
          {view.platformSummaries.length === 0 ? (
            <EmptyState title={`Aucun objet de ce type`} description="Choisis un autre type ou ajoute un objet." />
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
              {view.platformSummaries.map((p) => (
                <PlatformTile
                  key={p.platform}
                  platform={p.platform}
                  count={p.count}
                  unit={p.count > 1 ? unit : unit.replace(/s$/, '')}
                  value={p.value > 0 ? formatEuro(p.value) : undefined}
                  onClick={() => update({ platform: p.platform })}
                />
              ))}
            </div>
          )}
        </Section>
      ) : (
        <Section
          title={platform && platform !== ALL ? platform : query.trim() ? `Résultats` : `Tous les ${unit}`}
          description={`${view.items.length.toLocaleString('fr-FR')} ${unit}`}
          action={
            <button type="button" className="tv-link" onClick={() => update({ platform: null, q: null })}>
              ← Plateformes
            </button>
          }
        >
          {view.items.length === 0 ? (
            <EmptyState
              title="Aucun résultat"
              description="Essaie une autre recherche ou une autre plateforme."
              cta={{ label: 'Réinitialiser', icon: 'restart_alt', onClick: () => update({ platform: null, q: null }) }}
            />
          ) : (
            <div className="grid grid-cols-3 sm:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-x-3 gap-y-5">
              {view.items.map((item) => (
                <CollectionCard
                  key={item.id}
                  to={itemPath(item)}
                  title={item.title}
                  platform={item.platform}
                  src={item.cover_url || undefined}
                  condition={parseCondition(item.condition)}
                  value={formatEuroOrDash(bestPrice(item, prices))}
                />
              ))}
            </div>
          )}
        </Section>
      )}
    </div>
  )
}

function GridSkeleton() {
  return (
    <div className="flex flex-col gap-5">
      <Skeleton variant="text" lines={2} className="max-w-xs" />
      <Skeleton className="h-11 max-w-xl" />
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-16 rounded-lg" />
        ))}
      </div>
    </div>
  )
}
