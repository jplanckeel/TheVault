import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Item } from '../types'

export default function AccessoriesPage() {
  const navigate = useNavigate()
  const [accessories, setAccessories] = useState<Item[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.items
      .list()
      .then((items) => setAccessories(items.filter((i) => i.type === 'accessory')))
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false))
  }, [])

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
      <div className="flex items-start justify-between mb-8">
        <div>
          <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-2">Backoffice</p>
          <h1 className="text-2xl font-semibold text-vault-on-surface">Accessoires</h1>
          <p className="text-sm text-vault-on-surface-variant mt-1">Bibliothèque d'accessoires</p>
        </div>
      </div>

      {accessories.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-64 text-center">
          <p className="text-vault-on-surface-variant">Aucun accessoire pour l'instant.</p>
          <p className="text-xs text-vault-outline mt-2">Ajoutez des accessoires depuis l'application mobile.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {accessories.map((item) => (
            <div
              key={item.id}
              onClick={() => navigate(`/accessories/${item.id}`)}
              className="bg-vault-surface border border-vault-outline-variant rounded-xl overflow-hidden cursor-pointer transition-all hover:border-vault-primary hover:shadow-[0_0_0_1px_#BA9EFF] flex flex-col gap-4 p-5"
            >
              <div className="shrink-0 w-full aspect-square rounded-xl bg-vault-surface-highest flex items-center justify-center p-4">
                <span className="text-4xl">🎧</span>
              </div>
              <div className="flex flex-col gap-2">
                <h3 className="text-sm font-semibold text-vault-on-surface leading-snug">{item.title}</h3>
                <div className="flex flex-wrap gap-2">
                  <span className="text-xs text-vault-on-surface-variant">{item.platform}</span>
                  {item.condition && (
                    <span className="text-xs px-2 py-0.5 rounded-full bg-vault-surface-low text-vault-on-surface-variant">{item.condition}</span>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
