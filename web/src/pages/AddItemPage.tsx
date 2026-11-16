import { useEffect, useMemo, useRef, useState } from 'react'
import { api } from '../api'
import type { IGDBSearchResult } from '../api'
import type { Item } from '../types'
import { Button, Chip, EmptyState, FilterChip, GameCover, Input, ListRow, PageHeader, PlatformBadge, Skeleton, SourceBadge, useToast } from '../components/ui'
import CsvImportModal from '../components/CsvImportModal'
import { shortPlatform } from '../lib/platforms'

/** A search hit: an IGDB result, matched to the catalogue when the title already exists there. */
interface Candidate {
  key: string
  title: string
  cover?: string
  year?: number
  platforms: string[]
  /** Catalogue items with this title, by platform. */
  local: Map<string, Item>
  igdb?: IGDBSearchResult
}

type Status = 'owned' | 'wishlist'

function slugify(s: string): string {
  return s
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
}

/** Add screen: search IGDB (and the shared catalogue), pick a result, confirm in a sheet. */
export default function AddItemPage() {
  const toast = useToast()
  const [catalogue, setCatalogue] = useState<Item[]>([])
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<IGDBSearchResult[]>([])
  const [searching, setSearching] = useState(false)
  const [selected, setSelected] = useState<Candidate | null>(null)
  const [csvOpen, setCsvOpen] = useState(false)
  const debounce = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    api.items.list().then(setCatalogue).catch(() => {})
  }, [])

  useEffect(() => {
    if (debounce.current) clearTimeout(debounce.current)
    const q = query.trim()
    if (q.length < 2) return
    debounce.current = setTimeout(async () => {
      setSearching(true)
      try {
        setResults(await api.search.igdb(q))
      } finally {
        setSearching(false)
      }
    }, 350)
    return () => {
      if (debounce.current) clearTimeout(debounce.current)
    }
  }, [query])

  const hasQuery = query.trim().length >= 2
  const candidates = useMemo(() => (hasQuery ? buildCandidates(query, results, catalogue) : []), [hasQuery, query, results, catalogue])

  async function add(candidate: Candidate, platform: string, status: Status) {
    let item = candidate.local.get(platform)
    if (!item) {
      const igdb = candidate.igdb
      item = await api.items.upsert({
        external_id: slugify(`${candidate.title}-${platform}-${igdb?.id ?? ''}`),
        title: candidate.title,
        platform,
        type: 'game',
        region: '',
        condition: '',
        status,
        igdb_id: igdb?.id ?? '',
        cover_url: candidate.cover ?? '',
        summary: igdb?.summary ?? '',
        genres: (igdb?.genres ?? []).join(', '),
        developer: igdb?.developer ?? '',
        publisher: igdb?.publisher ?? '',
        release_year: candidate.year ?? 0,
      })
      const created = item
      setCatalogue((prev) => [...prev.filter((i) => i.id !== created.id), created])
    }
    await (status === 'owned' ? api.collection.add(item.id) : api.wishlist.add(item.id))
    toast.push(status === 'owned' ? `${candidate.title} est dans ton coffre` : `${candidate.title} ajouté à tes envies`, 'success')
  }

  return (
    <div className="tv-enter flex flex-col gap-6 max-w-3xl">
      <PageHeader
        eyebrow="Ajout"
        title="Ajouter un objet"
        actions={
          <Button variant="secondary" size="sm" leftIcon="upload_file" onClick={() => setCsvOpen(true)}>
            Importer un CSV
          </Button>
        }
      />

      <Input
        autoFocus
        leftIcon="search"
        placeholder="Titre d'un jeu, d'une console…"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        aria-label="Rechercher un objet"
        rightAdornment={searching ? <span className="tv-led tv-led-indigo is-blink" aria-label="Recherche en cours" /> : undefined}
      />

      {!hasQuery ? (
        <div className="tv-panel p-5 flex flex-col gap-3">
          <SourceBadge>IGDB</SourceBadge>
          <p className="text-[15px] text-ink-muted">
            Cherche un titre : la fiche (jaquette, studio, année) est remplie depuis IGDB et rejoint le catalogue partagé. Pour ajouter toute ta collection
            d'un coup, importe ton export GameEye.
          </p>
        </div>
      ) : searching && candidates.length === 0 ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-16 rounded-md" />
          ))}
        </div>
      ) : candidates.length === 0 ? (
        <EmptyState title={`Aucun résultat pour « ${query.trim()} »`} description="Vérifie l'orthographe ou essaie le titre original." />
      ) : (
        <section aria-label="Résultats">
          <p className="tv-engrave mb-2">Résultats IGDB · {candidates.length}</p>
          <div className="tv-panel p-2 flex flex-col">
            {candidates.map((c) => (
              <ListRow
                key={c.key}
                onClick={() => setSelected(c)}
                cover={{ title: c.title, platform: c.platforms[0], src: c.cover }}
                title={c.title}
                meta={[c.year, c.platforms.slice(0, 3).map(shortPlatform).join(', ') + (c.platforms.length > 3 ? '…' : '')].filter(Boolean).join(' · ')}
                trailing={c.local.size > 0 ? <Chip tone="primary">Catalogue</Chip> : undefined}
              />
            ))}
          </div>
        </section>
      )}

      {selected && <AddSheet candidate={selected} onClose={() => setSelected(null)} onAdd={add} />}
      <CsvImportModal
        open={csvOpen}
        onClose={() => setCsvOpen(false)}
        onComplete={(report) => {
          if (report.upserted > 0) api.items.list().then(setCatalogue).catch(() => {})
        }}
      />
    </div>
  )
}

function buildCandidates(query: string, results: IGDBSearchResult[], catalogue: Item[]): Candidate[] {
  const byTitle = new Map<string, Item[]>()
  for (const i of catalogue) byTitle.set(i.title.toLowerCase(), [...(byTitle.get(i.title.toLowerCase()) ?? []), i])
  const fromIgdb: Candidate[] = results.slice(0, 12).map((r) => {
    const local = new Map((byTitle.get(r.title.toLowerCase()) ?? []).map((i) => [i.platform, i] as const))
    const platforms = [...new Set([...(r.platforms ?? []), ...local.keys()])]
    return { key: `igdb-${r.id}`, title: r.title, cover: r.cover_url || [...local.values()][0]?.cover_url, year: r.release_year, platforms, local, igdb: r }
  })
  if (fromIgdb.length > 0) return fromIgdb
  // IGDB unavailable or empty: fall back to the shared catalogue.
  const q = query.trim().toLowerCase()
  return catalogue
    .filter((i) => i.title.toLowerCase().includes(q))
    .slice(0, 12)
    .map((i) => ({ key: i.id, title: i.title, cover: i.cover_url || undefined, year: i.release_year || undefined, platforms: [i.platform], local: new Map([[i.platform, i]]) }))
}

interface SheetProps {
  candidate: Candidate
  onClose: () => void
  onAdd: (candidate: Candidate, platform: string, status: Status) => Promise<void>
}

/** Confirmation sheet: platform, then « Envie » or « Ajouter » (as the mobile quick add). */
function AddSheet({ candidate, onClose, onAdd }: SheetProps) {
  const toast = useToast()
  const [platform, setPlatform] = useState(candidate.platforms[0] ?? '')
  const [busy, setBusy] = useState<Status | null>(null)

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  async function submit(status: Status) {
    setBusy(status)
    try {
      await onAdd(candidate, platform, status)
      onClose()
    } catch (e) {
      toast.push(`Erreur lors de l'ajout : ${String(e)}`, 'error')
      setBusy(null)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-end md:items-center justify-center bg-scrim p-0 md:p-6" onClick={onClose}>
      <div role="dialog" aria-modal="true" aria-labelledby="add-sheet-title" className="tv-sheet tv-enter max-w-lg rounded-b-none md:rounded-b-xl" onClick={(e) => e.stopPropagation()}>
        <span className="tv-sheet-grip md:hidden" aria-hidden="true" />
        <div className="flex gap-4 items-center">
          <div className="w-20 shrink-0">
            <GameCover title={candidate.title} platform={platform} src={candidate.cover} />
          </div>
          <div className="flex flex-col gap-2 min-w-0">
            <h2 id="add-sheet-title" className="font-display text-[20px] font-extrabold tracking-tight leading-tight">
              {candidate.title}
            </h2>
            {platform && <PlatformBadge platform={platform} short={false} />}
            {candidate.year && <span className="tv-serial">{candidate.year}</span>}
          </div>
        </div>
        {candidate.platforms.length > 1 && (
          <div>
            <p className="tv-engrave mb-2">Plateforme</p>
            <div className="flex flex-wrap gap-2">
              {candidate.platforms.map((p) => (
                <FilterChip key={p} label={shortPlatform(p)} platform={p} selected={platform === p} onClick={() => setPlatform(p)} />
              ))}
            </div>
          </div>
        )}
        <p className="text-[13px] text-ink-muted">L'état, le prix payé et les détails de ton exemplaire se renseignent dans l'application mobile.</p>
        <div className="flex gap-3">
          <Button variant="secondary" size="lg" leftIcon="favorite" className="flex-1" disabled={busy !== null || !platform} onClick={() => submit('wishlist')}>
            {busy === 'wishlist' ? '…' : 'Envie'}
          </Button>
          <Button size="lg" leftIcon="inventory_2" className="flex-[1.6]" disabled={busy !== null || !platform} onClick={() => submit('owned')}>
            {busy === 'owned' ? 'Ajout…' : 'Ajouter'}
          </Button>
        </div>
        <button type="button" className="tv-link self-center" onClick={onClose}>
          Annuler
        </button>
      </div>
    </div>
  )
}
