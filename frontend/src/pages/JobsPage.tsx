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

function ErrorModal({ error, onClose }: { error: string; onClose: () => void }) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(6,14,32,0.8)' }}
      onClick={onClose}
    >
      <div
        className="w-full max-w-lg rounded-xl border border-vault-error/40 bg-vault-surface-low p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-4">
          <p className="text-xs font-semibold uppercase tracking-widest text-vault-error">
            Détail de l'erreur
          </p>
          <button
            onClick={onClose}
            className="text-vault-on-surface-variant hover:text-vault-on-surface transition-colors text-sm"
          >
            ✕
          </button>
        </div>
        <pre className="text-xs text-vault-error/90 whitespace-pre-wrap break-all font-mono leading-relaxed bg-vault-surface rounded-lg p-4 border border-vault-error/20 max-h-72 overflow-y-auto">
          {error}
        </pre>
      </div>
    </div>
  )
}

export default function JobsPage() {
  const [jobs, setJobs] = useState<ScrapeJob[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedError, setSelectedError] = useState<string | null>(null)
  const [syncing, setSyncing] = useState(false)
  const [scraping, setScraping] = useState(false)
  const [msg, setMsg] = useState<string | null>(null)

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

  async function triggerSyncIGDB() {
    setSyncing(true)
    setMsg(null)
    try {
      const res = await api.items.syncIGDB()
      setMsg(res.message)
      loadJobs()
    } catch (e) {
      setMsg(`Erreur: ${String(e)}`)
    } finally {
      setSyncing(false)
    }
  }

  async function triggerScrape() {
    setScraping(true)
    setMsg(null)
    try {
      const job = await api.scrape.trigger('user')
      setMsg(`Job démarré (ID: ${job.id})`)
      loadJobs()
    } catch (e) {
      setMsg(`Erreur: ${String(e)}`)
    } finally {
      setScraping(false)
    }
  }

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
      {selectedError && (
        <ErrorModal error={selectedError} onClose={() => setSelectedError(null)} />
      )}

      <div className="flex items-start justify-between mb-8">
        <div>
          <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-2">
            Backoffice
          </p>
          <h1 className="text-2xl font-semibold text-vault-on-surface">Scrape Jobs</h1>
        </div>
        <div className="flex gap-2">
          <button
            onClick={triggerSyncIGDB}
            disabled={syncing}
            className="px-4 py-2 rounded-lg text-sm font-semibold text-vault-on-surface border border-vault-outline-variant disabled:opacity-50 disabled:cursor-not-allowed transition-opacity hover:border-vault-primary"
          >
            {syncing ? 'Sync IGDB…' : 'Sync IGDB'}
          </button>
          <button
            onClick={triggerScrape}
            disabled={scraping}
            className="px-4 py-2 rounded-lg text-sm font-semibold text-white disabled:opacity-50 disabled:cursor-not-allowed transition-opacity"
            style={{ background: 'linear-gradient(135deg, #8455EF, #EC63FF)' }}
          >
            {scraping ? 'Scraping…' : 'Trigger Scrape'}
          </button>
        </div>
      </div>

      {msg && (
        <div className="mb-4 px-4 py-3 rounded-lg bg-vault-surface border border-vault-outline-variant">
          <p className="text-sm text-vault-primary">{msg}</p>
        </div>
      )}

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
                  <td className="px-4 py-3 max-w-xs">
                    {job.error ? (
                      <button
                        onClick={() => setSelectedError(job.error!)}
                        className="text-xs text-vault-error truncate max-w-[200px] block text-left hover:underline cursor-pointer"
                        title="Cliquer pour voir le détail"
                      >
                        {job.error}
                      </button>
                    ) : null}
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
