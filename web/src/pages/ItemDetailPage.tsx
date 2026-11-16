import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../AuthContext'
import type { Item, PriceSnapshot } from '../types'
import { Button, Chip, CollectionCard, EmptyState, GameCover, PageHeader, PlatformBadge, Section, Skeleton, SourceBadge, useToast } from '../components/ui'
import PricePanel from '../components/PricePanel'
import { conditionLabel, parseCondition, type PriceCondition } from '../lib/condition'
import { itemPath } from '../lib/routes'

type Ownership = 'none' | 'owned' | 'wishlist'

const KIND_LABEL: Record<Item['type'], string> = { game: 'Jeu', console: 'Console', accessory: 'Accessoire' }

/** Item sheet (games, consoles and accessories): cover, details, eBay quote, add to the vault. */
export default function ItemDetailPage() {
  const { id = '' } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const toast = useToast()
  const { user, isAdmin } = useAuth()

  const [item, setItem] = useState<Item | null>(null)
  const [prices, setPrices] = useState<PriceSnapshot[]>([])
  const [related, setRelated] = useState<Item[]>([])
  const [ownership, setOwnership] = useState<Ownership>('none')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const coverInput = useRef<HTMLInputElement>(null)

  // Show the loader again when navigating to another item (state adjusted during render).
  const [loadedFor, setLoadedFor] = useState(id)
  if (loadedFor !== id) {
    setLoadedFor(id)
    setItem(null)
    setError(null)
  }

  useEffect(() => {
    Promise.all([api.items.get(id), api.items.prices(id)])
      .then(([i, p]) => {
        setItem(i)
        setPrices(p)
        if (i.type === 'console') {
          api.items
            .list()
            .then((all) => setRelated(all.filter((g) => g.type === 'game' && g.platform === i.platform)))
            .catch(() => {})
        } else setRelated([])
      })
      .catch((e) => setError(String(e)))
  }, [id])

  useEffect(() => {
    if (!user) return
    Promise.all([api.collection.list(), api.wishlist.list()])
      .then(([owned, wished]) => setOwnership(owned.some((i) => i.id === id) ? 'owned' : wished.some((i) => i.id === id) ? 'wishlist' : 'none'))
      .catch(() => {})
  }, [user, id])

  const condition = item ? parseCondition(item.condition) : null
  const initialQuote: PriceCondition | undefined = condition === 'sealed' ? 'new' : (condition ?? undefined)
  const facts = useMemo(() => (item ? itemFacts(item) : []), [item])

  async function run(action: () => Promise<void>, next: Ownership, message: string) {
    setBusy(true)
    try {
      await action()
      setOwnership(next)
      toast.push(message, 'success')
    } catch (e) {
      toast.push(`Erreur : ${String(e)}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  async function uploadCover(file: File) {
    if (!item) return
    try {
      const { cover_url } = await api.items.uploadCover(item.id, file)
      setItem({ ...item, cover_url })
      toast.push('Jaquette mise à jour', 'success')
    } catch (e) {
      toast.push(`Erreur : ${String(e)}`, 'error')
    }
  }

  if (error) {
    return (
      <EmptyState
        title="Objet introuvable"
        description="Cet objet n'existe pas ou a été retiré du catalogue."
        cta={{ label: "Retour à l'Explorer", icon: 'arrow_back', onClick: () => navigate('/catalogue') }}
      />
    )
  }
  if (!item) return <DetailSkeleton />

  return (
    <div className="tv-enter flex flex-col gap-8">
      <PageHeader back eyebrow={`Fiche · ${KIND_LABEL[item.type]}`} title={item.title} />

      <div className="grid grid-cols-1 lg:grid-cols-[280px_1fr] gap-6 lg:gap-10 items-start">
        <div className="flex flex-col gap-3 max-w-[240px] lg:max-w-none mx-auto lg:mx-0 w-full">
          <GameCover size="l" title={item.title} platform={item.platform} src={item.cover_url || undefined} condition={condition} />
          {isAdmin && (
            <>
              <Button variant="ghost" size="sm" leftIcon="image" onClick={() => coverInput.current?.click()}>
                Changer la jaquette
              </Button>
              <input
                ref={coverInput}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                hidden
                onChange={(e) => {
                  const f = e.target.files?.[0]
                  if (f) uploadCover(f)
                  e.target.value = ''
                }}
              />
            </>
          )}
        </div>

        <div className="flex flex-col gap-6 min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            {item.platform && <PlatformBadge platform={item.platform} region={item.region || undefined} short={false} />}
            {condition && <Chip>{conditionLabel(condition)}</Chip>}
            {item.igdb_id && <SourceBadge>Fiche IGDB</SourceBadge>}
          </div>

          {facts.length > 0 && (
            <dl className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {facts.map((f) => (
                <div key={f.label} className="tv-well px-3 py-2.5 min-w-0">
                  <dt className="tv-engrave">{f.label}</dt>
                  <dd className="mt-1 text-[14px] font-semibold truncate" title={f.value}>
                    {f.value}
                  </dd>
                </div>
              ))}
            </dl>
          )}

          {item.summary && <p className="text-[15px] text-ink-muted max-w-3xl line-clamp-6">{item.summary}</p>}

          <Actions
            loggedIn={user !== null}
            ownership={ownership}
            busy={busy}
            onLogin={() => navigate('/login')}
            onAdd={() => run(() => api.collection.add(item.id), 'owned', `${item.title} est dans ton coffre`)}
            onWish={() => run(() => api.wishlist.add(item.id), 'wishlist', `${item.title} ajouté à tes envies`)}
            onRemove={() => run(() => api.collection.remove(item.id), 'none', `${item.title} retiré`)}
          />

          <PricePanel key={item.id} prices={prices} searchQuery={`${item.title} ${item.platform}`.trim()} initial={initialQuote} />
        </div>
      </div>

      {related.length > 0 && (
        <Section title={`Jeux sur ${item.platform}`} description={`${related.length} dans le catalogue`}>
          <div className="grid grid-cols-3 sm:grid-cols-4 lg:grid-cols-6 gap-x-3 gap-y-5">
            {related.slice(0, 24).map((g) => (
              <CollectionCard key={g.id} to={itemPath(g)} title={g.title} platform={g.platform} src={g.cover_url || undefined} />
            ))}
          </div>
        </Section>
      )}
    </div>
  )
}

interface ActionsProps {
  loggedIn: boolean
  ownership: Ownership
  busy: boolean
  onLogin: () => void
  onAdd: () => void
  onWish: () => void
  onRemove: () => void
}

/** One indigo action per screen: « Ajouter » until the item is in the vault. */
function Actions({ loggedIn, ownership, busy, onLogin, onAdd, onWish, onRemove }: ActionsProps) {
  if (!loggedIn) {
    return (
      <Button size="lg" leftIcon="login" onClick={onLogin} className="self-start">
        Se connecter pour l'ajouter
      </Button>
    )
  }
  if (ownership === 'owned') {
    return (
      <div className="flex flex-wrap items-center gap-3">
        <Chip tone="success">Dans ton coffre</Chip>
        <Button variant="ghost" size="sm" onClick={onRemove} disabled={busy}>
          Retirer
        </Button>
      </div>
    )
  }
  return (
    <div className="flex flex-wrap items-center gap-3">
      <Button size="lg" leftIcon="inventory_2" onClick={onAdd} disabled={busy}>
        {ownership === 'wishlist' ? "Je l'ai" : 'Ajouter à mon coffre'}
      </Button>
      {ownership === 'wishlist' ? (
        <Button variant="ghost" onClick={onRemove} disabled={busy}>
          Retirer des envies
        </Button>
      ) : (
        <Button variant="secondary" size="lg" leftIcon="favorite" onClick={onWish} disabled={busy}>
          Envie
        </Button>
      )}
    </div>
  )
}

/** Known catalogue facts only; empty fields are left out. */
function itemFacts(item: Item): { label: string; value: string }[] {
  const facts: { label: string; value: string }[] = []
  if (item.release_year > 0) facts.push({ label: 'Sortie', value: String(item.release_year) })
  if (item.developer) facts.push({ label: 'Studio', value: item.developer })
  if (item.publisher) facts.push({ label: 'Éditeur', value: item.publisher })
  if (item.genres) facts.push({ label: 'Genre', value: item.genres.split(',').slice(0, 2).map((g) => g.trim()).join(', ') })
  return facts
}

function DetailSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <Skeleton variant="text" lines={2} className="max-w-sm" />
      <div className="grid grid-cols-1 lg:grid-cols-[280px_1fr] gap-10">
        <Skeleton className="aspect-[3/4] rounded-sm" />
        <Skeleton className="h-80 rounded-lg" />
      </div>
    </div>
  )
}
