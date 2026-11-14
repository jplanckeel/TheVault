import { useEffect, useState } from 'react'
import type { SyncEvent } from '../../types'
import { api } from '../../api'
import { Chip } from '../ui'
import { ACTIONS, SOURCES } from '../../lib/adminItems'
import { formatRelative } from '../../lib/format'

/** Latest sync events and admin actions on one item. */
export default function ItemHistory({ itemId, refreshKey }: { itemId: string; refreshKey: number }) {
  const [events, setEvents] = useState<SyncEvent[] | null>(null)

  useEffect(() => {
    api.admin
      .syncEvents({ item_id: itemId, limit: 10 })
      .then(setEvents)
      .catch(() => setEvents([]))
  }, [itemId, refreshKey])

  if (events === null) return <p className="text-sm text-vault-on-surface-variant">Chargement…</p>
  if (events.length === 0) return <p className="text-sm text-vault-on-surface-variant">Aucun événement enregistré pour cet objet.</p>
  return (
    <ul className="space-y-2">
      {events.map((e) => (
        <li key={e.id} className="flex flex-wrap items-center gap-2 text-sm">
          <Chip size="sm" tone={SOURCES[e.source].tone} icon={SOURCES[e.source].icon}>
            {SOURCES[e.source].label}
          </Chip>
          <Chip size="sm" tone={ACTIONS[e.action].tone}>
            {ACTIONS[e.action].label}
          </Chip>
          <span className="text-vault-on-surface-variant truncate">{e.detail || e.actor}</span>
          <span className="ml-auto text-xs text-vault-on-surface-variant" title={new Date(e.created_at).toLocaleString('fr-FR')}>
            {formatRelative(e.created_at)}
          </span>
        </li>
      ))}
    </ul>
  )
}
