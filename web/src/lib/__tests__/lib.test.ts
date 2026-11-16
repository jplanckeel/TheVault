import { describe, expect, it } from 'vitest'
import type { Item } from '../../types'
import { familyOf, shortPlatform } from '../platforms'
import { conditionPips, parseCondition } from '../condition'
import { bestPrice, buildPriceMap, totalValue } from '../prices'
import { buildCollectionView } from '../collectionView'
import { formatEuroOrDash } from '../format'
import { itemPath } from '../routes'

function item(id: string, patch: Partial<Item> = {}): Item {
  return {
    id,
    external_id: id,
    type: 'game',
    title: id,
    platform: 'GameCube',
    region: 'PAL',
    condition: 'cib',
    status: 'owned',
    cover_url: '',
    igdb_id: '',
    summary: '',
    genres: '',
    developer: '',
    publisher: '',
    release_year: 0,
    created_at: '2026-01-01T00:00:00Z',
    updated_at: '2026-01-01T00:00:00Z',
    ...patch,
  }
}

describe('platforms', () => {
  it('finds the manufacturer like the mobile app', () => {
    expect(familyOf('GameCube')).toBe('nintendo')
    expect(familyOf('Super Nintendo Entertainment System')).toBe('nintendo')
    expect(familyOf('PlayStation 2')).toBe('sony')
    expect(familyOf('Xbox 360')).toBe('xbox')
    expect(familyOf('Sega Mega Drive')).toBe('sega')
    expect(familyOf('Dreamcast')).toBe('sega')
    expect(familyOf('PC (Microsoft Windows)')).toBe('other')
    expect(familyOf('')).toBe('other')
  })

  it('shortens platform names for tags', () => {
    expect(shortPlatform('Super Nintendo Entertainment System')).toBe('SNES')
    expect(shortPlatform('PlayStation 2')).toBe('PS2')
    expect(shortPlatform('PlayStation')).toBe('PS1')
    expect(shortPlatform('Nintendo Switch')).toBe('Switch')
    expect(shortPlatform('GameCube')).toBe('GameCube')
  })
})

describe('condition', () => {
  it('normalises catalogue conditions', () => {
    expect(parseCondition('CIB')).toBe('cib')
    expect(parseCondition('mint')).toBe('new')
    expect(parseCondition('Scellé')).toBe('sealed')
    expect(parseCondition('graded')).toBeNull()
    expect(parseCondition(undefined)).toBeNull()
    expect(conditionPips('sealed')).toBe(4)
  })
})

describe('prices', () => {
  const prices = buildPriceMap([
    { id: '1', item_id: 'a', source: 'ebay_pal', condition: 'cib', price_min: 1, price_max: 2, price_median: 30, sample_count: 5, currency: 'EUR', scraped_at: '' },
    { id: '2', item_id: 'b', source: 'ebay_pal', condition: 'loose', price_min: 1, price_max: 2, price_median: 10, sample_count: 5, currency: 'EUR', scraped_at: '' },
  ])

  it('prefers the item condition, then cib, loose and new', () => {
    expect(bestPrice(item('a'), prices)).toBe(30)
    expect(bestPrice(item('b'), prices)).toBe(10)
    expect(bestPrice(item('c'), prices)).toBeUndefined()
    expect(totalValue([item('a'), item('b'), item('c')], prices)).toBe(40)
  })

  it('formats unknown amounts as a dash', () => {
    expect(formatEuroOrDash(undefined)).toBe('—')
    expect(formatEuroOrDash(0)).toBe('—')
    expect(formatEuroOrDash(12)).toMatch(/^12\s€$/)
  })
})

describe('collection view', () => {
  const all = [
    item('Zelda', { created_at: '2026-03-01T00:00:00Z' }),
    item('Mario', { platform: 'Wii', created_at: '2026-02-01T00:00:00Z' }),
    item('Halo', { platform: 'Xbox', created_at: '2026-01-01T00:00:00Z' }),
    item('GameCube', { type: 'console' }),
  ]
  const prices = buildPriceMap([])

  it('filters by type, platform and search, sorted by title', () => {
    const v = buildCollectionView(all, { type: 'game', query: '', platform: null, sort: 'title' }, prices)
    expect(v.items.map((i) => i.title)).toEqual(['Halo', 'Mario', 'Zelda'])
    expect(v.countByType).toEqual({ game: 3, console: 1, accessory: 0 })
    expect(buildCollectionView(all, { type: 'game', query: 'mar', platform: null, sort: 'recent' }, prices).items.map((i) => i.title)).toEqual(['Mario'])
    expect(buildCollectionView(all, { type: 'game', query: '', platform: 'wii', sort: 'recent' }, prices).items.map((i) => i.title)).toEqual(['Mario'])
  })

  it('groups platforms by manufacturer, Nintendo first', () => {
    const v = buildCollectionView(all, { type: 'game', query: '', platform: null, sort: 'recent' }, prices)
    expect(v.platformSummaries.map((p) => p.platform)).toEqual(['GameCube', 'Wii', 'Xbox'])
    expect(v.items[0].title).toBe('Zelda')
  })

  it('routes each kind to its sheet', () => {
    expect(itemPath({ id: '1', type: 'console' })).toBe('/consoles/1')
    expect(itemPath({ id: '1', type: 'game' })).toBe('/items/1')
  })
})

describe('loginError', () => {
  it('tells a wrong password from a rate limit and a server error', async () => {
    const { loginError } = await import('../../AuthContext')
    expect(loginError(401, null)).toBe('Email ou mot de passe incorrect.')
    expect(loginError(429, '42')).toBe('Trop de tentatives de connexion. Réessaie dans 42 s.')
    expect(loginError(429, null)).toContain('une minute')
    expect(loginError(502, null)).toContain('erreur 502')
  })
})

describe('consolePhotosMessage', () => {
  it('sums up a console photo run', async () => {
    const { consolePhotosMessage } = await import('../../components/admin/JobsPanel')
    expect(consolePhotosMessage({ updated: 20, failed: 0, unmatched: 3 })).toBe('20 console(s) mises à jour · 3 sans photo libre (à choisir dans Données)')
    expect(consolePhotosMessage({ updated: 0, failed: 2, unmatched: 0 })).toBe('Aucune console à mettre à jour · 2 échec(s) de téléchargement')
  })
})
