import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Item, PriceSnapshot, TopMover } from '../types'
import { Card, Chip, EmptyState, GameCard, Section, Skeleton, Stat } from '../components/ui'
import BrandPlaceholder from '../components/BrandPlaceholder'
import { itemPath } from '../lib/routes'
import { bestPrice, buildPriceMap, totalValue, type PriceMap } from '../lib/prices'
import { formatEuro } from '../lib/format'

const LATEST_COUNT = 6
const PLATFORM_COUNT = 5

interface PlatformShare {
  platform: string
  count: number
  fraction: number
}

/** Top platforms by number of games — same rule as the mobile dashboard. */
function platformShares(items: Item[]): PlatformShare[] {
  const games = items.filter((i) => i.type === 'game')
  if (games.length === 0) return []
  const counts = new Map<string, number>()
  for (const g of games) if (g.platform) counts.set(g.platform, (counts.get(g.platform) ?? 0) + 1)
  return [...counts.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, PLATFORM_COUNT)
    .map(([platform, count]) => ({ platform, count, fraction: count / games.length }))
}

/** Collection overview — real data only (see design/DESIGN_SYSTEM.md §1). */
export default function DashboardPage() {
  const navigate = useNavigate()
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
    // Market movers are optional: failures simply hide the card.
    Promise.all([api.prices.topMovers(30, 5), api.items.list()])
      .then(([m, items]) => {
        setMovers(m)
        setCatalogue(items)
      })
      .catch(() => {})
  }, [])

  const prices = useMemo(() => buildPriceMap(snapshots), [snapshots])
  const latest = useMemo(
    () => [...collection].sort((a, b) => b.created_at.localeCompare(a.created_at)).slice(0, LATEST_COUNT),
    [collection],
  )
  const platforms = useMemo(() => platformShares(collection), [collection])

  if (loading) return <DashboardSkeleton />

  const count = (type: Item['type']) => collection.filter((i) => i.type === type).length
  return (
    <div className="space-y-6 animate-fade-in-up">
      <header>
        <h1 className="text-display-lg text-vault-on-surface">Dashboard</h1>
        <p className="mt-1 text-vault-on-surface-variant">Votre vault en un coup d'œil</p>
      </header>

      {collection.length === 0 ? (
        <EmptyState
          icon="inventory_2"
          title="Votre vault est vide"
          description="Ajoutez votre premier objet pour voir apparaître la valeur de votre collection."
          cta={{ label: 'Ajouter un objet', icon: 'add', onClick: () => navigate('/add') }}
        />
      ) : (
        <>
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
            <ValueHero items={collection} prices={prices} />
            {platforms.length > 0 && <PlatformBreakdown platforms={platforms} />}
          </div>

          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <Stat label="Jeux" value={count('game')} icon="stadia_controller" onClick={() => navigate('/collection?type=game')} />
            <Stat label="Consoles" value={count('console')} icon="sports_esports" onClick={() => navigate('/collection?type=console')} />
            <Stat label="Accessoires" value={count('accessory')} icon="cable" onClick={() => navigate('/collection?type=accessory')} />
            <Stat label="Wishlist" value={wishlistCount} icon="favorite" onClick={() => navigate('/wishlist')} />
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
            <Card padding="md" radius="2xl" className={movers.length > 0 ? 'lg:col-span-2' : 'lg:col-span-3'}>
              <Section
                title="Derniers ajouts"
                action={<TextAction label="Voir tout" onClick={() => navigate('/collection?type=game')} />}
              >
                <div className={`grid grid-cols-2 sm:grid-cols-3 gap-3 ${movers.length > 0 ? '' : 'lg:grid-cols-6'}`}>
                  {latest.map((item) => (
                    <GameCard key={item.id} item={item} price={bestPrice(item, prices)} onClick={() => navigate(itemPath(item))} />
                  ))}
                </div>
              </Section>
            </Card>
            {movers.length > 0 && <MarketMovers movers={movers} catalogue={catalogue} />}
          </div>
        </>
      )}
    </div>
  )
}

function ValueHero({ items, prices }: { items: Item[]; prices: PriceMap }) {
  const priced = items.filter((i) => bestPrice(i, prices) !== undefined).length
  return (
    <Card padding="lg" radius="2xl" className="lg:col-span-2 flex flex-col justify-between">
      <div>
        <p className="text-label-bold text-vault-on-surface-variant">Valeur estimée</p>
        <p className="mt-2 text-display-lg text-vault-on-surface tabular-nums">{formatEuro(totalValue(items, prices))}</p>
      </div>
      <div className="mt-6 flex flex-wrap items-center gap-3">
        <Chip tone={priced === items.length ? 'success' : 'neutral'} icon="sell">
          {priced} / {items.length} objets valorisés
        </Chip>
        <span className="text-xs text-vault-on-surface-variant">Médiane des ventes eBay les plus récentes</span>
      </div>
    </Card>
  )
}

function PlatformBreakdown({ platforms }: { platforms: PlatformShare[] }) {
  return (
    <Card padding="md" radius="2xl">
      <Section title="Par plateforme">
        <ul className="space-y-3.5">
          {platforms.map((p) => (
            <li key={p.platform}>
              <div className="flex justify-between text-sm">
                <span className="font-semibold text-vault-on-surface truncate">{p.platform}</span>
                <span className="text-vault-on-surface-variant tabular-nums">{p.count} jeux</span>
              </div>
              <div className="mt-1.5 h-1.5 rounded-full bg-vault-surface-high overflow-hidden">
                <div className="h-full rounded-full bg-vault-primary" style={{ width: `${Math.round(p.fraction * 100)}%` }} />
              </div>
            </li>
          ))}
        </ul>
      </Section>
    </Card>
  )
}

function MarketMovers({ movers, catalogue }: { movers: TopMover[]; catalogue: Item[] }) {
  const navigate = useNavigate()
  const byId = new Map(catalogue.map((i) => [i.id, i]))
  return (
    <Card padding="md" radius="2xl">
      <Section title="Hausses du marché" description="Catalogue · 30 derniers jours">
        <ul>
          {movers.map((m) => {
            const item = byId.get(m.item_id)
            if (!item) return null
            return (
              <li key={`${m.item_id}:${m.condition}`}>
                <button
                  onClick={() => navigate(itemPath(item))}
                  className="w-full flex items-center gap-3 py-2 rounded-lg text-left hover:bg-vault-surface-high/50 focus-ring"
                >
                  <div className="h-10 w-10 shrink-0 rounded-md overflow-hidden">
                    {item.cover_url ? (
                      <img src={item.cover_url} alt="" className="h-full w-full object-cover" />
                    ) : (
                      <BrandPlaceholder platform={item.platform} />
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold text-vault-on-surface truncate">{item.title}</p>
                    <p className="text-[10px] font-bold tracking-widest uppercase text-vault-on-surface-variant truncate">
                      {item.platform} · {m.condition}
                    </p>
                  </div>
                  <div className="text-right shrink-0">
                    <p className="text-sm font-bold tabular-nums text-vault-success">+{formatEuro(m.delta_abs)}</p>
                    <p className="text-xs tabular-nums text-vault-on-surface-variant">+{m.delta_pct.toFixed(1)} %</p>
                  </div>
                </button>
              </li>
            )
          })}
        </ul>
      </Section>
    </Card>
  )
}

function TextAction({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button onClick={onClick} className="text-sm font-semibold text-vault-primary hover:text-vault-primary-dim focus-ring rounded">
      {label}
    </button>
  )
}

function DashboardSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton variant="text" lines={2} className="max-w-md" />
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <Skeleton variant="card" className="lg:col-span-2 h-48" />
        <Skeleton variant="card" className="h-48" />
      </div>
    </div>
  )
}
