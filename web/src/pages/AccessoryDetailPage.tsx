import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../AuthContext'
import type { Item } from '../types'
import { Card, Chip, MaterialIcon, Stat, useToast } from '../components/ui'
import BrandPlaceholder from '../components/BrandPlaceholder'

export default function AccessoryDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const toast = useToast()
  const { isAdmin } = useAuth()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [item, setItem] = useState<Item | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)

  useEffect(() => {
    if (!id) return
    api.items.get(id)
      .then(setItem)
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

  if (loading)
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-vault-on-surface-variant animate-pulse">Chargement…</p>
      </div>
    )
  if (error || !item)
    return (
      <div className="rounded-xl bg-vault-surface border border-vault-error/30 p-4">
        <p className="text-vault-error text-sm">{error ?? 'Accessoire introuvable.'}</p>
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
          {/* Cover thumbnail */}
          <div className="relative shrink-0 w-32 h-32 rounded-xl overflow-hidden bg-vault-surface-highest border border-vault-outline-variant/30 flex items-center justify-center group">
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
              <Chip tone="secondary" variant="soft" size="sm">Accessoire</Chip>
              {item.platform && <Chip tone="primary" variant="soft" size="sm">{item.platform}</Chip>}
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
        <Stat
          label="Condition"
          value={item.condition ? item.condition.toUpperCase() : '—'}
          hint="État de l'accessoire"
          icon="inventory_2"
        />
        <Stat
          label="Plateforme"
          value={item.platform || '—'}
          hint="Système compatible"
          icon="sports_esports"
        />
        <Stat
          label="Statut"
          value={item.status || '—'}
          hint={item.igdb_id ? `IGDB #${item.igdb_id}` : 'Non enrichi'}
          icon="verified"
        />
      </div>

      {/* Details card */}
      {(item.developer || item.publisher || item.genres || item.release_year > 0) && (
        <Card padding="md" radius="2xl">
          <h2 className="text-headline-sm text-vault-on-surface mb-4">Détails</h2>
          <dl className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            {item.developer && (
              <div>
                <dt className="text-xs text-vault-on-surface-variant uppercase tracking-wider mb-1">Fabricant</dt>
                <dd className="font-medium text-vault-on-surface">{item.developer}</dd>
              </div>
            )}
            {item.publisher && item.publisher !== item.developer && (
              <div>
                <dt className="text-xs text-vault-on-surface-variant uppercase tracking-wider mb-1">Éditeur</dt>
                <dd className="font-medium text-vault-on-surface">{item.publisher}</dd>
              </div>
            )}
            {item.release_year > 0 && (
              <div>
                <dt className="text-xs text-vault-on-surface-variant uppercase tracking-wider mb-1">Année</dt>
                <dd className="font-medium text-vault-on-surface">{item.release_year}</dd>
              </div>
            )}
            {item.genres && (
              <div>
                <dt className="text-xs text-vault-on-surface-variant uppercase tracking-wider mb-1">Catégorie</dt>
                <dd className="font-medium text-vault-on-surface">{item.genres}</dd>
              </div>
            )}
          </dl>
        </Card>
      )}
    </div>
  )
}
