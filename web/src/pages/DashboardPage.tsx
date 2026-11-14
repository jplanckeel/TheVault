import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../AuthContext'
import type { Item, PriceSnapshot, TopMover } from '../types'
import { CollectionCard, EmptyState, ListRow, PageHeader, PriceDelta, Section, Skeleton, SourceBadge, Stat } from '../components/ui'
import { itemPath } from '../lib/routes'
import { bestPrice, buildPriceMap, totalValue, type PriceMap } from '../lib/prices'
import { formatEuro, formatEuroOrDash, formatRelative } from '../lib/format'
import { conditionLabel, parseCondition } from '../lib/condition'
import { shortPlatform } from '../lib/platforms'

const SHELF_COUNT = 6

/** Home: vault value, counters, top pieces, market risers and latest additions — real data only. */
export default function DashboardPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const [collection, setCollection] = useState<Item[]>([])
  const [wishlistCount, setWishlistCount] = useState(0)
  const [snapshots, setSnapshots] = useState<PriceSnapshot[]>([])
  const [movers, setMovers] = useState<TopMover[]>([])
  const [catalogue, setCatalogue] = useState<Item[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([api.collection.list(), api.wishlist.list(), api.prices.latest()])
      .then(([c, w, s]) => {
        setCollection(c)
        setWishlistCount(w.length)
        setSnapshots(s)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
    // Market risers are optional: a failure simply hides the section.
    Promise.all([api.prices.topMovers(30, 5), api.items.list()])
      .then(([m, items]) => {
        setMovers(m)
        setCatalogue(items)
      })
      .catch(() => {})
  }, [])

  const prices = useMemo(() => buildPriceMap(snapshots), [snapshots])
  const latest = useMemo(() => [...collection].sort((a, b) => b.created_at.localeCompare(a.created_at)).slice(0, SHELF_COUNT), [collection])
  const topPieces = useMemo(
    () =>
      collection
        .map((item) => ({ item, value: bestPrice(item, prices) }))
        .filter((e): e is { item: Item; value: number } => e.value !== undefined)
        .sort((a, b) => b.value - a.value)
        .slice(0, SHELF_COUNT),
    [collection, prices],
  )
  const lastQuote = useMemo(() => snapshots.reduce<string | null>((acc, s) => (!acc || s.scraped_at > acc ? s.scraped_at : acc), null), [snapshots])

  if (loading) return <DashboardSkeleton />

  const name = user ? user.name || user.email.split('@')[0] : ''
  const count = (type: Item['type']) => collection.filter((i) => i.type === type).length
  return (
    <div className="tv-enter flex flex-col gap-8">
      <PageHeader eyebrow={name ? `Joueur 1 · ${name}` : 'Joueur 1'} title="Mon coffre" />

      {collection.length === 0 ? (
        <EmptyState
          title="Ton coffre est vide"
          description="Ajoute ton premier objet ou importe ton export GameEye pour voir la valeur de ta collection."
          cta={{ label: 'Ajouter un objet', icon: 'add', onClick: () => navigate('/add') }}
        />
      ) : (
        <>
          <div className="grid grid-cols-1 lg:grid-cols-[1.4fr_1fr] gap-4">
            <ValueCard items={collection} prices={prices} lastQuote={lastQuote} />
            <div className="grid grid-cols-2 gap-3 content-start">
              <Stat label="Jeux" value={count('game')} onClick={() => navigate('/collection?type=game')} />
              <Stat label="Consoles" value={count('console')} onClick={() => navigate('/collection?type=console')} />
              <Stat label="Accessoires" value={count('accessory')} onClick={() => navigate('/collection?type=accessory')} />
              <Stat label="Envies" value={wishlistCount} onClick={() => navigate('/wishlist')} />
            </div>
          </div>

          {topPieces.length > 0 && (
            <Section title="Pièces maîtresses" action={<Link to="/collection?platform=*" className="tv-link">Tout voir</Link>}>
              <Shelf>
                {topPieces.map(({ item, value }) => (
                  <ItemCard key={item.id} item={item} value={formatEuro(value)} />
                ))}
              </Shelf>
            </Section>
          )}

          <div className={`grid grid-cols-1 gap-8 ${movers.length > 0 ? 'xl:grid-cols-[1.6fr_1fr]' : ''}`}>
            <Section title="Derniers ajouts" action={<Link to="/collection" className="tv-link">Collection</Link>}>
              <Shelf>
                {latest.map((item) => (
                  <ItemCard key={item.id} item={item} value={formatEuroOrDash(bestPrice(item, prices))} />
                ))}
              </Shelf>
            </Section>
            {movers.length > 0 && <MarketRisers movers={movers} catalogue={catalogue} />}
          </div>
        </>
      )}
    </div>
  )
}

function ValueCard({ items, prices, lastQuote }: { items: Item[]; prices: PriceMap; lastQuote: string | null }) {
  const priced = items.filter((i) => bestPrice(i, prices) !== undefined).length
  const lit = Math.round((priced / Math.max(items.length, 1)) * 10)
  return (
    <section className="tv-panel flex flex-col gap-4 p-5" aria-label="Valeur du coffre">
      <div className="flex items-center justify-between gap-3">
        <span className="text-[13px] font-medium text-ink-muted">Valeur du coffre</span>
        <SourceBadge updated={lastQuote ? formatRelative(lastQuote) : undefined}>Cotes eBay</SourceBadge>
      </div>
      <span className="tv-display-xl">{formatEuro(totalValue(items, prices))}</span>
      <div className="tv-well px-4 py-3 flex flex-col gap-2">
        <div className="flex items-center justify-between text-[13px]">
          <span className="text-ink-muted">Objets cotés</span>
          <span className="tv-price">
            {priced} / {items.length}
          </span>
        </div>
        <div className="tv-meter" aria-hidden="true">
          {Array.from({ length: 10 }).map((_, i) => (
            // Off segments use the shell colour: the default (well) would vanish on this well.
            <span key={i} className={`tv-meter-seg ${i < lit ? 'is-on' : 'bg-shell!'}`} />
          ))}
        </div>
      </div>
      <p className="text-[12px] text-ink-faint">Médiane des annonces eBay de l'état de chaque objet. Les objets sans cote ne comptent pas.</p>
    </section>
  )
}

function MarketRisers({ movers, catalogue }: { movers: TopMover[]; catalogue: Item[] }) {
  const byId = new Map(catalogue.map((i) => [i.id, i]))
  return (
    <Section title="Plus fortes hausses" description="Catalogue · 30 derniers jours">
      <div className="tv-panel p-2 flex flex-col">
        {movers.map((m) => {
          const item = byId.get(m.item_id)
          if (!item) return null
          const condition = parseCondition(m.condition)
          return (
            <ListRow
              key={`${m.item_id}:${m.condition}`}
              to={itemPath(item)}
              cover={{ title: item.title, platform: item.platform, src: item.cover_url || undefined }}
              title={item.title}
              meta={`${shortPlatform(item.platform)}${condition ? ` · ${conditionLabel(condition)}` : ''}`}
              trailing={
                <span className="flex flex-col items-end gap-0.5">
                  <span>{formatEuro(m.price_new)}</span>
                  <PriceDelta value={m.delta_pct} className="text-[12px]" />
                </span>
              }
            />
          )
        })}
      </div>
    </Section>
  )
}

function Shelf({ children }: { children: ReactNode }) {
  return <div className="grid grid-cols-3 sm:grid-cols-6 gap-3">{children}</div>
}

function ItemCard({ item, value }: { item: Item; value: string }) {
  return (
    <CollectionCard
      to={itemPath(item)}
      title={item.title}
      platform={item.platform}
      src={item.cover_url || undefined}
      condition={parseCondition(item.condition)}
      value={value}
    />
  )
}

function DashboardSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <Skeleton variant="text" lines={2} className="max-w-xs" />
      <div className="grid grid-cols-1 lg:grid-cols-[1.4fr_1fr] gap-4">
        <Skeleton className="h-56 rounded-lg" />
        <Skeleton className="h-56 rounded-lg" />
      </div>
    </div>
  )
}
