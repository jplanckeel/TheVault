import { useEffect, useState } from 'react'
import { api } from '../api'
import type { Item } from '../types'

export default function CataloguePage() {
  const [items, setItems] = useState<Item[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [scraping, setScraping] = useState(false)
  const [scrapeMsg, setScrapeMsg] = useState<string | null>(null)

  useEffect(() => {
    api.items.list()
      .then(setItems)
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false))
  }, [])

  async function triggerScrape() {
    setScraping(true)
    setScrapeMsg(null)
    try {
      const job = await api.scrape.trigger('user')
      setScrapeMsg(`Scrape job started (ID: ${job.id})`)
    } catch (e) {
      setScrapeMsg(`Error: ${String(e)}`)
    } finally {
      setScraping(false)
    }
  }

  if (loading) return <p>Loading…</p>
  if (error) return <p style={{ color: 'red' }}>Error: {error}</p>

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <h1 style={{ margin: 0 }}>Catalogue</h1>
        <button onClick={triggerScrape} disabled={scraping}>
          {scraping ? 'Scraping…' : 'Trigger scrape'}
        </button>
      </div>

      {scrapeMsg && <p>{scrapeMsg}</p>}

      {items.length === 0 ? (
        <p>No items yet.</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr>
              {['Title', 'Platform', 'Type', 'Region', 'Status'].map((h) => (
                <th key={h} style={{ textAlign: 'left', padding: '0.5rem', borderBottom: '1px solid #e0e0e0' }}>
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td style={{ padding: '0.5rem', borderBottom: '1px solid #f0f0f0' }}>{item.title}</td>
                <td style={{ padding: '0.5rem', borderBottom: '1px solid #f0f0f0' }}>{item.platform}</td>
                <td style={{ padding: '0.5rem', borderBottom: '1px solid #f0f0f0' }}>{item.type}</td>
                <td style={{ padding: '0.5rem', borderBottom: '1px solid #f0f0f0' }}>{item.region}</td>
                <td style={{ padding: '0.5rem', borderBottom: '1px solid #f0f0f0' }}>{item.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
