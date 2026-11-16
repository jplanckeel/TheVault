import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Item, PriceSnapshot } from '../types'
import ItemGridView from '../components/ItemGridView'
import { buildPriceMap } from '../lib/prices'

/** Items the user wants to acquire. */
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

  const prices = useMemo(() => buildPriceMap(snapshots), [snapshots])

  return (
    <ItemGridView
      eyebrow="Liste d'envies"
      title="Envies"
      items={items}
      prices={prices}
      loading={loading}
      empty={{
        title: 'Aucune envie pour l’instant',
        description: "Ajoute des objets à tes envies depuis l'Explorer ou la fiche d'un objet.",
        cta: { label: "Ouvrir l'Explorer", icon: 'travel_explore', onClick: () => navigate('/catalogue') },
      }}
    />
  )
}
