import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Item } from '../types'

function PageHeader({ onScrape, scraping }: { onScrape: () => void; scraping: boolean }) {
  return (
    <div className="flex items-start justify-between mb-8">
      <div>
        <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-2">
          Backoffice
        </p>
        <h1 className="text-2xl font-semibold text-vault-on-surface">Catalogue</h1>
      </div>
      <button
        onClick={onScrape}
        disabled={scraping}
        className="px-4 py-2 rounded-lg text-sm font-semibold text-white disabled:opacity-50 disabled:cursor-not-allowed transition-opacity"
        style={{ background: 'linear-gradient(135deg, #8455EF, #EC63FF)' }}
      >
        {scraping ? 'Scraping…' : 'Trigger Scrape'}
      </button>
    </div>
  )
}

function ItemCard({ item, onClick }: { item: Item; onClick: () => void }) {
  return (
    <div
      onClick={onClick}
      className="bg-vault-surface border border-vault-outline-variant rounded-xl p-5 cursor-pointer transition-all hover:border-vault-primary hover:shadow-[0_0_0_1px_#BA9EFF]"
    >
      <div className="flex items-start justify-between mb-3">
        <h3 className="text-sm font-semibold text-vault-on-surface leading-snug flex-1 mr-2">
          {item.title}
        </h3>
        {item.region && (
          <span className="shrink-0 text-xs px-2 py-0.5 rounded-full bg-vault-secondary-container text-vault-secondary-text font-medium">
            {item.region}
          </span>
        )}
      </div>
      <div className="flex items-center gap-2 flex-wrap">
        <span className="text-xs text-vault-on-surface-variant">{item.platform}</span>
        {item.platform && item.type && (
          <span className="text-vault-outline text-xs">·</span>
        )}
        <span className="text-xs text-vault-on-surface-variant capitalize">{item.type}</span>
      </div>
      {item.status && (
        <div className="mt-3 pt-3 border-t border-vault-outline-variant">
          <span className="text-xs text-vault-on-surface-variant">{item.status}</span>
        </div>
      )}
    </div>
  )
}

export default function CataloguePage() {
  const navigate = useNavigate()
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
      setScrapeMsg(`Job démarré (ID: ${job.id})`)
    } catch (e) {
      setScrapeMsg(`Erreur: ${String(e)}`)
    } finally {
      setScraping(false)
    }
  }

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

  return (
    <div>
      <PageHeader onScrape={triggerScrape} scraping={scraping} />

      {scrapeMsg && (
        <div className="mb-6 px-4 py-3 rounded-lg bg-vault-surface border border-vault-outline-variant">
          <p className="text-sm text-vault-primary">{scrapeMsg}</p>
        </div>
      )}

      {items.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-64 text-center">
          <p className="text-vault-on-surface-variant">Aucun item pour l'instant.</p>
          <p className="text-xs text-vault-outline mt-2">Lance un scrape pour commencer.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {items.map((item) => (
            <ItemCard
              key={item.id}
              item={item}
              onClick={() => navigate(`/items/${item.id}`)}
            />
          ))}
        </div>
      )}
    </div>
  )
}
