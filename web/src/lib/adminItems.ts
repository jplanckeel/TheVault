import type { AdminItem, Item, ItemIssue, ItemPatch, SyncAction, SyncSource } from '../types'

type Tone = 'neutral' | 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info'

export const ISSUES: Record<ItemIssue, { label: string; icon: string; tone: Tone }> = {
  duplicate: { label: 'Doublon', icon: 'content_copy', tone: 'error' },
  missing_platform: { label: 'Sans plateforme', icon: 'device_unknown', tone: 'warning' },
  missing_cover: { label: 'Sans jaquette', icon: 'hide_image', tone: 'warning' },
  missing_igdb: { label: 'Sans IGDB', icon: 'link_off', tone: 'info' },
  missing_price: { label: 'Sans prix', icon: 'money_off', tone: 'neutral' },
}

/** Display order, most serious first. */
export const ISSUE_ORDER: ItemIssue[] = ['duplicate', 'missing_platform', 'missing_cover', 'missing_igdb', 'missing_price']

export const SOURCES: Record<SyncSource, { label: string; icon: string; tone: Tone }> = {
  mobile: { label: 'App mobile', icon: 'smartphone', tone: 'primary' },
  web: { label: 'Web', icon: 'language', tone: 'info' },
  admin: { label: 'Admin', icon: 'shield_person', tone: 'secondary' },
}

export const ACTIONS: Record<SyncAction, { label: string; tone: Tone }> = {
  created: { label: 'Créé', tone: 'success' },
  updated: { label: 'Mis à jour', tone: 'neutral' },
  rejected: { label: 'Refusé', tone: 'error' },
  edited: { label: 'Corrigé', tone: 'primary' },
  deleted: { label: 'Supprimé', tone: 'error' },
  merged: { label: 'Fusionné', tone: 'secondary' },
}

export interface AdminFilters {
  type: Item['type'] | 'all'
  issue: ItemIssue | 'any' | 'all'
  query: string
}

/** Filters back-office rows: type, issue ("any" = at least one issue) and free text. */
export function filterAdminItems(items: AdminItem[], f: AdminFilters): AdminItem[] {
  const q = f.query.trim().toLowerCase()
  return items.filter(
    (i) =>
      (f.type === 'all' || i.type === f.type) &&
      (f.issue === 'all' || (f.issue === 'any' ? i.issues.length > 0 : i.issues.includes(f.issue))) &&
      (!q || i.title.toLowerCase().includes(q) || i.platform.toLowerCase().includes(q) || i.external_id.toLowerCase().includes(q)),
  )
}

/** Number of items per issue. */
export function countIssues(items: AdminItem[]): Record<ItemIssue, number> {
  const counts = Object.fromEntries(ISSUE_ORDER.map((k) => [k, 0])) as Record<ItemIssue, number>
  for (const i of items) for (const issue of i.issues) counts[issue] += 1
  return counts
}

/** Editable fields of the item form. */
export const EDITABLE_FIELDS = [
  'title', 'type', 'platform', 'region', 'condition', 'cover_url', 'igdb_id',
  'developer', 'publisher', 'release_year', 'genres', 'summary',
] as const satisfies readonly (keyof ItemPatch)[]

export type ItemDraft = Pick<Item, (typeof EDITABLE_FIELDS)[number]>

export function draftOf(item: Item): ItemDraft {
  return Object.fromEntries(EDITABLE_FIELDS.map((k) => [k, item[k]])) as ItemDraft
}

/** Only the fields that differ from the original item, with trimmed strings. */
export function diffPatch(original: Item, draft: ItemDraft): ItemPatch {
  const patch: Record<string, unknown> = {}
  for (const key of EDITABLE_FIELDS) {
    const value = typeof draft[key] === 'string' ? (draft[key] as string).trim() : draft[key]
    if (value !== original[key]) patch[key] = value
  }
  return patch as ItemPatch
}
