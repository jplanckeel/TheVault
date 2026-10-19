import { useEffect, useRef, useMemo, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../AuthContext'
import type { Item, PriceSnapshot } from '../types'
import { Card, Chip, MaterialIcon, Stat, useToast } from '../components/ui'
import PriceSection from '../components/ui/PriceSection'
import BrandPlaceholder from '../components/BrandPlaceholder'

export default function ConsoleDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const toast = useToast()
  const { isAdmin } = useAuth()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [item, setItem] = useState<Item | null>(null)
  const [games, setGames] = useState<Item[]>([])
  const [prices, setPrices] = useState<PriceSnapshot[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)

  useEffect(() => {
    if (!id) return
    Promise.all([api.items.get(id), api.items.list(), api.items.prices(id)])
      .then(([console, all, p]) => {
        setItem(console)
        setGames(all.filter((i) => i.type === 'game' && i.platform === console.platform))
        setPrices(p)
      })
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false))
  }, [id])

  async function handleUpload(file: File) {
    if (!item) return
    setUploading(true)
    try {
      const { cover_url } = await api.items.uploadCover(item.id, file)
      setItem({ ...item, cover_url })
      toast.push('Cover mise à jour', 'success')
    } catch (e) {
      toast.push(`Erreur : ${String(e)}`, 'error')
    } finally {
      setUploading(false)
    }
  }

  const enrichedGames = useMemo(() => games.filter((g) => g.igdb_id), [games])

  if (loading)
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-vault-on-surface-variant animate-pulse">Chargement…</p>
      </div>
    )
  if (error || !item)
    return (
      <div className="rounded-xl bg-vault-surface border border-vault-error/30 p-4">
        <p className="text-vault-error text-sm">{error ?? 'Console introuvable.'}</p>
      </div>
    )

  const heroBg = item.cover_url
    ? `linear-gradient(135deg, rgba(15,15,18,0.85) 0%, rgba(15,15,18,0.55) 60%, rgba(15,15,18,0.95) 100%), url(${item.cover_url.replace(/t_cover_big(_2x)?/, 't_1080p')})`
    : 'linear-gradient(135deg, #1f1f22 0%, #2a2a2d 100%)'

  return (
    <div className="space-y-6">
      <button
        onClick={() => navigate(-1)}
        className="flex items-center gap-1.5 text-sm text-vault-on-surface-variant hover:text-vault-primary transition-colors focus-ring rounded"
      >
        <MaterialIcon name="arrow_back" size={16} />
        <span>Retour</span>
      </button>

      {/* Hero */}
      <div
        className="relative rounded-2xl overflow-hidden border border-vault-outline-variant/30"
        style={{ background: heroBg, backgroundSize: 'cover', backgroundPosition: 'center' }}
      >
        <div className="relative p-8 md:p-12 flex gap-8 items-start">
          <div className="relative shrink-0 w-32 h-32 rounded-xl overflow-hidden border border-vault-outline-variant/30 group">
            {item.cover_url ? (
              <img src={item.cover_url} alt={item.title} className="w-full h-full object-cover" />
            ) : (
              <BrandPlaceholder platform={item.platform} className="w-full h-full" />
            )}
            {isAdmin && (
              <button
                onClick={() => fileInputRef.current?.click()}
                disabled={uploading}
                className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-white text-xs font-medium"
              >
                {uploading ? '…' : '✏️'}
              </button>
            )}
          </div>

          {isAdmin && (
            <input ref={fileInputRef} type="file" accept="image/jpeg,image/png,image/webp" hidden
              onChange={(e) => { const f = e.target.files?.[0]; if (f) handleUpload(f); e.target.value = '' }} />
          )}

          <div className="flex-1 min-w-0">
            <div className="flex flex-wrap gap-2 mb-3">
              <Chip tone="primary" variant="soft" size="sm">{item.platform}</Chip>
              {item.region && <Chip tone="neutral" variant="soft" size="sm">{item.region}</Chip>}
            </div>
            <h1 className="text-display-lg text-vault-on-surface mb-2">{item.title}</h1>
            {item.summary && (
              <p className="max-w-2xl text-vault-on-surface-variant text-sm line-clamp-3">{item.summary}</p>
            )}
          </div>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Stat label="Jeux associés" value={String(games.length)} hint={`${enrichedGames.length} enrichis IGDB`} icon="stadia_controller" />
        <Stat label="Année de sortie" value={item.release_year > 0 ? String(item.release_year) : '—'} hint="Date de lancement" icon="calendar_today" />
        <Stat label="Statut" value={item.igdb_id ? 'Enrichie' : 'Non enrichie'} hint={item.igdb_id ? `IGDB #${item.igdb_id}` : 'Lance un sync IGDB'} icon="verified" />
      </div>

      {/* Prix eBay */}
      <PriceSection prices={prices} searchQuery={`${item.title} ${item.platform}`} />

      {/* Games list */}
      {games.length > 0 && (
        <Card padding="md" radius="2xl">
          <h2 className="text-headline-sm text-vault-on-surface mb-4">
            Jeux sur {item.platform} ({games.length})
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {games.map((game) => (
              <div
                key={game.id}
                onClick={() => navigate(`/items/${game.id}`)}
                className="flex gap-3 p-3 rounded-xl border border-vault-outline-variant/30 hover:border-vault-primary hover:bg-vault-surface-low/40 cursor-pointer transition-all"
              >
                <div className="shrink-0 w-12 h-16 rounded-lg overflow-hidden bg-vault-surface-highest flex items-center justify-center">
                  {game.cover_url ? (
                    <img src={game.cover_url} alt={game.title} className="w-full h-full object-cover" />
                  ) : (
                    <BrandPlaceholder platform={game.platform} className="w-full h-full" />
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-semibold text-vault-on-surface leading-snug line-clamp-2">{game.title}</p>
                  <p className="text-xs text-vault-on-surface-variant mt-1">
                    {game.release_year > 0 ? game.release_year : ''}
                    {game.genres ? ` · ${game.genres.split(',')[0]}` : ''}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  )
}
