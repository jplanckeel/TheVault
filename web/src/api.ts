import type { AdminItem, ImageCandidates, Item, ItemPatch, PriceSnapshot, ScrapeJob, SyncEvent, SyncSummary, TopMover } from './types'

export interface IGDBSearchResult {
  id: string
  title: string
  release_year?: number
  platforms?: string[]
  genres?: string[]
  cover_url?: string
  summary?: string
  developer?: string
  publisher?: string
}

const BASE = '/api/v1'

/** Session token; admin routes use the same token, the backend checks the role. */
function authHeaders(): Record<string, string> {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const isForm = body instanceof FormData
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: { ...(body !== undefined && !isForm ? { 'Content-Type': 'application/json' } : {}), ...authHeaders() },
    body: body === undefined ? undefined : isForm ? body : JSON.stringify(body),
  })
  if (!res.ok) {
    const detail = await res.text().catch(() => '')
    throw new Error(`${method} ${path}: ${res.status}${detail ? ` — ${detail.trim()}` : ''}`)
  }
  const text = res.status === 204 ? '' : await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}

const get = <T,>(path: string) => request<T>('GET', path)
const post = <T,>(path: string, body: unknown = {}) => request<T>('POST', path, body)
const put = <T,>(path: string, body: unknown) => request<T>('PUT', path, body)
const del = (path: string) => request<void>('DELETE', path)

function query(params: Record<string, string | number | undefined>): string {
  const q = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) if (v !== undefined && v !== '') q.set(k, String(v))
  const s = q.toString()
  return s ? `?${s}` : ''
}

export const api = {
  items: {
    list: () => get<Item[]>('/items'),
    get: (id: string) => get<Item>(`/items/${id}`),
    prices: (id: string) => get<PriceSnapshot[]>(`/items/${id}/prices`),
    syncIGDB: () => post<{ message: string; total: number }>('/items/sync-igdb'),
    updateCover: (id: string, coverURL: string) => put<{ cover_url: string }>(`/items/${id}/cover`, { cover_url: coverURL }),
    uploadCover: (id: string, file: File) => {
      const form = new FormData()
      form.append('cover', file)
      return post<{ cover_url: string }>(`/items/${id}/cover/upload`, form)
    },
    /** Upsert an item in the shared catalogue. Any logged-in user can contribute. */
    upsert: (item: Partial<Item> & { external_id: string; title: string; platform: string }) => post<Item>('/items', item),
  },
  prices: {
    latest: () => get<PriceSnapshot[]>('/prices/latest'),
    /** Largest price increases over the last `days` days, across the whole catalogue. */
    topMovers: (days = 30, limit = 5) => get<TopMover[]>(`/prices/top-movers${query({ days, limit })}`),
  },
  scrape: {
    trigger: (triggeredBy: string, itemIds?: string[]) => post<ScrapeJob>('/scrape', { triggered_by: triggeredBy, item_ids: itemIds }),
    list: () => get<ScrapeJob[]>('/scrape'),
    get: (id: string) => get<ScrapeJob>(`/scrape/${id}`),
  },
  collection: {
    list: () => get<Item[]>('/collection'),
    add: (itemId: string) => post<void>('/collection', { item_id: itemId, status: 'owned' }),
    remove: (itemId: string) => del(`/collection/${itemId}`),
  },
  wishlist: {
    list: () => get<Item[]>('/wishlist'),
    add: (itemId: string) => post<void>('/collection', { item_id: itemId, status: 'wishlist' }),
    remove: (itemId: string) => del(`/collection/${itemId}`),
  },
  search: {
    /** IGDB search — accessible to any logged-in user. Returns [] on error. */
    igdb: async (q: string): Promise<IGDBSearchResult[]> => {
      try {
        return await get<IGDBSearchResult[]>(`/search${query({ q })}`)
      } catch {
        return []
      }
    },
  },
  /** Back-office — requires an account with the admin role. */
  admin: {
    items: () => get<AdminItem[]>('/admin/items'),
    update: (id: string, patch: ItemPatch) => put<Item>(`/admin/items/${id}`, patch),
    remove: (id: string) => del(`/admin/items/${id}`),
    merge: (sourceId: string, targetId: string) => post<Item>(`/admin/items/${sourceId}/merge`, { target_id: targetId }),
    refreshIGDB: (id: string) => post<ScrapeJob>(`/admin/items/${id}/igdb`),
    /** Applies the free console photos now (otherwise hourly). */
    consolePhotos: () => post<{ updated: number; failed: number; unmatched: number }>('/admin/console-photos'),
    imageCandidates: (id: string) => get<ImageCandidates>(`/admin/items/${id}/images`),
    importCover: (id: string, url: string) => post<{ cover_url: string }>(`/admin/items/${id}/cover/import`, { url }),
    refreshPrice: (id: string) => post<ScrapeJob>('/scrape', { triggered_by: 'admin', item_ids: [id] }),
    syncEvents: (filter: { source?: string; action?: string; item_id?: string; limit?: number } = {}) =>
      get<SyncEvent[]>(`/admin/sync-events${query(filter)}`),
    syncSummary: (hours = 24) => get<SyncSummary>(`/admin/sync-summary${query({ hours })}`),
  },
}
