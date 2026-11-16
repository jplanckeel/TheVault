import { useState } from 'react'
import type { AdminItem, ImageCandidate, ImageCandidates } from '../../types'
import { api } from '../../api'
import { Button, Chip, MaterialIcon, Skeleton, useToast } from '../ui'

interface Props {
  item: AdminItem
  /** Called once the chosen image is the item cover. */
  onApplied: () => void
}

const SOURCE: Record<ImageCandidate['source'], { label: string; hint: string }> = {
  commons: { label: 'Wikimedia', hint: 'Photo libre de droits (domaine public).' },
  igdb: { label: 'IGDB', hint: 'Jaquette IGDB, y compris des éditions et versions du jeu.' },
  ebay: { label: 'eBay', hint: "Photo d'une annonce eBay : vérifie qu'elle montre bien l'objet." },
}

const UNAVAILABLE: Record<string, string> = { ebay: 'eBay', igdb: 'IGDB' }

/**
 * Cover suggestions of an item (free Commons photo for consoles, IGDB covers of every
 * edition, eBay listing photos). Searched on demand — each search uses the eBay quota —
 * then the chosen image is copied to the server and protected from syncs.
 */
export default function ImageSuggestions({ item, onApplied }: Props) {
  const toast = useToast()
  const [data, setData] = useState<ImageCandidates | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [chosen, setChosen] = useState<ImageCandidate | null>(null)
  const [applying, setApplying] = useState(false)

  async function search() {
    setLoading(true)
    setError(null)
    setChosen(null)
    try {
      setData(await api.admin.imageCandidates(item.id))
    } catch (e) {
      setError(String(e))
    } finally {
      setLoading(false)
    }
  }

  async function apply(candidate: ImageCandidate) {
    setApplying(true)
    try {
      await api.admin.importCover(item.id, candidate.url)
      toast.push('Jaquette mise à jour', 'success')
      setChosen(null)
      onApplied()
    } catch (e) {
      toast.push(`Erreur : ${String(e)}`, 'error')
    } finally {
      setApplying(false)
    }
  }

  if (!data && !loading) {
    return (
      <div className="space-y-3">
        <p className="text-sm text-ink-muted">
          Cherche des images pour cet objet : photo libre de la console, jaquettes IGDB de toutes les éditions, photos des annonces eBay.
        </p>
        {error && <p className="text-sm text-loss">{error}</p>}
        <Button size="sm" variant="secondary" leftIcon="image_search" onClick={search}>
          Chercher des images
        </Button>
      </div>
    )
  }

  if (loading || !data) {
    return (
      <div className="grid grid-cols-3 sm:grid-cols-4 gap-3" aria-busy>
        {Array.from({ length: 8 }).map((_, i) => (
          <Skeleton key={i} className="aspect-[3/4] rounded-md" />
        ))}
      </div>
    )
  }

  const missing = data.unavailable.map((s) => UNAVAILABLE[s] ?? s)
  return (
    <div className="space-y-4">
      {missing.length > 0 && (
        <p className="text-xs text-ink-muted">
          <MaterialIcon name="info" size={14} className="mr-1 align-[-2px]" />
          Indisponible : {missing.join(', ')} (identifiants absents, quota ou réseau).
        </p>
      )}
      {data.candidates.length === 0 ? (
        <p className="text-sm text-ink-muted">Aucune image trouvée. Tu peux importer un fichier avec « Importer une jaquette ».</p>
      ) : (
        <ul className="grid grid-cols-3 sm:grid-cols-4 gap-3">
          {data.candidates.map((c) => {
            const selected = chosen?.url === c.url
            return (
              <li key={c.url}>
                <button
                  type="button"
                  onClick={() => setChosen(selected ? null : c)}
                  aria-pressed={selected}
                  title={c.label}
                  className={`group block w-full rounded-md bg-well overflow-hidden focus-ring ${selected ? 'ring-2 ring-indigo' : ''}`}
                >
                  <img src={c.url} alt={c.label} loading="lazy" referrerPolicy="no-referrer" className="aspect-[3/4] w-full object-contain" />
                </button>
                <div className="mt-1 flex items-center gap-1">
                  <Chip tone={c.source === 'commons' ? 'success' : 'neutral'}>{SOURCE[c.source].label}</Chip>
                  <span className="text-[11px] text-ink-muted truncate" title={c.label}>{c.label}</span>
                </div>
              </li>
            )
          })}
        </ul>
      )}
      {chosen && (
        <div className="rounded-md bg-well p-3 space-y-2">
          <p className="text-sm text-ink">{SOURCE[chosen.source].hint}</p>
          <div className="flex flex-wrap gap-2">
            <Button size="sm" leftIcon="check" disabled={applying} onClick={() => apply(chosen)}>
              Utiliser cette image
            </Button>
            {chosen.page_url && (
              <a className="tv-btn tv-btn-sm tv-btn-ghost" href={chosen.page_url} target="_blank" rel="noopener noreferrer">
                Voir la source
              </a>
            )}
            <Button size="sm" variant="ghost" onClick={() => setChosen(null)}>
              Annuler
            </Button>
          </div>
        </div>
      )}
      <Button size="sm" variant="ghost" leftIcon="refresh" onClick={search}>
        Relancer la recherche
      </Button>
    </div>
  )
}
