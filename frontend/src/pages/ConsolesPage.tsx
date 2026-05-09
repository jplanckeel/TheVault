import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Item } from '../types'

function ConsoleCard({ item, onClick }: { item: Item; onClick: () => void }) {
  const controller = item.platform.toLowerCase().replace(/\s+/g, '-')

  return (
    <div
      onClick={onClick}
      className="bg-vault-surface border border-vault-outline-variant rounded-xl overflow-hidden cursor-pointer transition-all hover:border-vault-primary hover:shadow-[0_0_0_1px_#BA9EFF] flex flex-col gap-4 p-5"
    >
      {/* Controller SVG */}
      <div className="shrink-0 w-full aspect-square rounded-xl bg-vault-surface-highest flex items-center justify-center p-4">
        <img
          src={`/controllercons/${controller}.svg`}
          alt={item.platform}
          className="w-full h-full object-contain"
          onError={(e) => {
            ;(e.target as HTMLImageElement).src = '/controllercons/joy-cons.svg'
          }}
        />
      </div>

      {/* Info */}
      <div className="flex flex-col gap-2">
        <h3 className="text-sm font-semibold text-vault-on-surface leading-snug">
          {item.title}
        </h3>

        <div className="flex flex-wrap gap-2">
          {item.release_year > 0 && (
            <span className="text-xs px-2 py-0.5 rounded-full bg-vault-surface-low text-vault-on-surface-variant">
              {item.release_year}
            </span>
          )}
          <span className="text-xs text-vault-on-surface-variant">
            {item.platform}
          </span>
        </div>

        {/* Count of games */}
        <div className="mt-auto pt-3 border-t border-vault-outline-variant">
          <span className="text-xs text-vault-on-surface-variant">
            {item.igdb_id ? 'Enregistrée' : 'Non enrichie'}
          </span>
        </div>
      </div>
    </div>
  )
}

function PageHeader() {
  return (
    <div className="flex items-start justify-between mb-8">
      <div>
        <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-2">
          Backoffice
        </p>
        <h1 className="text-2xl font-semibold text-vault-on-surface">Consoles</h1>
        <p className="text-sm text-vault-on-surface-variant mt-1">
          Bibliothèque de consoles
        </p>
      </div>
    </div>
  )
}

export default function ConsolesPage() {
  const navigate = useNavigate()
  const [consoles, setConsoles] = useState<Item[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.items
      .list()
      .then((items) => {
        const consoles = items.filter((i) => i.type === 'console')
        setConsoles(consoles)
      })
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
      <PageHeader />

      {consoles.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-64 text-center">
          <p className="text-vault-on-surface-variant">Aucune console pour l'instant.</p>
          <p className="text-xs text-vault-outline mt-2">
            Ajoutez des consoles depuis l'application mobile.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {consoles.map((console) => (
            <ConsoleCard
              key={console.id}
              item={console}
              onClick={() => navigate(`/consoles/${console.id}`)}
            />
          ))}
        </div>
      )}
    </div>
  )
}
