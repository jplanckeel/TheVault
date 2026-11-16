import { useEffect, useMemo, useState } from 'react'
import { api } from '../api'
import type { Item, PriceSnapshot } from '../types'
import ItemGridView from '../components/ItemGridView'
import { buildPriceMap } from '../lib/prices'

/** Explorer: the shared catalogue of every item synced by the community. */
export default function CataloguePage() {
  const [items, setItems] = useState<Item[]>([])
  const [snapshots, setSnapshots] = useState<PriceSnapshot[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([api.items.list(), api.prices.latest()])
      .then(([i, s]) => {
        setItems(i)
        setSnapshots(s)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const prices = useMemo(() => buildPriceMap(snapshots), [snapshots])

  return (
    <ItemGridView
      eyebrow="Catalogue partagé"
      title="Explorer"
      items={items}
      prices={prices}
      loading={loading}
      empty={{
        title: 'Catalogue vide',
        description: "Les objets apparaissent ici dès qu'ils sont synchronisés depuis l'application mobile.",
      }}
    />
  )
}
