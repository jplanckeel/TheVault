import { useEffect, useState } from 'react'
import { api } from '../api'
import type { ScrapeJob } from '../types'

const STATUS_COLOR: Record<string, string> = {
  pending: '#888',
  running: '#0070f3',
  done: '#0a0',
  failed: '#c00',
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

  if (loading) return <p>Loading…</p>
  if (error) return <p style={{ color: 'red' }}>Error: {error}</p>

  return (
    <div>
      <h1>Scrape Jobs</h1>
      {jobs.length === 0 ? (
        <p>No jobs yet.</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr>
              {['Started At', 'Triggered By', 'Status', 'Progress', 'Error'].map((h) => (
                <th key={h} style={{ textAlign: 'left', padding: '0.5rem', borderBottom: '1px solid #e0e0e0' }}>
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {jobs.map((job) => (
              <tr key={job.id}>
                <td style={{ padding: '0.5rem', borderBottom: '1px solid #f0f0f0' }}>
                  {new Date(job.started_at).toLocaleString()}
                </td>
                <td style={{ padding: '0.5rem', borderBottom: '1px solid #f0f0f0' }}>{job.triggered_by}</td>
                <td style={{ padding: '0.5rem', borderBottom: '1px solid #f0f0f0' }}>
                  <span style={{ color: STATUS_COLOR[job.status] ?? '#000', fontWeight: 600 }}>
                    {job.status}
                  </span>
                </td>
                <td style={{ padding: '0.5rem', borderBottom: '1px solid #f0f0f0' }}>
                  {job.items_done} / {job.items_total}
                </td>
                <td style={{ padding: '0.5rem', borderBottom: '1px solid #f0f0f0', color: '#c00' }}>
                  {job.error ?? ''}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
