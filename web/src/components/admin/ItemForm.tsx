import { useState } from 'react'
import type { AdminItem, Item } from '../../types'
import { Button, Input, Select, useToast } from '../ui'
import { api } from '../../api'
import { diffPatch, draftOf, type ItemDraft } from '../../lib/adminItems'

const TYPE_OPTIONS = [
  { value: 'game', label: 'Jeu' },
  { value: 'console', label: 'Console' },
  { value: 'accessory', label: 'Accessoire' },
]

interface Props {
  item: AdminItem
  onSaved: (item: Item) => void
}

/**
 * Catalogue fields editor. Only changed fields are sent; the backend then locks the item
 * so that later syncs from the apps do not overwrite the correction.
 */
export default function ItemForm({ item, onSaved }: Props) {
  const toast = useToast()
  const [draft, setDraft] = useState<ItemDraft>(() => draftOf(item))
  const [saving, setSaving] = useState(false)
  const patch = diffPatch(item, draft)
  const dirty = Object.keys(patch).length > 0

  function set<K extends keyof ItemDraft>(key: K, value: ItemDraft[K]) {
    setDraft((d) => ({ ...d, [key]: value }))
  }

  async function save() {
    if (!draft.title.trim()) {
      toast.push('Le titre est obligatoire', 'error')
      return
    }
    setSaving(true)
    try {
      onSaved(await api.admin.update(item.id, patch))
      toast.push('Correction enregistrée', 'success')
    } catch (e) {
      toast.push(`Erreur : ${String(e)}`, 'error')
    } finally {
      setSaving(false)
    }
  }

  const text = (key: Exclude<keyof ItemDraft, 'type' | 'release_year'>, label: string, placeholder = '') => (
    <Input label={label} value={draft[key]} placeholder={placeholder} onChange={(e) => set(key, e.target.value)} />
  )

  return (
    <form
      className="space-y-4"
      onSubmit={(e) => {
        e.preventDefault()
        if (dirty) save()
      }}
    >
      {text('title', 'Titre')}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <Select label="Type" value={draft.type} options={TYPE_OPTIONS} onChange={(e) => set('type', e.target.value as Item['type'])} />
        {text('platform', 'Plateforme', 'PlayStation 2, Switch…')}
        {text('region', 'Région', 'PAL, NTSC-U…')}
        {text('condition', 'État', 'loose, cib, new…')}
        {text('developer', 'Développeur')}
        {text('publisher', 'Éditeur')}
        <Input
          label="Année de sortie"
          type="number"
          min={0}
          max={2100}
          value={draft.release_year || ''}
          onChange={(e) => set('release_year', Number(e.target.value) || 0)}
        />
        {text('igdb_id', 'ID IGDB')}
      </div>
      {text('genres', 'Genres', 'Action, Aventure…')}
      {text('cover_url', 'URL de la jaquette', 'https://…')}
      <label className="block">
        <span className="tv-engrave text-ink-muted mb-2 block">Résumé</span>
        <textarea
          rows={4}
          value={draft.summary}
          onChange={(e) => set('summary', e.target.value)}
          className="w-full rounded-md bg-well border border-line/30 px-3 py-2 text-sm text-ink placeholder:text-ink-muted/60 focus:outline-none focus:border-indigo"
        />
      </label>
      <div className="flex items-center justify-between gap-3 pt-2">
        <p className="text-xs text-ink-muted">
          {dirty ? `${Object.keys(patch).length} champ(s) modifié(s)` : 'Aucune modification'}
        </p>
        <div className="flex gap-2">
          <Button type="button" variant="ghost" disabled={!dirty || saving} onClick={() => setDraft(draftOf(item))}>
            Annuler
          </Button>
          <Button type="submit" leftIcon="save" disabled={!dirty || saving}>
            {saving ? 'Enregistrement…' : 'Enregistrer'}
          </Button>
        </div>
      </div>
    </form>
  )
}
