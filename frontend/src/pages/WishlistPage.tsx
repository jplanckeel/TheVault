import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Item, PriceSnapshot } from '../types'
import { EmptyState, GameCard, Skeleton } from '../components/ui'

function bestPrice(itemId: string, condition: string, priceMap: Map<string, number>): number {
  return (
    priceMap.get(`${itemId}:${condition}`) ??
    priceMap.get(`${itemId}:cib`) ??
    priceMap.get(`${itemId}:loose`) ??
    priceMap.get(`${itemId}:new`) ??
    0
  )
}

export default function WishlistPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<Item[]>([])
  const [snapshots, setSnapshots] = useState<PriceSnapshot[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([api.wishlist.list(), api.prices.latest()])
      .then(([w, s]) => {
        setItems(w)
        setSnapshots(s)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const priceMap = useMemo(() => {
    const m = new Map<string, number>()
    for (const s of snapshots) m.set(`${s.item_id}:${s.condition}`, s.price_median)
    return m
  }, [snapshots])

  const totalToAcquire = useMemo(
    () => items.reduce((acc, i) => acc + bestPrice(i.id, i.condition, priceMap), 0),
    [items, priceMap],
  )

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton variant="text" lines={2} className="max-w-md" />
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="aspect-[3/4]" />
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6 animate-fade-in-up">
      <div>
        <h1 className="text-display-lg text-vault-on-surface">Wishlist</h1>
        <p className="mt-2 text-vault-on-surface-variant">
          <span className="font-bold text-vault-on-surface tabular-nums">{items.length}</span> items à acquérir
          {totalToAcquire > 0 && (
            <>
              <span className="mx-2 opacity-40">•</span>
              <span className="font-bold text-vault-primary tabular-nums">
                {totalToAcquire.toLocaleString('en-US', {
                  style: 'currency',
                  currency: 'EUR',
                  maximumFractionDigits: 0,
                })}
              </span>{' '}
              estimés
            </>
          )}
        </p>
      </div>

      {items.length === 0 ? (
        <EmptyState
          icon="favorite"
          title="Aucun coup de cœur"
          description="Marque les jeux que tu veux ajouter à ta vault depuis le catalogue ou la page d'un item."
          cta={{ label: 'Parcourir le catalogue', icon: 'storefront', onClick: () => navigate('/catalogue') }}
        />
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {items.map((item) => (
            <GameCard
              key={item.id}
              item={item}
              price={bestPrice(item.id, item.condition, priceMap) || undefined}
              wishlist
              onClick={() => navigate(`/items/${item.id}`)}
            />
          ))}
        </div>
      )}
    </div>
  )
}
