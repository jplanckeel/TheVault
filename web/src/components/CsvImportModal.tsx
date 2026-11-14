import { useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { api } from '../api'
import type { Item } from '../types'
import { Button, Card, Chip, MaterialIcon, useToast } from './ui'
import { matchKey, parseGameEyeCsv, type GameEyeRow } from '../lib/gameeyeCsv'

type Phase = 'idle' | 'preview' | 'running' | 'done'

interface ImportReport {
  added: number
  alreadyOwned: number
  notFound: { title: string; platform: string }[]
  failed: { title: string; error: string }[]
  upserted: number
}

interface Props {
  open: boolean
  onClose: () => void
  /** Called when import finished — parent can refresh its data. */
  onComplete?: (report: ImportReport) => void
}

/**
 * Modal that imports a GameEye CSV into the user's collection.
 * - Parses CSV in the browser (no upload).
 * - For each row: matches against /items, adds to /collection.
 * - Admin only: can also upsert missing items into the catalogue first.
 */
export default function CsvImportModal({ open, onClose, onComplete }: Props) {
  const toast = useToast()
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const [phase, setPhase] = useState<Phase>('idle')
  const [rows, setRows] = useState<GameEyeRow[]>([])
  const [fileName, setFileName] = useState<string>('')
  const [createMissing, setCreateMissing] = useState(true)
  const [progress, setProgress] = useState({ done: 0, total: 0, currentTitle: '' })
  const [report, setReport] = useState<ImportReport | null>(null)
  const [dragOver, setDragOver] = useState(false)

  // Reset when the modal closes (adjusting state during render, not in an effect).
  const [wasOpen, setWasOpen] = useState(open)
  if (wasOpen !== open) {
    setWasOpen(open)
    if (!open) {
      setPhase('idle')
      setRows([])
      setFileName('')
      setProgress({ done: 0, total: 0, currentTitle: '' })
      setReport(null)
      setCreateMissing(false)
    }
  }

  const wishlistCount = useMemo(() => rows.filter((r) => r.status === 'wishlist').length, [rows])

  if (!open) return null

  /* -------------------------------------------------------------------------
   * File handling
   * ----------------------------------------------------------------------- */

  async function handleFile(file: File) {
    if (!file.name.toLowerCase().endsWith('.csv')) {
      toast.push('Le fichier doit être un .csv', 'error')
      return
    }
    try {
      const text = await file.text()
      const parsed = parseGameEyeCsv(text)
      if (parsed.length === 0) {
        toast.push('Aucune ligne valide trouvée dans le CSV', 'error')
        return
      }
      setRows(parsed)
      setFileName(file.name)
      setPhase('preview')
    } catch (e) {
      toast.push(`Erreur de lecture : ${String(e)}`, 'error')
    }
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault()
    setDragOver(false)
    const file = e.dataTransfer.files?.[0]
    if (file) handleFile(file)
  }

  /* -------------------------------------------------------------------------
   * Import execution
   * ----------------------------------------------------------------------- */

  async function runImport() {
    setPhase('running')
    const r: ImportReport = { added: 0, alreadyOwned: 0, notFound: [], failed: [], upserted: 0 }

    // 1) Fetch full catalogue once and build a match index.
    const all = await api.items.list()
    const byKey = new Map<string, Item>()
    for (const it of all) {
      byKey.set(matchKey(it.title, it.platform), it)
    }

    // 2) Fetch the user's existing collection + wishlist to skip duplicates.
    const [ownedNow, wishNow] = await Promise.all([api.collection.list(), api.wishlist.list()])
    const ownedIds = new Set(ownedNow.map((i) => i.id))
    const wishIds = new Set(wishNow.map((i) => i.id))

    setProgress({ done: 0, total: rows.length, currentTitle: '' })

    // 3) Iterate.
    for (let i = 0; i < rows.length; i++) {
      const row = rows[i]
      setProgress({ done: i, total: rows.length, currentTitle: row.title })

      const key = matchKey(row.title, row.platform)
      let item = byKey.get(key)

      // Upsert if missing — any logged-in user can contribute to the catalogue.
      if (!item && createMissing) {
        try {
          const created = await api.items.upsert({
            external_id: row.externalId,
            title: row.title,
            platform: row.platform,
            type: row.type,
            region: row.region,
            condition: row.condition,
            status: row.status,
            developer: row.developer ?? '',
            publisher: row.publisher ?? '',
            genres: row.genres ?? '',
          })
          byKey.set(key, created)
          item = created
          r.upserted += 1
        } catch (e) {
          // Surface the real error so users can see what's wrong.
          r.failed.push({ title: row.title, error: `Upsert failed: ${String(e)}` })
          continue
        }
      }

      if (!item) {
        r.notFound.push({ title: row.title, platform: row.platform })
        continue
      }

      const targetSet = row.status === 'wishlist' ? wishIds : ownedIds
      if (targetSet.has(item.id)) {
        r.alreadyOwned += 1
        continue
      }

      try {
        if (row.status === 'wishlist') {
          await api.wishlist.add(item.id)
          wishIds.add(item.id)
        } else {
          await api.collection.add(item.id)
          ownedIds.add(item.id)
        }
        r.added += 1
      } catch (e) {
        r.failed.push({ title: row.title, error: String(e) })
      }
    }

    setProgress({ done: rows.length, total: rows.length, currentTitle: '' })
    setReport(r)
    setPhase('done')
    onComplete?.(r)
    if (r.failed.length === 0) {
      toast.push(`${r.added} items ajoutés à ta vault`, 'success')
    } else {
      toast.push(`${r.added} ajoutés, ${r.failed.length} échecs`, 'error')
    }
  }

  /* -------------------------------------------------------------------------
   * Render
   * ----------------------------------------------------------------------- */

  return createPortal(
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm tv-enter"
      onClick={(e) => {
        if (e.target === e.currentTarget && phase !== 'running') onClose()
      }}
    >
      <Card padding="lg" className="w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between gap-4 mb-6">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-lg bg-indigo/15 text-indigo-ink flex items-center justify-center">
              <MaterialIcon name="upload_file" size={22} />
            </div>
            <div>
              <h2 className="tv-section text-ink">Import GameEye CSV</h2>
              <p className="text-xs text-ink-muted">
                Format gameeye / ge-tracker — toutes les colonnes sont reconnues
              </p>
            </div>
          </div>
          {phase !== 'running' && (
            <button
              onClick={onClose}
              className="text-ink-muted hover:text-ink focus-ring rounded-md"
              aria-label="Fermer"
            >
              <MaterialIcon name="close" size={22} />
            </button>
          )}
        </div>

        {/* Phase: idle (drop zone) */}
        {phase === 'idle' && (
          <div
            onDrop={handleDrop}
            onDragOver={(e) => {
              e.preventDefault()
              setDragOver(true)
            }}
            onDragLeave={() => setDragOver(false)}
            onClick={() => fileInputRef.current?.click()}
            className={[
              'rounded-lg border-2 border-dashed p-10 text-center cursor-pointer transition-all',
              dragOver
                ? 'border-indigo bg-indigo/5'
                : 'border-line/40 hover:border-indigo/50 hover:bg-panel/40',
            ].join(' ')}
          >
            <MaterialIcon name="cloud_upload" size={40} className="text-indigo-ink mb-3" />
            <p className="text-sm font-semibold text-ink">
              Glisse ton fichier <code className="text-indigo-ink">.csv</code> ici
            </p>
            <p className="text-xs text-ink-muted mt-1">ou clique pour le choisir</p>
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv,text/csv"
              hidden
              onChange={(e) => {
                const f = e.target.files?.[0]
                if (f) handleFile(f)
              }}
            />
          </div>
        )}

        {/* Phase: preview */}
        {phase === 'preview' && (
          <div className="space-y-4">
            <div className="flex items-center gap-2 flex-wrap">
              <Chip tone="primary" icon="description">
                {fileName}
              </Chip>
              <Chip tone="success">
                {rows.length - wishlistCount} owned
              </Chip>
              {wishlistCount > 0 && (
                <Chip tone="primary">
                  {wishlistCount} wishlist
                </Chip>
              )}
            </div>

            {/* Preview table — first 5 rows */}
            <div className="rounded-lg border border-line/30 overflow-hidden">
              <table className="w-full text-xs">
                <thead className="bg-well">
                  <tr className="text-left text-ink-muted tv-engrave">
                    <th className="px-3 py-2">Title</th>
                    <th className="px-3 py-2">Platform</th>
                    <th className="px-3 py-2">Cond.</th>
                    <th className="px-3 py-2">Type</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.slice(0, 5).map((r) => (
                    <tr key={r.externalId} className="border-t border-line/20">
                      <td className="px-3 py-2 text-ink truncate max-w-[200px]">{r.title}</td>
                      <td className="px-3 py-2 text-ink-muted">{r.platform}</td>
                      <td className="px-3 py-2 uppercase text-indigo-ink font-bold">{r.condition}</td>
                      <td className="px-3 py-2 text-ink-muted">{r.type}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {rows.length > 5 && (
                <p className="px-3 py-2 text-xs text-ink-muted text-center bg-well/30">
                  + {rows.length - 5} autres lignes
                </p>
              )}
            </div>

            {/* Catalogue contribution option */}
            <label className="flex items-start gap-3 p-3 rounded-lg border border-line/30 cursor-pointer hover:bg-panel/40 transition-colors">
              <input
                type="checkbox"
                checked={createMissing}
                onChange={(e) => setCreateMissing(e.target.checked)}
                className="mt-0.5 accent-indigo"
              />
              <div className="flex-1">
                <p className="text-sm font-semibold text-ink">
                  Créer les items manquants dans le catalogue
                </p>
                <p className="text-xs text-ink-muted mt-0.5">
                  Les titres absents du catalogue partagé seront ajoutés automatiquement (sans cover IGDB pour l'instant).
                </p>
              </div>
            </label>

            <div className="flex gap-2 justify-end pt-2">
              <Button variant="ghost" onClick={() => setPhase('idle')}>
                Annuler
              </Button>
              <Button leftIcon="play_arrow" onClick={runImport}>
                Lancer l'import ({rows.length})
              </Button>
            </div>
          </div>
        )}

        {/* Phase: running */}
        {phase === 'running' && (
          <div className="space-y-4 py-4">
            <p className="text-sm text-ink-muted text-center">
              Import en cours…{' '}
              <span className="font-bold text-ink tabular-nums">
                {progress.done}/{progress.total}
              </span>
            </p>
            <div className="h-2 rounded-full bg-well overflow-hidden">
              <div
                className="h-full bg-indigo transition-all"
                style={{ width: `${progress.total ? (progress.done / progress.total) * 100 : 0}%` }}
              />
            </div>
            {progress.currentTitle && (
              <p className="text-xs text-ink-muted text-center truncate">
                {progress.currentTitle}
              </p>
            )}
          </div>
        )}

        {/* Phase: done — report */}
        {phase === 'done' && report && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
              <div className="rounded-lg bg-led-sync/10 border border-led-sync/30 p-3">
                <p className="text-2xl font-extrabold text-gain tabular-nums">{report.added}</p>
                <p className="text-[10px] tv-engrave text-ink-muted">Ajoutés</p>
              </div>
              <div className="rounded-lg bg-well/40 border border-line/30 p-3">
                <p className="text-2xl font-extrabold text-ink tabular-nums">
                  {report.alreadyOwned}
                </p>
                <p className="text-[10px] tv-engrave text-ink-muted">Déjà présents</p>
              </div>
              <div className="rounded-lg bg-snes-b/10 border border-snes-b/30 p-3">
                <p className="text-2xl font-extrabold text-ink tabular-nums">
                  {report.notFound.length}
                </p>
                <p className="text-[10px] tv-engrave text-ink-muted">Non trouvés</p>
              </div>
              <div className="rounded-lg bg-led-power/10 border border-led-power/30 p-3">
                <p className="text-2xl font-extrabold text-loss tabular-nums">{report.failed.length}</p>
                <p className="text-[10px] tv-engrave text-ink-muted">Échecs</p>
              </div>
            </div>

            {report.upserted > 0 && (
              <p className="text-xs text-indigo-ink">
                <MaterialIcon name="auto_awesome" size={14} className="mr-1" />
                {report.upserted} item{report.upserted > 1 ? 's' : ''} créé
                {report.upserted > 1 ? 's' : ''} dans le catalogue.
              </p>
            )}

            {report.notFound.length > 0 && (
              <details className="rounded-lg border border-line/30 p-3">
                <summary className="text-sm text-ink-muted cursor-pointer">
                  Voir les {report.notFound.length} items non trouvés
                </summary>
                <ul className="mt-3 space-y-1 max-h-40 overflow-y-auto text-xs text-ink-muted">
                  {report.notFound.slice(0, 50).map((n) => (
                    <li key={`${n.title}-${n.platform}`}>
                      • <span className="text-ink">{n.title}</span> — {n.platform}
                    </li>
                  ))}
                  {report.notFound.length > 50 && (
                    <li className="opacity-70">+ {report.notFound.length - 50} autres…</li>
                  )}
                </ul>
              </details>
            )}

            {report.failed.length > 0 && (
              <details className="rounded-lg border border-led-power/30 p-3">
                <summary className="text-sm text-loss cursor-pointer">
                  Voir les {report.failed.length} échecs
                </summary>
                <ul className="mt-3 space-y-1 max-h-40 overflow-y-auto text-xs">
                  {report.failed.slice(0, 50).map((f, i) => (
                    <li key={i} className="text-loss">
                      • {f.title} — {f.error}
                    </li>
                  ))}
                </ul>
              </details>
            )}

            <div className="flex justify-end gap-2 pt-2">
              <Button variant="ghost" onClick={() => setPhase('idle')}>
                Importer un autre CSV
              </Button>
              <Button leftIcon="check" onClick={onClose}>
                Terminer
              </Button>
            </div>
          </div>
        )}
      </Card>
    </div>,
    document.body
  )
}
