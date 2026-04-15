import { useEffect, useState } from 'react'
import { api } from '../api'
import type { ScrapeJob } from '../types'

const STATUS_CONFIG: Record<string, { color: string; bg: string; label: string; pulse?: boolean }> = {
  pending: { color: '#6D758C', bg: 'rgba(109,117,140,0.15)', label: 'En attente' },
  running: { color: '#BA9EFF', bg: 'rgba(186,158,255,0.15)', label: 'En cours', pulse: true },
  done:    { color: '#10B981', bg: 'rgba(16,185,129,0.15)', label: 'Terminé' },
  failed:  { color: '#FF6E84', bg: 'rgba(255,110,132,0.15)', label: 'Échoué' },
}

const JOB_TYPE_CONFIG: Record<string, { label: string; color: string; bg: string }> = {
  ebay: { label: 'eBay', color: '#E53238', bg: 'rgba(229,50,56,0.15)' },
  igdb: { label: 'IGDB', color: '#BA9EFF', bg: 'rgba(186,158,255,0.15)' },
}

function JobTypeBadge({ type }: { type: string }) {
  const cfg = JOB_TYPE_CONFIG[type] ?? { label: type, color: '#A3AAC4', bg: 'rgba(163,170,196,0.15)' }
  return (
    <span
      className="inline-flex items-center justify-center text-xs font-semibold px-2.5 py-1 rounded-full"
      style={{ color: cfg.color, background: cfg.bg }}
    >
      {cfg.label}
    </span>
  )
}

function StatusBadge({ status }: { status: string }) {
  const cfg = STATUS_CONFIG[status] ?? { color: '#A3AAC4', bg: 'rgba(163,170,196,0.15)', label: status }
  return (
    <span
      className={`inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-full ${cfg.pulse ? 'animate-pulse' : ''}`}
      style={{ color: cfg.color, background: cfg.bg }}
    >
      <span className="w-1.5 h-1.5 rounded-full" style={{ background: cfg.color }} />
      {cfg.label}
    </span>
  )
}

function ProgressBar({ done, total }: { done: number; total: number }) {
  const pct = total > 0 ? Math.round((done / total) * 100) : 0
  return (
    <div className="flex items-center gap-3 min-w-[120px]">
      <div className="flex-1 h-1.5 rounded-full bg-vault-outline-variant overflow-hidden">
        <div
          className="h-full rounded-full transition-all"
          style={{ width: `${pct}%`, background: 'linear-gradient(90deg, #8455EF, #EC63FF)' }}
        />
      </div>
      <span className="text-xs text-vault-on-surface-variant whitespace-nowrap">
        {done}/{total}
      </span>
    </div>
  )
}

export default function JobsPage() {
  const [jobs, setJobs] = useState<ScrapeJob[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  function loadJobs() {
    return api.scrape.list()
      .then(setJobs)
      .catch((e) => setError(String(e)))
  }

  useEffect(() => {
    loadJobs().finally(() => setLoading(false))
    const interval = setInterval(loadJobs, 5000)
    return () => clearInterval(interval)
  }, [])

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <p className="text-vault-on-surface-variant animate-pulse">Chargement…</p>
    </div>
  )

  if (error) return (
    <div className="rounded-xl bg-vault-surface border border-vault-error/30 p-4">
      <p className="text-vault-error text-sm">Erreur : {error}</p>
    </div>
  )

  return (
    <div>
      <div className="mb-8">
        <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-2">
          Backoffice
        </p>
        <h1 className="text-2xl font-semibold text-vault-on-surface">Scrape Jobs</h1>
      </div>

      {jobs.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-64 text-center">
          <p className="text-vault-on-surface-variant">Aucun job pour l'instant.</p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-vault-outline-variant">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-vault-outline-variant bg-vault-surface">
                {['Démarré le', 'Type', 'Déclenché par', 'Statut', 'Progression', 'Erreur'].map((h) => (
                  <th key={h} className="text-left px-4 py-3 text-xs font-medium tracking-wider text-vault-on-surface-variant uppercase">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {jobs.map((job) => (
                <tr key={job.id} className="border-b border-vault-outline-variant/50 hover:bg-vault-surface/50 transition-colors">
                  <td className="px-4 py-3 text-vault-on-surface-variant">
                    {new Date(job.started_at).toLocaleString('fr-FR')}
                  </td>
                  <td className="px-4 py-3">
                    <JobTypeBadge type={job.job_type} />
                  </td>
                  <td className="px-4 py-3 text-vault-on-surface">{job.triggered_by}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={job.status} />
                  </td>
                  <td className="px-4 py-3">
                    <ProgressBar done={job.items_done} total={job.items_total} />
                  </td>
                  <td className="px-4 py-3 text-vault-error text-xs max-w-xs truncate">
                    {job.error ?? ''}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
