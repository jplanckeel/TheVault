import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { IGDBSearchResult } from '../api'
import { useAuth } from '../AuthContext'
import type { Item } from '../types'
import {
  Button,
  Card,
  Chip,
  EmptyState,
  Input,
  MaterialIcon,
  Select,
  Skeleton,
  useToast,
} from '../components/ui'
import CsvImportModal from '../components/CsvImportModal'

/* -----------------------------------------------------------------------------
 * Models — unifie résultat IGDB et item local sous une "Suggestion"
 * --------------------------------------------------------------------------- */

interface Suggestion {
  /** Local item id if it exists in DB; otherwise IGDB id (string). */
  id: string
  /** True if already present in our DB (allowing direct add-to-collection). */
  isLocal: boolean
  title: string
  cover_url?: string
  release_year?: number
  platform?: string
  summary?: string
  /** Heuristic badge for the card top-left. */
  badge?: { label: string; tone: 'primary' | 'secondary' | 'error' | 'success' }
  /** Raw IGDB payload — present when isLocal is false. Used to upsert on add. */
  igdb?: IGDBSearchResult
}

const CONDITION_OPTIONS = [
  { value: 'sealed', label: 'Sealed / New' },
  { value: 'cib', label: 'Complete in Box (CIB)' },
  { value: 'loose', label: 'Loose' },
  { value: 'graded', label: 'Graded' },
  { value: 'mint', label: 'Mint' },
]

/* -----------------------------------------------------------------------------
 * Suggestion card
 * --------------------------------------------------------------------------- */

function SuggestionCard({
  suggestion,
  onAdd,
  added,
  pending,
}: {
  suggestion: Suggestion
  onAdd: (condition: string) => void
  added: boolean
  pending: boolean
}) {
  const [condition, setCondition] = useState('sealed')

  return (
    <Card padding="none" radius="2xl" className="overflow-hidden flex flex-col">
      <div className="relative aspect-[16/10] overflow-hidden bg-vault-surface-high">
        {suggestion.cover_url ? (
          <img
            src={suggestion.cover_url}
            alt={suggestion.title}
            loading="lazy"
            className="h-full w-full object-cover"
          />
        ) : (
          <div className="h-full w-full flex items-center justify-center text-5xl opacity-20">
            🎮
          </div>
        )}
        {suggestion.badge && (
          <div className="absolute top-3 left-3">
            <Chip tone={suggestion.badge.tone} variant="solid" size="md">
              {suggestion.badge.label}
            </Chip>
          </div>
        )}
        {!suggestion.isLocal && (
          <div className="absolute top-3 right-3">
            <Chip tone="primary" variant="soft" icon="auto_awesome" size="sm">
              IGDB
            </Chip>
          </div>
        )}
        <div className="absolute inset-x-0 bottom-0 h-1/2 bg-gradient-to-t from-black/85 via-black/30 to-transparent" />
      </div>

      <div className="p-4 flex flex-col gap-3 flex-1">
        <div>
          <h3 className="font-display text-base font-bold text-vault-on-surface line-clamp-2">
            {suggestion.title}
          </h3>
          <p className="mt-1 text-[10px] tracking-widest font-bold uppercase text-vault-on-surface-variant">
            {suggestion.release_year ? `${suggestion.release_year} • ` : ''}
            {suggestion.platform || 'Multi-platform'}
          </p>
        </div>

        <Select
          label="Condition"
          value={condition}
          onChange={(e) => setCondition(e.target.value)}
          options={CONDITION_OPTIONS}
        />

        <Button
          fullWidth
          leftIcon={added ? 'check_circle' : suggestion.isLocal ? 'add_circle' : 'auto_awesome'}
          onClick={() => onAdd(condition)}
          disabled={pending || added}
          variant={added ? 'subtle' : 'primary'}
          className="mt-auto"
          title={
            !suggestion.isLocal
              ? "Cet item IGDB sera importé dans le catalogue puis ajouté à ta vault"
              : undefined
          }
        >
          {added
            ? 'Added to Vault'
            : pending
              ? 'Adding…'
              : suggestion.isLocal
                ? 'Quick Add to Vault'
                : 'Import & Add to Vault'}
        </Button>
      </div>
    </Card>
  )
}

/* -----------------------------------------------------------------------------
 * Page
 * --------------------------------------------------------------------------- */

export default function AddItemPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const toast = useToast()

  const [allItems, setAllItems] = useState<Item[]>([])
  const [igdbResults, setIgdbResults] = useState<IGDBSearchResult[]>([])
  const [query, setQuery] = useState('')
  const [searching, setSearching] = useState(false)
  const [loadingItems, setLoadingItems] = useState(true)
  const [addingId, setAddingId] = useState<string | null>(null)
  const [added, setAdded] = useState<Set<string>>(new Set())
  const [igdbAvailable, setIgdbAvailable] = useState(true)
  const [csvOpen, setCsvOpen] = useState(false)

  /** Load local DB once (used both for "Suggested" landing and for matching IGDB hits to existing rows). */
  useEffect(() => {
    api.items
      .list()
      .then(setAllItems)
      .catch(() => {})
      .finally(() => setLoadingItems(false))
  }, [])

  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current)
    if (!query.trim() || !user) {
      setIgdbResults([])
      return
    }
    debounceRef.current = setTimeout(async () => {
      setSearching(true)
      try {
        const r = await api.search.igdb(query.trim())
        setIgdbResults(r)
        setIgdbAvailable(r.length > 0 || query.trim().length < 2)
      } finally {
        setSearching(false)
      }
    }, 350)
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current)
    }
  }, [query, user])

  /** Suggestions: IGDB results when available, otherwise local catalogue filtered by query. */
  const suggestions: Suggestion[] = useMemo(() => {
    const q = query.trim().toLowerCase()
    const localFiltered = q
      ? allItems.filter(
          (i) =>
            i.title.toLowerCase().includes(q) ||
            (i.platform || '').toLowerCase().includes(q),
        )
      : allItems.slice(0, 9)

    if (igdbResults.length > 0) {
      return igdbResults.slice(0, 9).map((r) => {
        const localMatch = allItems.find((i) => i.title.toLowerCase() === r.title.toLowerCase())
        return {
          id: localMatch?.id ?? r.id,
          isLocal: Boolean(localMatch),
          title: r.title,
          cover_url: r.cover_url || localMatch?.cover_url,
          release_year: r.release_year,
          platform: r.platforms?.[0] || localMatch?.platform,
          summary: r.summary,
          badge: pickBadge(r.title, r.release_year),
          igdb: r,
        }
      })
    }

    return localFiltered.map((i) => ({
      id: i.id,
      isLocal: true,
      title: i.title,
      cover_url: i.cover_url,
      release_year: i.release_year,
      platform: i.platform,
      summary: i.summary,
      badge: pickBadge(i.title, i.release_year),
    }))
  }, [igdbResults, allItems, query])

  function slugify(s: string): string {
    return s
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-|-$/g, '')
  }

  async function handleAdd(suggestion: Suggestion, condition: string) {
    setAddingId(suggestion.id)
    try {
      let itemId = suggestion.id

      // If the item only exists in IGDB, upsert it into our catalogue first.
      if (!suggestion.isLocal) {
        if (!suggestion.igdb) {
          throw new Error('Données IGDB manquantes pour cet item')
        }
        const igdb = suggestion.igdb
        const platform = igdb.platforms?.[0] || 'Unknown'
        const created = await api.items.upsert({
          external_id: slugify(`${igdb.title}-${platform}-${igdb.id}`),
          title: igdb.title,
          platform,
          type: 'game',
          region: '',
          condition,
          status: 'owned',
          igdb_id: igdb.id,
          cover_url: igdb.cover_url || '',
          summary: igdb.summary || '',
          genres: (igdb.genres || []).join(', '),
          developer: igdb.developer || '',
          publisher: igdb.publisher || '',
          release_year: igdb.release_year || 0,
        })
        itemId = created.id
        // Refresh local catalogue cache.
        setAllItems((prev) => [...prev.filter((i) => i.id !== created.id), created])
      }

      await api.collection.add(itemId)
      setAdded((prev) => new Set(prev).add(suggestion.id))
      toast.push(`${suggestion.title} ajouté à la vault`, 'success')
    } catch (e) {
      toast.push(`Erreur lors de l'ajout : ${String(e)}`, 'error')
    } finally {
      setAddingId(null)
    }
  }

  /* ---- Render ---- */

  return (
    <div className="space-y-6 animate-fade-in-up">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <h1 className="text-display-lg text-vault-on-surface">Add New Item</h1>
        <Chip
          tone={igdbAvailable ? 'success' : 'neutral'}
          variant="soft"
          icon="circle"
          size="md"
        >
          {igdbAvailable ? 'API: IGDB CONNECTED' : 'API: LOCAL CATALOGUE'}
        </Chip>
      </div>

      {/* Search */}
      <Card padding="md" radius="2xl" glass>
        <div className="flex gap-3 flex-wrap">
          <div className="flex-1 min-w-[280px]">
            <Input
              leftIcon="search"
              placeholder="Search games, consoles, or limited editions..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              size="lg"
              rightAdornment={
                searching ? (
                  <span className="text-vault-primary text-xs px-2">Searching…</span>
                ) : undefined
              }
            />
          </div>
          <Button variant="ghost" size="lg" leftIcon="filter_list">
            Filters
          </Button>
          <Button variant="primary" size="lg" leftIcon="search">
            Search
          </Button>
        </div>
      </Card>

      {/* Results */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <MaterialIcon name="auto_awesome" className="text-vault-primary" size={22} filled />
            <h2 className="text-headline-md text-vault-on-surface">Suggested Results</h2>
          </div>
          <p className="text-label-bold text-vault-on-surface-variant">
            {suggestions.length} ITEM{suggestions.length > 1 ? 'S' : ''} FOUND
            {igdbResults.length === 0 && query.trim() === '' && ' (LOCAL)'}
          </p>
        </div>

        {loadingItems ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-96" />
            ))}
          </div>
        ) : suggestions.length === 0 ? (
          <EmptyState
            icon={query ? 'search_off' : 'travel_explore'}
            title={query ? `Aucun résultat pour « ${query} »` : 'Démarre une recherche'}
            description="Recherche un titre, un studio, ou une plateforme. Les résultats sont fournis par IGDB. Tu peux ajouter directement à ta vault, le catalogue partagé sera enrichi automatiquement."
          />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {suggestions.map((s) => (
              <SuggestionCard
                key={s.id}
                suggestion={s}
                onAdd={(c) => handleAdd(s, c)}
                pending={addingId === s.id}
                added={added.has(s.id)}
              />
            ))}
          </div>
        )}
      </div>

      {/* Bulk + Manual */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Card padding="lg" radius="2xl" className="border-dashed border-vault-outline-variant/40">
          <div className="flex items-start gap-4">
            <div className="h-12 w-12 rounded-xl bg-vault-primary/15 text-vault-primary flex items-center justify-center shrink-0">
              <MaterialIcon name="upload_file" size={22} />
            </div>
            <div className="min-w-0">
              <h3 className="text-headline-sm text-vault-on-surface">Bulk CSV Import</h3>
              <p className="text-sm text-vault-on-surface-variant mt-1">
                Importe ta collection PriceCharting d'un coup.
              </p>
              <Button
                variant="ghost"
                size="sm"
                leftIcon="upload"
                className="mt-4"
                onClick={() => setCsvOpen(true)}
              >
                Choisir un CSV
              </Button>
            </div>
          </div>
        </Card>

        <Card padding="lg" radius="2xl" className="border-dashed border-vault-outline-variant/40">
          <div className="flex items-start gap-4">
            <div className="h-12 w-12 rounded-xl bg-vault-primary/15 text-vault-primary flex items-center justify-center shrink-0">
              <MaterialIcon name="edit_note" size={22} />
            </div>
            <div className="min-w-0">
              <h3 className="text-headline-sm text-vault-on-surface">Manual Entry</h3>
              <p className="text-sm text-vault-on-surface-variant mt-1">
                Variantes, prototypes, ou items hors-catalogue.
              </p>
              <Button
                variant="ghost"
                size="sm"
                leftIcon="add"
                className="mt-4"
                onClick={() => navigate('/jobs')}
              >
                Saisie manuelle
              </Button>
            </div>
          </div>
        </Card>
      </div>

      {/* Status footer */}
      <div className="flex flex-col md:flex-row gap-4 md:items-center justify-between text-label-bold text-vault-on-surface-variant pt-4 border-t border-vault-outline-variant/15">
        <div className="flex items-center gap-3 flex-1 max-w-md">
          <span>Catalogue size</span>
          <div className="flex-1 h-1.5 rounded-full bg-vault-surface-high overflow-hidden">
            <div
              className="h-full rounded-full bg-gradient-to-r from-vault-primary-deep to-vault-primary glow-purple"
              style={{ width: `${Math.min(100, allItems.length / 20)}%` }}
            />
          </div>
          <span className="tabular-nums">{allItems.length} items</span>
        </div>
        <div className="flex items-center gap-4">
          <span className="flex items-center gap-2">
            <MaterialIcon name="schedule" size={14} />
            LAST SYNC: LIVE
          </span>
          <span className="flex items-center gap-2">
            <span className="h-2 w-2 rounded-full bg-vault-success animate-pulse" />
            SERVER: OPTIMAL
          </span>
        </div>
      </div>

      <CsvImportModal
        open={csvOpen}
        onClose={() => setCsvOpen(false)}
        onComplete={(report) => {
          // Refresh local catalogue if the import created new items.
          if (report.upserted > 0) {
            api.items
              .list()
              .then(setAllItems)
              .catch(() => {})
          }
        }}
      />
    </div>
  )
}

/* -----------------------------------------------------------------------------
 * Helpers
 * --------------------------------------------------------------------------- */

function pickBadge(
  title: string,
  year?: number,
): { label: string; tone: 'primary' | 'secondary' | 'error' | 'success' } | undefined {
  const t = title.toLowerCase()
  if (year && year >= new Date().getFullYear() - 1) return { label: 'NEW RELEASE', tone: 'primary' }
  if (t.includes('limited') || t.includes('collector')) return { label: 'RARE FIND', tone: 'error' }
  if (t.includes('console') || t.includes('controller') || t.includes('hardware'))
    return { label: 'HARDWARE', tone: 'secondary' }
  if (year && year < 2005) return { label: 'RETRO', tone: 'secondary' }
  return { label: 'MINT CANDIDATE', tone: 'primary' }
}
