import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Item, PriceSnapshot } from '../types'

const CONDITION_STYLE: Record<string, { color: string; bg: string; label: string }> = {
  loose: { color: '#3B82F6', bg: 'rgba(59,130,246,0.12)', label: 'Loose' },
  cib:   { color: '#BA9EFF', bg: 'rgba(186,158,255,0.12)', label: 'CIB' },
  new:   { color: '#10B981', bg: 'rgba(16,185,129,0.12)', label: 'New' },
}

function PriceCard({ condition, snapshots }: { condition: string; snapshots: PriceSnapshot[] }) {
  const style = CONDITION_STYLE[condition] ?? { color: '#A3AAC4', bg: 'rgba(163,170,196,0.12)', label: condition }
  const latest = snapshots[0]
  if (!latest) return null

  return (
    <div
      className="rounded-xl p-5 border flex-1 min-w-[160px]"
      style={{ borderColor: style.color + '40', background: style.bg }}
    >
      <div className="flex items-center justify-between mb-4">
        <span className="text-xs font-semibold uppercase tracking-widest" style={{ color: style.color }}>
          {style.label}
        </span>
        <span className="text-xs px-2 py-0.5 rounded-full bg-vault-surface-highest text-vault-on-surface-variant">
          {latest.sample_count} ventes
        </span>
      </div>
      <p className="text-3xl font-bold mb-1" style={{ color: style.color }}>
        {latest.price_median.toFixed(2)} {latest.currency}
      </p>
      <p className="text-xs text-vault-on-surface-variant">médiane</p>
      <div className="flex gap-4 mt-4 pt-4 border-t" style={{ borderColor: style.color + '30' }}>
        <div>
          <p className="text-xs text-vault-on-surface-variant">Min</p>
          <p className="text-sm font-medium text-vault-on-surface">{latest.price_min.toFixed(2)} {latest.currency}</p>
        </div>
        <div>
          <p className="text-xs text-vault-on-surface-variant">Max</p>
          <p className="text-sm font-medium text-vault-on-surface">{latest.price_max.toFixed(2)} {latest.currency}</p>
        </div>
      </div>
    </div>
  )
}

function PriceHistoryTable({ prices }: { prices: PriceSnapshot[] }) {
  if (prices.length === 0) return (
    <p className="text-vault-on-surface-variant text-sm">Aucune donnée de prix.</p>
  )

  return (
    <div className="overflow-x-auto rounded-xl border border-vault-outline-variant">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-vault-outline-variant bg-vault-surface">
            {['Date', 'Condition', 'Min', 'Médiane', 'Max', 'Ventes'].map((h) => (
              <th key={h} className="text-left px-4 py-3 text-xs font-medium tracking-wider text-vault-on-surface-variant uppercase">
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {prices.map((p) => {
            const style = CONDITION_STYLE[p.condition] ?? { color: '#A3AAC4', label: p.condition }
            return (
              <tr key={p.id} className="border-b border-vault-outline-variant/50 hover:bg-vault-surface/50 transition-colors">
                <td className="px-4 py-3 text-vault-on-surface-variant">
                  {new Date(p.scraped_at).toLocaleDateString('fr-FR')}
                </td>
                <td className="px-4 py-3">
                  <span className="text-xs font-semibold uppercase" style={{ color: style.color }}>
                    {style.label}
                  </span>
                </td>
                <td className="px-4 py-3 text-vault-on-surface">{p.price_min.toFixed(2)} €</td>
                <td className="px-4 py-3 font-semibold text-vault-on-surface">{p.price_median.toFixed(2)} €</td>
                <td className="px-4 py-3 text-vault-on-surface">{p.price_max.toFixed(2)} €</td>
                <td className="px-4 py-3 text-vault-on-surface-variant">{p.sample_count}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

export default function ItemDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [item, setItem] = useState<Item | null>(null)
  const [prices, setPrices] = useState<PriceSnapshot[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    Promise.all([api.items.get(id), api.items.prices(id)])
      .then(([item, prices]) => { setItem(item); setPrices(prices) })
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <p className="text-vault-on-surface-variant animate-pulse">Chargement…</p>
    </div>
  )
  if (error) return (
    <div className="rounded-xl bg-vault-surface border border-vault-error/30 p-4">
      <p className="text-vault-error text-sm">Erreur : {error}</p>
    </div>
  )
  if (!item) return <p className="text-vault-on-surface-variant">Item introuvable.</p>

  const byCondition: Record<string, PriceSnapshot[]> = {}
  for (const p of prices) {
    if (!byCondition[p.condition]) byCondition[p.condition] = []
    byCondition[p.condition].push(p)
  }

  const badges = [
    { label: item.platform },
    { label: item.type },
    { label: item.region },
    { label: item.condition },
  ].filter((b) => b.label)

  return (
    <div>
      <button
        onClick={() => navigate('/')}
        className="flex items-center gap-2 text-sm text-vault-on-surface-variant hover:text-vault-primary transition-colors mb-6"
      >
        ← Retour au catalogue
      </button>

      <div className="mb-8">
        <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-2">
          Item
        </p>
        <h1 className="text-2xl font-semibold text-vault-on-surface mb-4">{item.title}</h1>
        <div className="flex flex-wrap gap-2">
          {badges.map((b) => (
            <span
              key={b.label}
              className="text-xs px-3 py-1 rounded-full bg-vault-secondary-container text-vault-secondary-text font-medium"
            >
              {b.label}
            </span>
          ))}
        </div>
      </div>

      {Object.keys(byCondition).length > 0 && (
        <div className="mb-8">
          <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-4">
            Prix actuels
          </p>
          <div className="flex gap-4 flex-wrap">
            {['loose', 'cib', 'new'].map((cond) =>
              byCondition[cond] ? (
                <PriceCard key={cond} condition={cond} snapshots={byCondition[cond]} />
              ) : null
            )}
          </div>
        </div>
      )}

      <div>
        <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-4">
          Historique des prix
        </p>
        <PriceHistoryTable prices={prices} />
      </div>
    </div>
  )
}
