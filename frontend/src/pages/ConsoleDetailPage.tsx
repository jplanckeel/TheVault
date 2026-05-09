import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Item } from '../types'

function ConsoleInfo({ console, onCoverUpdate }: { console: Item; onCoverUpdate: (url: string) => void }) {
  const [coverInput, setCoverInput] = useState('')
  const [saving, setSaving] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!coverInput.trim()) return
    setSaving(true)
    try {
      await api.items.updateCover(console.id, coverInput.trim())
      onCoverUpdate(coverInput.trim())
      setCoverInput('')
    } catch (err) {
      alert('Erreur: ' + err)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="flex gap-6 items-start mb-4">
      {/* Console image */}
      <div className="shrink-0 w-28 rounded-xl overflow-hidden bg-vault-surface border border-vault-outline-variant p-4">
        {console.cover_url ? (
          <img src={console.cover_url} alt={console.title} className="w-full h-auto object-contain" />
        ) : (
          <img
            src={`/controllercons/${console.platform.toLowerCase().replace(/\s+/g, '-')}.svg`}
            alt={console.platform}
            className="w-full h-auto object-contain"
            onError={(e) => {
              ;(e.target as HTMLImageElement).src = '/controllercons/joy-cons.svg'
            }}
          />
        )}
      </div>

      <div className="flex-1 min-w-0">
        <h1 className="text-2xl font-semibold text-vault-on-surface mb-1">
          {console.title}
        </h1>
        {console.release_year > 0 && (
          <p className="text-sm text-vault-on-surface-variant mb-3">
            Sortie : {console.release_year}
          </p>
        )}

        <div className="flex flex-wrap gap-2 mb-3">
          <span className="text-xs px-3 py-1 rounded-full bg-vault-secondary-container text-vault-secondary-text font-medium">
            {console.platform}
          </span>
          <span className="text-xs px-3 py-1 rounded-full bg-vault-secondary-container text-vault-secondary-text font-medium">
            {console.type}
          </span>
          {console.region && (
            <span className="text-xs px-3 py-1 rounded-full bg-vault-secondary-container text-vault-secondary-text font-medium">
              {console.region}
            </span>
          )}
        </div>

        {!console.cover_url && (
          <form onSubmit={handleSubmit} className="flex gap-2 mt-2">
            <input
              type="url"
              placeholder="URL de l'image…"
              value={coverInput}
              onChange={(e) => setCoverInput(e.target.value)}
              className="flex-1 px-3 py-1.5 text-sm rounded-lg border border-vault-outline-variant bg-vault-surface-low text-vault-on-surface placeholder:text-vault-on-surface-variant/50"
            />
            <button
              type="submit"
              disabled={saving}
              className="px-3 py-1.5 text-sm rounded-lg bg-vault-primary text-white font-medium disabled:opacity-50"
            >
              {saving ? '…' : 'Ajouter'}
            </button>
          </form>
        )}
      </div>
    </div>
  )
}

function GamesList({ games }: { games: Item[] }) {
  if (games.length === 0) {
    return (
      <p className="text-vault-on-surface-variant text-sm">
        Aucune licence enregistrée pour cette console.
      </p>
    )
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
      {games.map((game) => (
        <div
          key={game.id}
          className="bg-vault-surface border border-vault-outline-variant rounded-xl overflow-hidden cursor-pointer transition-all hover:border-vault-primary hover:shadow-[0_0_0_1px_#BA9EFF]"
          onClick={() => window.location.href = `/items/${game.id}`}
        >
          <div className="p-3 flex gap-3">
            {/* Cover thumbnail */}
            <div className="shrink-0 w-12 h-16 rounded-lg overflow-hidden bg-vault-surface-highest flex items-center justify-center">
              {game.cover_url ? (
                <img src={game.cover_url} alt={game.title} className="w-full h-full object-cover" />
              ) : (
                <span className="text-xl opacity-30">🎮</span>
              )}
            </div>

            <div className="flex-1 min-w-0">
              <h3 className="text-sm font-semibold text-vault-on-surface leading-snug mb-1">
                {game.title}
              </h3>
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-xs text-vault-on-surface-variant">
                  {game.release_year > 0 ? game.release_year : 'N/A'}
                </span>
                {game.genres && (
                  <>
                    <span className="text-vault-outline text-xs">·</span>
                    <span className="text-xs text-vault-on-surface-variant truncate max-w-[150px]">
                      {game.genres}
                    </span>
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}

function StatsCard({
  icon, label, value,
}: { icon: string; label: string; value: string | number }) {
  return (
    <div className="rounded-xl border border-vault-outline-variant bg-vault-surface p-5 flex-1">
      <div className="flex items-center gap-2 mb-3">
        <span className="text-lg">{icon}</span>
        <span className="text-xs font-semibold uppercase tracking-widest text-vault-on-surface-variant">
          {label}
        </span>
      </div>
      <p className="text-2xl font-bold text-vault-primary">{value}</p>
    </div>
  )
}

export default function ConsoleDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [console, setConsole] = useState<Item | null>(null)
  const [games, setGames] = useState<Item[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    Promise.all([api.items.get(id), api.items.list()])
      .then(([consoleItem, allItems]) => {
        setConsole(consoleItem)
        const games = allItems.filter((i) => i.type === 'game' && i.platform === consoleItem.platform)
        setGames(games)
      })
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
  if (!console) return <p className="text-vault-on-surface-variant">Console introuvable.</p>

  const totalGamesValue = games.reduce((acc, game) => {
    // Simplified: just count games for now
    return acc + 1
  }, 0)

  return (
    <div>
      <button
        onClick={() => navigate('/consoles')}
        className="flex items-center gap-2 text-sm text-vault-on-surface-variant hover:text-vault-primary transition-colors mb-6"
      >
        ← Retour aux consoles
      </button>

      <ConsoleInfo console={console} onCoverUpdate={(url) => setConsole({ ...console, cover_url: url })} />

      {/* Stats */}
      <div className="mb-8">
        <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-4">
          Statistiques
        </p>
        <div className="flex gap-4 flex-wrap">
          <StatsCard icon="🎮" label="Jeux" value={games.length} />
          <StatsCard icon="💰" label="Valeur totale" value={`${totalGamesValue} €`} />
          <StatsCard icon="📅" label="Année de sortie" value={console.release_year} />
        </div>
      </div>

      {/* Games */}
      <div>
        <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-4">
          Jeux associés ({games.length})
        </p>
        <GamesList games={games} />
      </div>
    </div>
  )
}
