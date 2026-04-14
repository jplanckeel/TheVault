import type { Item, PriceSnapshot, ScrapeJob } from './types'

const BASE = '/api/v1'

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`)
  if (!res.ok) throw new Error(`GET ${path}: ${res.status}`)
  return res.json() as Promise<T>
}

async function post<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`POST ${path}: ${res.status}`)
  return res.json() as Promise<T>
}

export const api = {
  items: {
    list: () => get<Item[]>('/items'),
    get: (id: string) => get<Item>(`/items/${id}`),
    prices: (id: string) => get<PriceSnapshot[]>(`/items/${id}/prices`),
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
