import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Item } from '../types'

export default function AccessoryDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [accessory, setAccessory] = useState<Item | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    api.items
      .get(id)
      .then(setAccessory)
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false))
  }, [id])

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
  if (!accessory) return <p className="text-vault-on-surface-variant">Accessoire introuvable.</p>

  const handleCoverSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const input = (e.target as HTMLFormElement).elements.namedItem('cover_url') as HTMLInputElement
    if (!input.value.trim()) return
    try {
      await api.items.updateCover(accessory.id, input.value.trim())
      setAccessory({ ...accessory, cover_url: input.value.trim() })
      input.value = ''
    } catch (err) {
      alert('Erreur: ' + err)
    }
  }

  return (
    <div>
      <button
        onClick={() => navigate('/accessories')}
        className="flex items-center gap-2 text-sm text-vault-on-surface-variant hover:text-vault-primary transition-colors mb-6"
      >
        ← Retour aux accessoires
      </button>

      <div className="flex gap-6 items-start mb-8">
        <div className="shrink-0 w-28 h-28 rounded-xl bg-vault-surface border border-vault-outline-variant flex items-center justify-center overflow-hidden">
          {accessory.cover_url ? (
            <img src={accessory.cover_url} alt={accessory.title} className="w-full h-full object-cover" />
          ) : (
            <span className="text-4xl">🎧</span>
          )}
        </div>
        <div className="flex-1 min-w-0">
          <h1 className="text-2xl font-semibold text-vault-on-surface mb-1">{accessory.title}</h1>
          <div className="flex flex-wrap gap-2 mt-3">
            <span className="text-xs px-3 py-1 rounded-full bg-vault-secondary-container text-vault-secondary-text font-medium">
              {accessory.platform}
            </span>
            {accessory.condition && (
              <span className="text-xs px-3 py-1 rounded-full bg-vault-secondary-container text-vault-secondary-text font-medium">
                {accessory.condition}
              </span>
            )}
            {accessory.region && (
              <span className="text-xs px-3 py-1 rounded-full bg-vault-secondary-container text-vault-secondary-text font-medium">
                {accessory.region}
              </span>
            )}
          </div>

          {!accessory.cover_url && (
            <form onSubmit={handleCoverSubmit} className="flex gap-2 mt-3">
              <input
                name="cover_url"
                type="url"
                placeholder="URL de l'image…"
                className="flex-1 px-3 py-1.5 text-sm rounded-lg border border-vault-outline-variant bg-vault-surface-low text-vault-on-surface placeholder:text-vault-on-surface-variant/50"
              />
              <button
                type="submit"
                className="px-3 py-1.5 text-sm rounded-lg bg-vault-primary text-white font-medium"
              >
                Ajouter
              </button>
            </form>
          )}
        </div>
      </div>

      {accessory.summary && (
        <div className="rounded-xl border border-vault-outline-variant bg-vault-surface p-5 mb-6">
          <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-3">Description</p>
          <p className="text-sm text-vault-on-surface">{accessory.summary}</p>
        </div>
      )}
    </div>
  )
}
