import type { Item } from '../types'
import { bestPrice, type PriceMap } from './prices'

/** Item category — same set as the mobile `ItemKind`. */
export type ItemType = Item['type']

/** Sort order — same set as the mobile `CollectionSort`. */
export type SortMode = 'recent' | 'value' | 'title'

export const ITEM_TYPES: { value: ItemType; label: string }[] = [
  { value: 'game', label: 'Jeux' },
  { value: 'console', label: 'Consoles' },
  { value: 'accessory', label: 'Accessoires' },
]

export const SORT_MODES: { value: SortMode; label: string }[] = [
  { value: 'recent', label: 'Récents' },
  { value: 'value', label: 'Valeur' },
  { value: 'title', label: 'A → Z' },
]

export interface CollectionFilters {
  type: ItemType
  query: string
  platform: string | null
  sort: SortMode
}

export interface CollectionView {
  /** Visible items, filtered and sorted. */
  items: Item[]
  /** Platforms of the selected type, most frequent first. */
  platforms: string[]
  /** Number of items per type, before text / platform filtering. */
  countByType: Record<ItemType, number>
}

/**
 * Applies the filters of a collection grid. Mirrors `buildCollectionView` in
 * composeApp/.../collection/CollectionModels.kt — keep both in sync.
 */
export function buildCollectionView(all: Item[], filters: CollectionFilters, prices: PriceMap): CollectionView {
  const ofType = all.filter((i) => i.type === filters.type)
  const counts = new Map<string, number>()
  for (const i of ofType) if (i.platform) counts.set(i.platform, (counts.get(i.platform) ?? 0) + 1)
  const platforms = [...counts.entries()].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0])).map(([p]) => p)

  const query = filters.query.trim().toLowerCase()
  const visible = ofType
    .filter((i) => !filters.platform || i.platform.toLowerCase() === filters.platform.toLowerCase())
    .filter((i) => !query || i.title.toLowerCase().includes(query) || i.platform.toLowerCase().includes(query))

  const byTitle = (a: Item, b: Item) => a.title.localeCompare(b.title, 'fr')
  const sorted = [...visible].sort((a, b) => {
    switch (filters.sort) {
      case 'recent':
        return new Date(b.created_at).getTime() - new Date(a.created_at).getTime() || byTitle(a, b)
      case 'value':
        return (bestPrice(b, prices) ?? -1) - (bestPrice(a, prices) ?? -1) || byTitle(a, b)
      case 'title':
        return byTitle(a, b)
    }
  })

  const countByType = { game: 0, console: 0, accessory: 0 } as Record<ItemType, number>
  for (const i of all) countByType[i.type] = (countByType[i.type] ?? 0) + 1
  return { items: sorted, platforms, countByType }
}
