export interface Item {
  id: string
  external_id: string
  type: 'game' | 'console' | 'accessory'
  title: string
  platform: string
  region: string
  condition: string
  status: string
  cover_url: string
  igdb_id: string
  summary: string
  genres: string
  developer: string
  publisher: string
  release_year: number
  created_at: string
  updated_at: string
}

export interface PriceSnapshot {
  id: string
  item_id: string
  source: string
  condition: 'loose' | 'cib' | 'new'
  price_min: number
  price_max: number
  price_median: number
  sample_count: number
  currency: string
  scraped_at: string
}

export type JobStatus = 'pending' | 'running' | 'done' | 'failed'

export interface ScrapeJob {
  id: string
  job_type: string
  triggered_by: string
  status: JobStatus
  items_total: number
  items_done: number
  error?: string
  started_at: string
  finished_at?: string
}

/** Price evolution of an item over a period (GET /prices/top-movers). */
export interface TopMover {
  item_id: string
  condition: string
  price_old: number
  price_new: number
  delta_abs: number
  delta_pct: number
}

/** Data quality issues computed by the backend (GET /admin/items). */
export type ItemIssue = 'missing_cover' | 'missing_igdb' | 'missing_platform' | 'missing_price' | 'duplicate'

/** Catalogue item with back-office information. */
export interface AdminItem extends Item {
  admin_edited: boolean
  issues: ItemIssue[]
  duplicate_ids: string[]
  owners: number
  has_price: boolean
}

/** Editable catalogue fields; omitted fields are left untouched. */
export type ItemPatch = Partial<
  Pick<Item, 'type' | 'title' | 'platform' | 'region' | 'condition' | 'cover_url' | 'igdb_id' | 'summary' | 'genres' | 'developer' | 'publisher' | 'release_year'>
>

export type SyncSource = 'mobile' | 'web' | 'admin'
export type SyncAction = 'created' | 'updated' | 'rejected' | 'edited' | 'deleted' | 'merged'

/** One write to the shared catalogue (sync or admin correction). */
export interface SyncEvent {
  id: string
  source: SyncSource
  action: SyncAction
  item_id: string | null
  external_id: string
  title: string
  actor: string
  detail: string
  created_at: string
}

/** Event counts per source and action since `since`. */
export interface SyncSummary {
  since: string
  counts: Partial<Record<SyncSource, Partial<Record<SyncAction, number>>>>
}
