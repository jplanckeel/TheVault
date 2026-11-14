import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Item, PriceSnapshot } from '../types'
import { Button } from '../components/ui'
import CsvImportModal from '../components/CsvImportModal'
import ItemGridView from '../components/ItemGridView'
import { buildPriceMap } from '../lib/prices'

/** Items owned by the logged-in user. */
export default function CollectionPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<Item[]>([])
  const [snapshots, setSnapshots] = useState<PriceSnapshot[]>([])
  const [loading, setLoading] = useState(true)
  const [csvOpen, setCsvOpen] = useState(false)

  function load() {
    return Promise.all([api.collection.list(), api.prices.latest()])
      .then(([c, s]) => {
        setItems(c)
        setSnapshots(s)
      })
      .catch(() => {})
  }

  useEffect(() => {
    load().finally(() => setLoading(false))
  }, [])

  const prices = useMemo(() => buildPriceMap(snapshots), [snapshots])

  return (
    <>
      <ItemGridView
        eyebrow="Mon coffre"
        title="Collection"
        items={items}
        prices={prices}
        loading={loading}
        actions={
          <>
            <Button variant="secondary" size="sm" leftIcon="upload_file" onClick={() => setCsvOpen(true)}>
              Importer un CSV
            </Button>
            <Button size="sm" leftIcon="add" onClick={() => navigate('/add')}>
              Ajouter
            </Button>
          </>
        }
        empty={{
          title: 'Ton coffre est vide',
          description: 'Importe ton export GameEye ou ajoute ton premier objet depuis IGDB.',
          cta: { label: 'Importer un CSV', icon: 'upload_file', onClick: () => setCsvOpen(true) },
        }}
      />
      <CsvImportModal open={csvOpen} onClose={() => setCsvOpen(false)} onComplete={() => load()} />
    </>
  )
}
