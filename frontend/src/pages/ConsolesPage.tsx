import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Item } from '../types'
import BrandPlaceholder from '../components/BrandPlaceholder'

function ConsoleCover({ item }: { item: Item }) {
  const [imgError, setImgError] = useState(false)
  if (item.cover_url && !imgError) {
    return <img src={item.cover_url} alt={item.title} className="w-full h-full object-cover" onError={() => setImgError(true)} />
  }
  return <BrandPlaceholder platform={item.platform} className="w-full h-full" />
}

export default function ConsolesPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<Item[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [activePlatform, setActivePlatform] = useState('all')

  useEffect(() => {
    api.items
      .list()
      .then((all) => setItems(all.filter((i) => i.type === 'console')))
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false))
  }, [])

  const platforms = useMemo(() => {
    const set = new Set(items.map((i) => i.platform).filter(Boolean))
    return Array.from(set).sort()
  }, [items])

  const filtered = useMemo(() => {
    let xs = items
    if (activePlatform !== 'all') xs = xs.filter((i) => i.platform === activePlatform)
    if (search.trim()) xs = xs.filter((i) => i.title.toLowerCase().includes(search.toLowerCase()))
    return xs
  }, [items, activePlatform, search])

  if (loading)
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-vault-on-surface-variant animate-pulse">Chargement…</p>
      </div>
    )
  if (error)
    return (
      <div className="rounded-xl bg-vault-surface border border-vault-error/30 p-4">
        <p className="text-vault-error text-sm">Erreur : {error}</p>
      </div>
    )

  return (
    <div>
      <div className="flex items-start justify-between mb-6">
        <div>
          <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-2">Backoffice</p>
          <h1 className="text-2xl font-semibold text-vault-on-surface">Consoles</h1>
          <p className="text-sm text-vault-on-surface-variant mt-1">{items.length} console{items.length > 1 ? 's' : ''}</p>
        </div>
      </div>

      {/* Search + filters */}
      <div className="flex flex-col gap-3 mb-6">
        <input
          type="search"
          placeholder="Rechercher une console…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full max-w-sm px-4 py-2 rounded-xl bg-vault-surface border border-vault-outline-variant text-sm text-vault-on-surface placeholder:text-vault-on-surface-variant/50 focus:outline-none focus:border-vault-primary"
        />
        {platforms.length > 1 && (
          <div className="flex flex-wrap gap-2">
            <button
              onClick={() => setActivePlatform('all')}
              className={`text-xs px-3 py-1 rounded-full font-medium transition-colors ${activePlatform === 'all' ? 'bg-vault-primary text-vault-on-primary' : 'bg-vault-surface-low text-vault-on-surface-variant hover:text-vault-on-surface'}`}
            >
              Tous
            </button>
            {platforms.map((p) => (
              <button
                key={p}
                onClick={() => setActivePlatform(p)}
                className={`text-xs px-3 py-1 rounded-full font-medium transition-colors ${activePlatform === p ? 'bg-vault-primary text-vault-on-primary' : 'bg-vault-surface-low text-vault-on-surface-variant hover:text-vault-on-surface'}`}
              >
                {p}
              </button>
            ))}
          </div>
        )}
      </div>

      {filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-64 text-center">
          <p className="text-vault-on-surface-variant">Aucune console trouvée.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {filtered.map((item) => (
            <div
              key={item.id}
              onClick={() => navigate(`/consoles/${item.id}`)}
              className="bg-vault-surface border border-vault-outline-variant rounded-xl overflow-hidden cursor-pointer transition-all hover:border-vault-primary hover:shadow-[0_0_0_1px_#BA9EFF] flex flex-col gap-4 p-5"
            >
              <div className="w-full aspect-square rounded-xl bg-vault-surface-highest flex items-center justify-center overflow-hidden">
                <ConsoleCover item={item} />
              </div>
              <div className="flex flex-col gap-2">
                <h3 className="text-sm font-semibold text-vault-on-surface leading-snug line-clamp-2">{item.title}</h3>
                <div className="flex flex-wrap gap-2">
                  <span className="text-xs text-vault-on-surface-variant">{item.platform}</span>
                  {item.release_year > 0 && (
                    <span className="text-xs px-2 py-0.5 rounded-full bg-vault-surface-low text-vault-on-surface-variant">{item.release_year}</span>
                  )}
                </div>
                <div className="mt-auto pt-3 border-t border-vault-outline-variant">
                  <span className="text-xs text-vault-on-surface-variant">
                    {item.igdb_id ? 'Enrichie' : 'Non enrichie'}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
