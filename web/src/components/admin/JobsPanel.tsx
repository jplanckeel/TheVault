import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { api } from '../../api'
import type { JobStatus, ScrapeJob } from '../../types'
import { Button, Card, Chip, EmptyState, Skeleton, useToast } from '../ui'
import { formatRelative } from '../../lib/format'

const REFRESH_MS = 5000

type Tone = 'neutral' | 'primary' | 'success' | 'error' | 'info'

/** Sync statuses — design/DESIGN_SYSTEM.md §6. */
const STATUS: Record<JobStatus, { label: string; tone: Tone; icon: string }> = {
  pending: { label: 'En attente', tone: 'neutral', icon: 'schedule' },
  running: { label: 'En cours', tone: 'primary', icon: 'progress_activity' },
  done: { label: 'Terminé', tone: 'success', icon: 'check_circle' },
  failed: { label: 'Échoué', tone: 'error', icon: 'error' },
}

const JOB_TYPES: Record<string, { label: string; tone: Tone }> = {
  ebay: { label: 'Prix eBay', tone: 'info' },
  igdb: { label: 'Métadonnées IGDB', tone: 'primary' },
}

function ProgressBar({ done, total }: { done: number; total: number }) {
  const pct = total > 0 ? Math.round((done / total) * 100) : 0
  return (
    <div className="flex items-center gap-3 min-w-[140px]">
      <div className="flex-1 h-1.5 rounded-full bg-well overflow-hidden">
        <div className="h-full rounded-full bg-indigo transition-all" style={{ width: `${pct}%` }} />
      </div>
      <span className="text-xs text-ink-muted tabular-nums whitespace-nowrap">
        {done}/{total}
      </span>
    </div>
  )
}

function ErrorDialog({ error, onClose }: { error: string; onClose: () => void }) {
  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70" onClick={onClose} role="dialog" aria-modal>
      <Card padding="lg" className="w-full max-w-lg" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-4">
          <p className="tv-engrave text-loss">Détail de l'erreur</p>
          <Button variant="secondary" size="sm" onClick={onClose}>
            Fermer
          </Button>
        </div>
        <pre className="text-xs text-loss whitespace-pre-wrap break-all font-mono bg-panel rounded-md p-4 max-h-72 overflow-y-auto">
          {error}
        </pre>
      </Card>
    </div>,
    document.body,
  )
}

/** eBay price scraping and IGDB enrichment runs, with manual triggers. */
export default function JobsPanel() {
  const toast = useToast()
  const [jobs, setJobs] = useState<ScrapeJob[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [selectedError, setSelectedError] = useState<string | null>(null)
  const [busy, setBusy] = useState<'igdb' | 'ebay' | null>(null)

  function loadJobs() {
    return api.scrape
      .list()
      .then((j) => {
        setJobs(j)
        setLoadError(null)
      })
      .catch((e) => setLoadError(String(e)))
  }

  useEffect(() => {
    loadJobs().finally(() => setLoading(false))
    const interval = setInterval(loadJobs, REFRESH_MS)
    return () => clearInterval(interval)
  }, [])

  async function run(kind: 'igdb' | 'ebay') {
    setBusy(kind)
    try {
      if (kind === 'igdb') {
        const res = await api.items.syncIGDB()
        toast.push(res.message, 'success')
      } else {
        await api.scrape.trigger('admin')
        toast.push('Scraping eBay démarré', 'success')
      }
      loadJobs()
    } catch (e) {
      toast.push(`Erreur : ${String(e)}`, 'error')
    } finally {
      setBusy(null)
    }
  }

  return (
    <div className="space-y-4">
      {selectedError && <ErrorDialog error={selectedError} onClose={() => setSelectedError(null)} />}

      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <p className="text-sm text-ink-muted">Actualisation automatique toutes les 5 secondes.</p>
        <div className="flex gap-2 flex-wrap">
          <Button variant="ghost" leftIcon="auto_awesome" disabled={busy !== null} onClick={() => run('igdb')}>
            {busy === 'igdb' ? 'Synchronisation…' : 'Enrichir via IGDB'}
          </Button>
          <Button leftIcon="sell" disabled={busy !== null} onClick={() => run('ebay')}>
            {busy === 'ebay' ? 'Démarrage…' : 'Scraper les prix eBay'}
          </Button>
        </div>
      </div>

      {loading ? (
        <Skeleton variant="card" className="h-64" />
      ) : loadError ? (
        <EmptyState icon="cloud_off" title="Impossible de charger les synchronisations" description={loadError} />
      ) : jobs.length === 0 ? (
        <EmptyState icon="sync" title="Aucune synchronisation" description="Lancez un enrichissement IGDB ou un scraping eBay." />
      ) : (
        <Card padding="none" className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-line/30">
                {['Démarré', 'Type', 'Déclenché par', 'Statut', 'Progression', 'Erreur'].map((h) => (
                  <th key={h} className="text-left px-4 py-3 tv-engrave text-ink-muted">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {jobs.map((job) => {
                const status = STATUS[job.status] ?? STATUS.pending
                const type = JOB_TYPES[job.job_type] ?? { label: job.job_type, tone: 'neutral' as Tone }
                return (
                  <tr key={job.id} className="border-b border-line/15 last:border-0 hover:bg-well/40">
                    <td className="px-4 py-3 text-ink-muted whitespace-nowrap" title={new Date(job.started_at).toLocaleString('fr-FR')}>
                      {formatRelative(job.started_at)}
                    </td>
                    <td className="px-4 py-3">
                      <Chip tone={type.tone}>{type.label}</Chip>
                    </td>
                    <td className="px-4 py-3 text-ink">{job.triggered_by}</td>
                    <td className="px-4 py-3">
                      <Chip tone={status.tone} icon={status.icon} className={job.status === 'running' ? 'animate-pulse' : ''}>
                        {status.label}
                      </Chip>
                    </td>
                    <td className="px-4 py-3">
                      <ProgressBar done={job.items_done} total={job.items_total} />
                    </td>
                    <td className="px-4 py-3 max-w-xs">
                      {job.error && (
                        <button
                          onClick={() => setSelectedError(job.error ?? '')}
                          className="text-xs text-loss truncate max-w-[220px] block text-left hover:underline focus-ring rounded"
                          title="Voir le détail"
                        >
                          {job.error}
                        </button>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </Card>
      )}
    </div>
  )
}
