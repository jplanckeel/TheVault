import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { CollectionCard, EmptyState, Logo, Skeleton, SourceBadge } from '../components/ui'
import { formatEuro } from '../lib/format'
import type { Condition } from '../lib/condition'

/** One shared item (the backend never sends notes, purchase prices or storage places). */
interface SharedItem {
  kind: string
  title: string
  platform: string
  coverUrl?: string
  completeness?: string
  edition?: string
  value?: number | null
}

interface PublicView {
  title: string
  scope: 'collection' | 'wishlist'
  updated_at: string
  count: number
  total: number | null
  items: SharedItem[]
}

const COMPLETENESS: Record<string, Condition> = { LOOSE: 'loose', CIB: 'cib', NEW: 'new' }

/** Read-only public page of a shared collection or wishlist (no account needed). */
export default function SharePage() {
  const { token = '' } = useParams()
  const [view, setView] = useState<PublicView | null>(null)
  const [error, setError] = useState(false)

  useEffect(() => {
    fetch(`/api/v1/public/shares/${encodeURIComponent(token)}`)
      .then((res) => (res.ok ? (res.json() as Promise<PublicView>) : Promise.reject(new Error(String(res.status)))))
      .then(setView)
      .catch(() => setError(true))
  }, [token])

  return (
    <main className="min-h-screen bg-shell text-ink px-4 py-8 sm:px-8">
      <div className="max-w-6xl mx-auto flex flex-col gap-8">
        <Logo size={30} />
        {error ? (
          <EmptyState title="Lien introuvable" description="Ce lien n'existe pas ou a été révoqué par son propriétaire." />
        ) : !view ? (
          <div className="grid grid-cols-3 sm:grid-cols-5 lg:grid-cols-7 gap-3" aria-busy="true">
            {Array.from({ length: 14 }).map((_, i) => (
              <Skeleton key={i} className="aspect-[3/4] rounded-sm" />
            ))}
          </div>
        ) : (
          <>
            <header className="flex flex-col gap-2">
              <span className="tv-engrave">Partage en lecture seule · {view.scope === 'wishlist' ? 'Envies' : 'Collection'}</span>
              <h1 className="tv-title">{view.title || (view.scope === 'wishlist' ? 'Liste d’envies' : 'Collection')}</h1>
              <div className="flex flex-wrap items-center gap-x-4 gap-y-1">
                <span className="tv-price text-[15px]">
                  {view.count.toLocaleString('fr-FR')} objets{view.total != null ? ` · ${formatEuro(view.total)}` : ''}
                </span>
                <SourceBadge updated={new Date(view.updated_at).toLocaleDateString('fr-FR')}>Mis à jour</SourceBadge>
              </div>
            </header>
            <ul className="grid grid-cols-3 sm:grid-cols-5 lg:grid-cols-7 gap-x-3 gap-y-5">
              {view.items.map((item, i) => (
                <li key={`${item.title}-${item.platform}-${i}`}>
                  <CollectionCard
                    title={item.title}
                    platform={item.platform}
                    src={item.coverUrl || undefined}
                    condition={item.completeness ? COMPLETENESS[item.completeness] ?? null : null}
                    value={item.value != null ? formatEuro(item.value) : view.total == null ? '' : '—'}
                  />
                </li>
              ))}
            </ul>
          </>
        )}
      </div>
    </main>
  )
}
