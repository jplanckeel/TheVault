import type { Item, PriceSnapshot } from '../types'

/** Median price per `itemId:condition`. */
export type PriceMap = Map<string, number>

/** Indexes the latest snapshots by item and condition. */
export function buildPriceMap(snapshots: PriceSnapshot[]): PriceMap {
  const map: PriceMap = new Map()
  for (const s of snapshots) map.set(`${s.item_id}:${s.condition}`, s.price_median)
  return map
}

/**
 * Best known price for an item: its own condition first, then CIB, loose and new.
 * Returns undefined when no price is known.
 */
export function bestPrice(item: Pick<Item, 'id' | 'condition'>, prices: PriceMap): number | undefined {
  const keys = [item.condition, 'cib', 'loose', 'new'].map((c) => `${item.id}:${c}`)
  for (const key of keys) {
    const value = prices.get(key)
    if (value) return value
  }
  return undefined
}

/** Sum of the best known prices. */
export function totalValue(items: Item[], prices: PriceMap): number {
  return items.reduce((acc, item) => acc + (bestPrice(item, prices) ?? 0), 0)
}
