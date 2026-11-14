import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'

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

const euro = (n: number) => n.toLocaleString('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 })

const COMPLETENESS: Record<string, string> = { LOOSE: 'Loose', CIB: 'Complet', NEW: 'Neuf' }

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

  if (error) {
    return (
      <main className="min-h-screen bg-vault-surface text-vault-on-surface flex items-center justify-center p-6">
        <p className="text-base">Ce lien n'existe pas ou a été révoqué.</p>
      </main>
    )
  }
  if (!view) {
    return <main className="min-h-screen bg-vault-surface" aria-busy="true" />
  }

  const heading = view.title || (view.scope === 'wishlist' ? 'Liste d’envies' : 'Collection')
  return (
    <main className="min-h-screen bg-vault-surface text-vault-on-surface px-4 py-8 sm:px-8">
      <header className="max-w-6xl mx-auto mb-8">
        <p className="text-label-bold uppercase tracking-widest text-vault-on-surface-variant">TheVault · partage en lecture seule</p>
        <h1 className="text-headline-md mt-2">{heading}</h1>
        <p className="text-sm text-vault-on-surface-variant mt-2">
          {view.count} objets
          {view.total != null && <> · {euro(view.total)}</>}
          {' · '}mis à jour le {new Date(view.updated_at).toLocaleDateString('fr-FR')}
        </p>
      </header>
      <ul className="max-w-6xl mx-auto grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-6 gap-4">
        {view.items.map((item, i) => (
          <li key={`${item.title}-${item.platform}-${i}`} className="flex flex-col gap-2">
            <div className="aspect-[3/4] rounded-lg overflow-hidden bg-vault-surface-high">
              {item.coverUrl ? (
                <img src={item.coverUrl} alt="" loading="lazy" className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full flex items-end p-2 text-sm font-bold">{item.title}</div>
              )}
            </div>
            <p className="text-sm font-semibold leading-tight line-clamp-2">{item.title}</p>
            <p className="text-xs text-vault-on-surface-variant">
              {item.platform}
              {item.completeness && <> · {COMPLETENESS[item.completeness] ?? item.completeness}</>}
              {item.value != null && <> · {euro(item.value)}</>}
            </p>
          </li>
        ))}
      </ul>
    </main>
  )
}
