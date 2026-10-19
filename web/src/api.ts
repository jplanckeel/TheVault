import type { Item, PriceSnapshot, ScrapeJob } from './types'

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

function userHeaders(): Record<string, string> {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

function adminHeaders(): Record<string, string> {
  const token = sessionStorage.getItem('admin_token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function get<T>(path: string, auth: 'none' | 'user' | 'admin' = 'none'): Promise<T> {
  const headers = auth === 'admin' ? adminHeaders() : auth === 'user' ? userHeaders() : {}
  const res = await fetch(`${BASE}${path}`, { headers })
  if (!res.ok) throw new Error(`GET ${path}: ${res.status}`)
  return res.json() as Promise<T>
}

async function post<T>(path: string, body: unknown, auth: 'user' | 'admin' = 'admin'): Promise<T> {
  const headers = auth === 'admin' ? adminHeaders() : userHeaders()
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    const detail = await res.text().catch(() => '')
    throw new Error(`POST ${path}: ${res.status}${detail ? ` — ${detail.trim()}` : ''}`)
  }
  // Backend may return 204 No Content for write operations.
  if (res.status === 204 || res.headers.get('content-length') === '0') {
    return undefined as T
  }
  const text = await res.text()
  if (!text) return undefined as T
  return JSON.parse(text) as T
}

async function put<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...adminHeaders() },
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    const detail = await res.text().catch(() => '')
    throw new Error(`PUT ${path}: ${res.status}${detail ? ` — ${detail.trim()}` : ''}`)
  }
  if (res.status === 204) return undefined as T
  const text = await res.text()
  if (!text) return undefined as T
  return JSON.parse(text) as T
}

async function del(path: string): Promise<void> {
  const res = await fetch(`${BASE}${path}`, {
    method: 'DELETE',
    headers: userHeaders(),
  })
  if (!res.ok) {
    const detail = await res.text().catch(() => '')
    throw new Error(`DELETE ${path}: ${res.status}${detail ? ` — ${detail.trim()}` : ''}`)
  }
}

export const api = {
  items: {
    list: () => get<Item[]>('/items'),
    get: (id: string) => get<Item>(`/items/${id}`),
    prices: (id: string) => get<PriceSnapshot[]>(`/items/${id}/prices`),
    syncIGDB: () => post<{ message: string; total: number }>('/items/sync-igdb', {}, 'admin'),
    updateCover: (id: string, coverURL: string) =>
      put<{ cover_url: string }>(`/items/${id}/cover`, { cover_url: coverURL }),
    uploadCover: (id: string, file: File): Promise<{ cover_url: string }> => {
      const form = new FormData()
      form.append('cover', file)
      const token = sessionStorage.getItem('admin_token')
      return fetch(`${BASE}/items/${id}/cover/upload`, {
        method: 'POST',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: form,
      }).then((res) => {
        if (!res.ok) throw new Error(`upload cover: ${res.status}`)
        return res.json()
      })
    },
    /** Upsert an item in the shared catalogue. Any logged-in user can contribute. */
    upsert: (item: Partial<Item> & { external_id: string; title: string; platform: string }) =>
      post<Item>('/items', item, 'user'),
  },
  prices: {
    latest: () => get<PriceSnapshot[]>('/prices/latest'),
  },
  scrape: {
    trigger: (triggeredBy: string, itemIds?: string[]) =>
      post<ScrapeJob>('/scrape', { triggered_by: triggeredBy, item_ids: itemIds }, 'admin'),
    list: () => get<ScrapeJob[]>('/scrape', 'admin'),
    get: (id: string) => get<ScrapeJob>(`/scrape/${id}`, 'admin'),
  },
  collection: {
    list: () => get<Item[]>('/collection', 'user'),
    add: (itemId: string) => post<void>('/collection', { item_id: itemId, status: 'owned' }, 'user'),
    remove: (itemId: string) => del(`/collection/${itemId}`),
  },
  wishlist: {
    list: () => get<Item[]>('/wishlist', 'user'),
    add: (itemId: string) => post<void>('/collection', { item_id: itemId, status: 'wishlist' }, 'user'),
    remove: (itemId: string) => del(`/collection/${itemId}`),
  },
  search: {
    /** IGDB search — accessible to any logged-in user. Returns [] on error. */
    igdb: async (q: string): Promise<IGDBSearchResult[]> => {
      try {
        return await get<IGDBSearchResult[]>(`/search?q=${encodeURIComponent(q)}`, 'user')
      } catch {
        return []
      }
    },
  },
}
