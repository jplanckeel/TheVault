import { useMemo, useState } from 'react'
import type { AdminItem } from '../../types'
import { Button, Chip, Input, useToast } from '../ui'
import { api } from '../../api'
import ItemThumb from './ItemThumb'

interface Props {
  item: AdminItem
  all: AdminItem[]
  /** Called after a merge with the item that was kept. */
  onMerged: (keptId: string) => void
}

const SEARCH_LIMIT = 6

/**
 * Duplicate handling. "Absorber" keeps the current item and folds the other one into it;
 * "Fusionner dans…" folds the current item into a chosen target. Collections, prices and
 * history follow, and later syncs of the removed item resolve to the kept one.
 */
export default function MergeSection({ item, all, onMerged }: Props) {
  const toast = useToast()
  const [query, setQuery] = useState('')
  const [busy, setBusy] = useState<string | null>(null)
  const byId = useMemo(() => new Map(all.map((i) => [i.id, i])), [all])
  const duplicates = item.duplicate_ids.map((id) => byId.get(id)).filter((i): i is AdminItem => Boolean(i))

  const matches = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (q.length < 2) return []
    return all.filter((i) => i.id !== item.id && i.type === item.type && i.title.toLowerCase().includes(q)).slice(0, SEARCH_LIMIT)
  }, [all, item, query])

  async function merge(sourceId: string, targetId: string) {
    setBusy(sourceId + targetId)
    try {
      await api.admin.merge(sourceId, targetId)
      toast.push('Fusion effectuée', 'success')
      onMerged(targetId)
    } catch (e) {
      toast.push(`Erreur : ${String(e)}`, 'error')
    } finally {
      setBusy(null)
    }
  }

  const row = (other: AdminItem, action: { label: string; source: string; target: string }) => (
    <li key={other.id} className="flex items-center gap-3 py-2">
      <ItemThumb item={other} className="h-10 w-8" />
      <div className="min-w-0 flex-1">
        <p className="text-sm font-semibold text-vault-on-surface truncate">{other.title}</p>
        <p className="text-xs text-vault-on-surface-variant truncate">
          {other.platform || '—'} · {other.owners} propriétaire(s) · {other.external_id}
        </p>
      </div>
      <Button size="sm" variant="subtle" disabled={busy !== null} onClick={() => merge(action.source, action.target)}>
        {action.label}
      </Button>
    </li>
  )

  return (
    <div className="space-y-4">
      {duplicates.length > 0 && (
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Chip tone="error" size="sm" icon="content_copy">
              {duplicates.length} doublon(s) probable(s)
            </Chip>
          </div>
          <ul className="divide-y divide-vault-outline-variant/15">
            {duplicates.map((d) => row(d, { label: 'Absorber', source: d.id, target: item.id }))}
          </ul>
        </div>
      )}
      <div>
        <Input leftIcon="search" placeholder="Fusionner dans un autre objet : rechercher un titre…" value={query} onChange={(e) => setQuery(e.target.value)} />
        {matches.length > 0 && (
          <ul className="mt-2 divide-y divide-vault-outline-variant/15">
            {matches.map((m) => row(m, { label: 'Fusionner dedans', source: item.id, target: m.id }))}
          </ul>
        )}
        {query.trim().length >= 2 && matches.length === 0 && (
          <p className="mt-2 text-xs text-vault-on-surface-variant">Aucun objet du même type ne correspond.</p>
        )}
      </div>
    </div>
  )
}
