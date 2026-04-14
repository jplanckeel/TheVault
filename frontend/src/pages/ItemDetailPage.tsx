import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { api } from '../api'
import type { Item, PriceSnapshot } from '../types'

export default function ItemDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [item, setItem] = useState<Item | null>(null)
  const [prices, setPrices] = useState<PriceSnapshot[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    Promise.all([api.items.get(id), api.items.prices(id)])
      .then(([item, prices]) => {
        setItem(item)
        setPrices(prices)
      })
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <p>Loading…</p>
  if (error) return <p style={{ color: 'red' }}>Error: {error}</p>
  if (!item) return <p>Item not found.</p>

  return (
    <div>
      <Link to="/" style={{ marginBottom: '1rem', display: 'inline-block' }}>← Back to catalogue</Link>

      <h1>{item.title}</h1>
      <table style={{ marginBottom: '2rem' }}>
        <tbody>
          {[
            ['Platform', item.platform],
            ['Type', item.type],
            ['Region', item.region],
            ['Condition', item.condition],
            ['Status', item.status],
          ].map(([label, value]) => (
            <tr key={label}>
              <td style={{ padding: '0.25rem 1rem 0.25rem 0', fontWeight: 600 }}>{label}</td>
              <td style={{ padding: '0.25rem 0' }}>{value}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h2>Price history</h2>
      {prices.length === 0 ? (
        <p>No price data yet.</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr>
              {['Date', 'Condition', 'Min (€)', 'Median (€)', 'Max (€)', 'Samples'].map((h) => (
                <th key={h} style={{ textAlign: 'left', padding: '0.5rem', borderBottom: '1px solid #e0e0e0' }}>
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {prices.map((p) => (
              <tr key={p.id}>
                <td style={{ padding: '0.5rem', borderBottom: '1px solid #f0f0f0' }}>
                  {new Date(p.scraped_at).toLocaleDateString()}
                </td>
                <td style={{ padding: '0.5rem', borderBottom: '1px solid #f0f0f0' }}>{p.condition}</td>
                <td style={{ padding: '0.5rem', borderBottom: '1px solid #f0f0f0' }}>{p.price_min.toFixed(2)}</td>
                <td style={{ padding: '0.5rem', borderBottom: '1px solid #f0f0f0' }}>{p.price_median.toFixed(2)}</td>
                <td style={{ padding: '0.5rem', borderBottom: '1px solid #f0f0f0' }}>{p.price_max.toFixed(2)}</td>
                <td style={{ padding: '0.5rem', borderBottom: '1px solid #f0f0f0' }}>{p.sample_count}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
