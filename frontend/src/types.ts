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
  triggered_by: string
  status: JobStatus
  items_total: number
  items_done: number
  error?: string
  started_at: string
  finished_at?: string
}
