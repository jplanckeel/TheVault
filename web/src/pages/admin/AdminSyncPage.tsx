import { useCallback, useEffect, useState } from 'react'
import { api } from '../../api'
import type { SyncAction, SyncEvent, SyncSource, SyncSummary } from '../../types'
import { Card, Chip, EmptyState, FilterChip, Skeleton, Stat } from '../../components/ui'
import JobsPanel from '../../components/admin/JobsPanel'
import { ACTIONS, SOURCES } from '../../lib/adminItems'
import { formatRelative } from '../../lib/format'

const REFRESH_MS = 10_000
type Tab = 'events' | 'jobs'

function count(summary: SyncSummary | null, source: SyncSource, actions: SyncAction[]): number {
  return actions.reduce((acc, a) => acc + (summary?.counts[source]?.[a] ?? 0), 0)
}

function SummaryRow({ summary }: { summary: SyncSummary | null }) {
  const mobile = count(summary, 'mobile', ['created', 'updated'])
  const created = count(summary, 'mobile', ['created']) + count(summary, 'web', ['created'])
  const rejected = count(summary, 'mobile', ['rejected']) + count(summary, 'web', ['rejected'])
  const admin = count(summary, 'admin', ['edited', 'merged', 'deleted'])
  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <Stat label="Synchros mobile · 24 h" value={mobile} />
      <Stat label="Nouveaux objets · 24 h" value={created} />
      <Stat label="Refusées · 24 h" value={rejected} hint="Objets supprimés par un admin" />
      <Stat label="Corrections · 24 h" value={admin} />
    </div>
  )
}

function EventsTable({ events }: { events: SyncEvent[] }) {
  return (
    <Card padding="none" className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-line/30">
            {['Quand', 'Source', 'Action', 'Objet', 'Par', 'Détail'].map((h) => (
              <th key={h} className="text-left px-4 py-3 tv-engrave text-ink-muted whitespace-nowrap">{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {events.map((e) => (
            <tr key={e.id} className="border-b border-line/15 last:border-0 hover:bg-well/40">
              <td className="px-4 py-2.5 text-ink-muted whitespace-nowrap" title={new Date(e.created_at).toLocaleString('fr-FR')}>
                {formatRelative(e.created_at)}
              </td>
              <td className="px-4 py-2.5">
                <Chip tone={SOURCES[e.source].tone} icon={SOURCES[e.source].icon}>{SOURCES[e.source].label}</Chip>
              </td>
              <td className="px-4 py-2.5">
                <Chip tone={ACTIONS[e.action].tone}>{ACTIONS[e.action].label}</Chip>
              </td>
              <td className="px-4 py-2.5">
                <p className="text-ink truncate max-w-[260px]">{e.title || '—'}</p>
                <p className="text-xs text-ink-muted font-mono truncate max-w-[260px]">{e.external_id}</p>
              </td>
              <td className="px-4 py-2.5 text-ink-muted truncate max-w-[180px]">{e.actor}</td>
              <td className="px-4 py-2.5 text-ink-muted truncate max-w-[240px]">{e.detail}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  )
}

/** Back-office — what the apps sent, what admins changed, and the scraping jobs. */
export default function AdminSyncPage() {
  const [tab, setTab] = useState<Tab>('events')
  const [source, setSource] = useState<SyncSource | ''>('')
  const [action, setAction] = useState<SyncAction | ''>('')
  const [events, setEvents] = useState<SyncEvent[] | null>(null)
  const [summary, setSummary] = useState<SyncSummary | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    Promise.all([api.admin.syncEvents({ source, action, limit: 200 }), api.admin.syncSummary(24)])
      .then(([e, s]) => {
        setEvents(e)
        setSummary(s)
        setError(null)
      })
      .catch((e) => setError(String(e)))
  }, [source, action])

  useEffect(() => {
    load()
    const interval = setInterval(load, REFRESH_MS)
    return () => clearInterval(interval)
  }, [load])

  return (
    <div className="space-y-6 tv-enter">
      <header>
        <p className="tv-engrave text-indigo-ink">Back-office</p>
        <h1 className="tv-title text-ink">Synchronisations</h1>
        <p className="mt-1 text-ink-muted">Envois des applications, corrections admin et tâches de scraping.</p>
      </header>

      <SummaryRow summary={summary} />

      <div className="flex gap-2">
        <FilterChip label="Journal des synchros" selected={tab === 'events'} onClick={() => setTab('events')} />
        <FilterChip label="Tâches eBay / IGDB" selected={tab === 'jobs'} onClick={() => setTab('jobs')} />
      </div>

      {tab === 'jobs' ? (
        <JobsPanel />
      ) : (
        <div className="space-y-4">
          <div className="flex flex-wrap gap-2">
            <FilterChip label="Toutes sources" selected={source === ''} onClick={() => setSource('')} />
            {(Object.keys(SOURCES) as SyncSource[]).map((s) => (
              <FilterChip key={s} label={SOURCES[s].label} selected={source === s} onClick={() => setSource(s)} />
            ))}
            <span className="mx-1 w-px bg-line/40" />
            <FilterChip label="Toutes actions" selected={action === ''} onClick={() => setAction('')} />
            {(Object.keys(ACTIONS) as SyncAction[]).map((a) => (
              <FilterChip key={a} label={ACTIONS[a].label} selected={action === a} onClick={() => setAction(a)} />
            ))}
          </div>
          {error ? (
            <EmptyState icon="cloud_off" title="Impossible de charger le journal" description={error} />
          ) : events === null ? (
            <Skeleton variant="card" className="h-64" />
          ) : events.length === 0 ? (
            <EmptyState icon="history" title="Aucun événement" description="Les synchros de l'app mobile et les corrections apparaîtront ici." />
          ) : (
            <EventsTable events={events} />
          )}
        </div>
      )}
    </div>
  )
}
