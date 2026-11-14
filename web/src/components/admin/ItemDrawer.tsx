import { useEffect, useRef, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import type { AdminItem, Item } from '../../types'
import { api } from '../../api'
import { Button, Chip, IconButton, useToast } from '../ui'
import { ISSUES, ISSUE_ORDER } from '../../lib/adminItems'
import ItemThumb from './ItemThumb'
import ItemForm from './ItemForm'
import MergeSection from './MergeSection'
import ItemHistory from './ItemHistory'

const TYPE_LABEL: Record<Item['type'], string> = { game: 'Jeu', console: 'Console', accessory: 'Accessoire' }

interface Props {
  item: AdminItem
  all: AdminItem[]
  onClose: () => void
  /** Reload the list; `selectId` keeps the drawer open on that item (null closes it). */
  onChanged: (selectId: string | null) => void
}

function Block({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-2xl bg-vault-surface-low border border-vault-outline-variant/30 p-5">
      <h3 className="text-label-bold text-vault-primary mb-4">{title}</h3>
      {children}
    </section>
  )
}

/** Side panel to correct one catalogue item. */
export default function ItemDrawer({ item, all, onClose, onChanged }: Props) {
  const toast = useToast()
  const fileRef = useRef<HTMLInputElement>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [historyKey, setHistoryKey] = useState(0)

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  async function run(name: string, action: () => Promise<unknown>, success: string, selectId: string | null = item.id) {
    setBusy(name)
    try {
      await action()
      toast.push(success, 'success')
      setHistoryKey((k) => k + 1)
      onChanged(selectId)
    } catch (e) {
      toast.push(`Erreur : ${String(e)}`, 'error')
    } finally {
      setBusy(null)
    }
  }

  const issues = ISSUE_ORDER.filter((i) => item.issues.includes(i))
  return createPortal(
    <div className="fixed inset-0 z-50 flex justify-end bg-black/60" onClick={onClose} role="dialog" aria-modal aria-label={`Corriger ${item.title}`}>
      <aside
        className="h-full w-full max-w-2xl overflow-y-auto bg-vault-bg border-l border-vault-outline-variant/30 p-4 md:p-6 space-y-4 animate-fade-in-up"
        onClick={(e) => e.stopPropagation()}
      >
        <header className="flex items-start gap-4">
          <button
            type="button"
            onClick={() => fileRef.current?.click()}
            className="group relative focus-ring rounded-lg"
            title="Importer une jaquette"
          >
            <ItemThumb item={item} className="h-28 w-20 rounded-lg" />
            <span className="absolute inset-0 hidden group-hover:flex items-center justify-center rounded-lg bg-black/60 text-xs font-semibold text-vault-on-surface">
              Changer
            </span>
          </button>
          <input
            ref={fileRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            className="hidden"
            onChange={(e) => {
              const file = e.target.files?.[0]
              if (file) run('cover', () => api.items.uploadCover(item.id, file), 'Jaquette mise à jour')
              e.target.value = ''
            }}
          />
          <div className="min-w-0 flex-1">
            <h2 className="text-headline-md text-vault-on-surface">{item.title}</h2>
            <p className="text-xs text-vault-on-surface-variant font-mono truncate">{item.external_id}</p>
            <div className="mt-2 flex flex-wrap gap-2">
              <Chip size="sm" tone="primary">{TYPE_LABEL[item.type]}</Chip>
              {item.admin_edited && <Chip size="sm" tone="secondary" icon="lock">Corrigé · protégé des synchros</Chip>}
              <Chip size="sm">{item.owners} propriétaire(s)</Chip>
            </div>
          </div>
          <IconButton icon="close" aria-label="Fermer" onClick={onClose} />
        </header>

        <Block title="À corriger">
          {issues.length === 0 ? (
            <p className="text-sm text-vault-success">Aucun problème détecté.</p>
          ) : (
            <div className="flex flex-wrap gap-2 mb-4">
              {issues.map((i) => (
                <Chip key={i} tone={ISSUES[i].tone} icon={ISSUES[i].icon}>{ISSUES[i].label}</Chip>
              ))}
            </div>
          )}
          <div className="flex flex-wrap gap-2">
            {item.type === 'game' && (
              <Button size="sm" variant="subtle" leftIcon="auto_awesome" disabled={busy !== null}
                onClick={() => run('igdb', () => api.admin.refreshIGDB(item.id), 'Enrichissement IGDB lancé')}>
                Relancer IGDB
              </Button>
            )}
            <Button size="sm" variant="subtle" leftIcon="sell" disabled={busy !== null}
              onClick={() => run('price', () => api.admin.refreshPrice(item.id), 'Scraping du prix lancé')}>
              Rafraîchir le prix
            </Button>
            <Button size="sm" variant="subtle" leftIcon="image" disabled={busy !== null} onClick={() => fileRef.current?.click()}>
              Importer une jaquette
            </Button>
          </div>
        </Block>

        <Block title="Informations">
          <ItemForm key={`${item.id}:${item.updated_at}`} item={item} onSaved={() => { setHistoryKey((k) => k + 1); onChanged(item.id) }} />
        </Block>

        <Block title="Doublons et fusion">
          <MergeSection item={item} all={all} onMerged={(keptId) => onChanged(keptId)} />
        </Block>

        <Block title="Historique">
          <ItemHistory itemId={item.id} refreshKey={historyKey} />
        </Block>

        <section className="rounded-2xl border border-vault-error/30 p-5">
          <h3 className="text-label-bold text-vault-error mb-2">Zone dangereuse</h3>
          <p className="text-sm text-vault-on-surface-variant mb-4">
            La suppression retire l'objet des collections ({item.owners}) et de l'historique des prix. Les prochaines synchros de cet objet seront refusées.
          </p>
          {confirmDelete ? (
            <div className="flex flex-wrap gap-2">
              <Button variant="danger" leftIcon="delete_forever" disabled={busy !== null}
                onClick={() => run('delete', () => api.admin.remove(item.id), 'Objet supprimé', null)}>
                Confirmer la suppression
              </Button>
              <Button variant="ghost" onClick={() => setConfirmDelete(false)}>Annuler</Button>
            </div>
          ) : (
            <Button variant="danger" leftIcon="delete" onClick={() => setConfirmDelete(true)}>Supprimer l'objet</Button>
          )}
        </section>
      </aside>
    </div>,
    document.body,
  )
}
