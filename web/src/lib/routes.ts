import type { Item } from '../types'

/** Detail route of an item: consoles and accessories have dedicated pages. */
export function itemPath(item: Pick<Item, 'id' | 'type'>): string {
  if (item.type === 'console') return `/consoles/${item.id}`
  if (item.type === 'accessory') return `/accessories/${item.id}`
  return `/items/${item.id}`
}
