import type { Item, PriceSnapshot, ScrapeJob } from './types'

const BASE = '/api/v1'

function authHeaders(): Record<string, string> {
  const token = sessionStorage.getItem('admin_token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`, { headers: authHeaders() })
  if (!res.ok) throw new Error(`GET ${path}: ${res.status}`)
  return res.json() as Promise<T>
}

async function post<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`POST ${path}: ${res.status}`)
  return res.json() as Promise<T>
}

async function put<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`PUT ${path}: ${res.status}`)
  return res.json() as Promise<T>
}

export const api = {
  items: {
    list: () => get<Item[]>('/items'),
    get: (id: string) => get<Item>(`/items/${id}`),
    prices: (id: string) => get<PriceSnapshot[]>(`/items/${id}/prices`),
    syncIGDB: () => post<{ message: string; total: number }>('/items/sync-igdb', {}),
    updateCover: (id: string, coverURL: string) =>
      put<{ cover_url: string }>(`/items/${id}/cover`, { cover_url: coverURL }),
  },
  prices: {
    latest: () => get<PriceSnapshot[]>('/prices/latest'),
  },
  scrape: {
    trigger: (triggeredBy: string, itemIds?: string[]) =>
      post<ScrapeJob>('/scrape', { triggered_by: triggeredBy, item_ids: itemIds }),
    list: () => get<ScrapeJob[]>('/scrape'),
    get: (id: string) => get<ScrapeJob>(`/scrape/${id}`),
  },
}
