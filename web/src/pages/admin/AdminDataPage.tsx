import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../../api'
import type { AdminItem, Item, ItemIssue } from '../../types'
import { Button, Chip, EmptyState, FilterChip, Input, MaterialIcon, Skeleton } from '../../components/ui'
import ItemThumb from '../../components/admin/ItemThumb'
import ItemDrawer from '../../components/admin/ItemDrawer'
import { countIssues, filterAdminItems, ISSUE_ORDER, ISSUES, type AdminFilters } from '../../lib/adminItems'
import { formatRelative } from '../../lib/format'

const PAGE_SIZE = 100
const TYPES: { value: AdminFilters['type']; label: string }[] = [
  { value: 'all', label: 'Tous types' },
  { value: 'game', label: 'Jeux' },
  { value: 'console', label: 'Consoles' },
  { value: 'accessory', label: 'Accessoires' },
]
const TYPE_LABEL: Record<Item['type'], string> = { game: 'Jeu', console: 'Console', accessory: 'Accessoire' }

function IssueChips({ issues }: { issues: ItemIssue[] }) {
  if (issues.length === 0) return <Chip tone="success" icon="check">OK</Chip>
  return (
    <div className="flex flex-wrap gap-1">
      {ISSUE_ORDER.filter((i) => issues.includes(i)).map((i) => (
        <Chip key={i} tone={ISSUES[i].tone}>{ISSUES[i].label}</Chip>
      ))}
    </div>
  )
}

/** Back-office — browse the shared catalogue and fix data quality issues. */
export default function AdminDataPage() {
  const [items, setItems] = useState<AdminItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filters, setFilters] = useState<AdminFilters>({ type: 'all', issue: 'any', query: '' })
  const [visible, setVisible] = useState(PAGE_SIZE)
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const load = useCallback(() => {
    return api.admin
      .items()
      .then((data) => {
        setItems(data)
        setError(null)
      })
      .catch((e) => setError(String(e)))
  }, [])

  useEffect(() => {
    load().finally(() => setLoading(false))
  }, [load])

  const ofType = useMemo(() => filterAdminItems(items, { ...filters, issue: 'all', query: '' }), [items, filters])
  const counts = useMemo(() => countIssues(ofType), [ofType])
  const toFix = ofType.filter((i) => i.issues.length > 0).length
  const rows = useMemo(() => filterAdminItems(items, filters), [items, filters])
  const selected = items.find((i) => i.id === selectedId) ?? null

  function update(patch: Partial<AdminFilters>) {
    setFilters((f) => ({ ...f, ...patch }))
    setVisible(PAGE_SIZE)
  }

  return (
    <div className="space-y-6 tv-enter">
      <header>
        <p className="tv-engrave text-indigo-ink">Back-office</p>
        <h1 className="tv-title text-ink">Données</h1>
        <p className="mt-1 text-ink-muted tabular-nums">
          {items.length.toLocaleString('fr-FR')} objets au catalogue · {toFix.toLocaleString('fr-FR')} à corriger
        </p>
      </header>

      <div className="space-y-3">
        <div className="flex gap-2 overflow-x-auto pb-1">
          <FilterChip label="À corriger" count={toFix} selected={filters.issue === 'any'} onClick={() => update({ issue: 'any' })} />
          {ISSUE_ORDER.map((i) => (
            <FilterChip key={i} label={ISSUES[i].label} count={counts[i]} selected={filters.issue === i} onClick={() => update({ issue: i })} />
          ))}
          <FilterChip label="Tout" count={ofType.length} selected={filters.issue === 'all'} onClick={() => update({ issue: 'all' })} />
        </div>
        <div className="flex flex-col lg:flex-row gap-3 lg:items-center">
          <div className="w-full lg:max-w-md">
            <Input leftIcon="search" placeholder="Titre, plateforme ou identifiant…" value={filters.query} onChange={(e) => update({ query: e.target.value })} />
          </div>
          <div className="flex gap-2 overflow-x-auto shrink-0">
            {TYPES.map((t) => (
              <FilterChip key={t.value} label={t.label} selected={filters.type === t.value} onClick={() => update({ type: t.value })} />
            ))}
          </div>
        </div>
      </div>

      {loading ? (
        <Skeleton variant="card" className="h-96" />
      ) : error ? (
        <EmptyState icon="cloud_off" title="Impossible de charger le catalogue" description={error} />
      ) : rows.length === 0 ? (
        <EmptyState
          icon={filters.issue === 'any' ? 'task_alt' : 'filter_alt_off'}
          title={filters.issue === 'any' ? 'Rien à corriger' : 'Aucun résultat'}
          description={filters.issue === 'any' ? 'Toutes les données de ce filtre sont propres.' : 'Essayez un autre filtre.'}
        />
      ) : (
        <>
          <div className="hidden md:block rounded-lg bg-panel border border-line/30 overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-line/30">
                  {['Objet', 'Type', 'Plateforme', 'Problèmes', 'Propriétaires', 'Mis à jour'].map((h) => (
                    <th key={h} className="text-left px-4 py-3 tv-engrave text-ink-muted whitespace-nowrap">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.slice(0, visible).map((item) => (
                  <tr
                    key={item.id}
                    onClick={() => setSelectedId(item.id)}
                    onKeyDown={(e) => e.key === 'Enter' && setSelectedId(item.id)}
                    tabIndex={0}
                    className="cursor-pointer border-b border-line/15 last:border-0 hover:bg-well/40 focus:outline-none focus:bg-well/60"
                  >
                    <td className="px-4 py-2.5">
                      <div className="flex items-center gap-3 min-w-0">
                        <ItemThumb item={item} />
                        <div className="min-w-0">
                          <p className="font-semibold text-ink truncate max-w-[320px]">
                            {item.title}
                            {item.admin_edited && <MaterialIcon name="lock" size={14} className="ml-1 text-indigo-ink" />}
                          </p>
                          <p className="text-xs text-ink-muted font-mono truncate max-w-[320px]">{item.external_id}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-2.5 text-ink-muted">{TYPE_LABEL[item.type]}</td>
                    <td className="px-4 py-2.5 text-ink">{item.platform || <span className="text-ink">—</span>}</td>
                    <td className="px-4 py-2.5"><IssueChips issues={item.issues} /></td>
                    <td className="px-4 py-2.5 text-ink-muted tabular-nums">{item.owners}</td>
                    <td className="px-4 py-2.5 text-ink-muted whitespace-nowrap">{formatRelative(item.updated_at)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="md:hidden space-y-2">
            {rows.slice(0, visible).map((item) => (
              <li key={item.id}>
                <button
                  onClick={() => setSelectedId(item.id)}
                  className="w-full flex items-center gap-3 rounded-lg bg-panel border border-line/30 p-3 text-left focus-ring"
                >
                  <ItemThumb item={item} />
                  <div className="min-w-0 flex-1 space-y-1">
                    <p className="text-sm font-semibold text-ink truncate">{item.title}</p>
                    <p className="text-xs text-ink-muted truncate">{TYPE_LABEL[item.type]} · {item.platform || '—'}</p>
                    <IssueChips issues={item.issues} />
                  </div>
                </button>
              </li>
            ))}
          </ul>

          {rows.length > visible && (
            <div className="flex justify-center">
              <Button variant="ghost" onClick={() => setVisible((v) => v + PAGE_SIZE)}>
                Afficher plus ({rows.length - visible} restants)
              </Button>
            </div>
          )}
        </>
      )}

      {selected && (
        <ItemDrawer
          item={selected}
          all={items}
          onClose={() => setSelectedId(null)}
          onChanged={(id) => {
            setSelectedId(id)
            load()
          }}
        />
      )}
    </div>
  )
}
