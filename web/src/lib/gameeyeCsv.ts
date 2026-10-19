/**
 * GameEye CSV import — parses an export from gameeye / ge-tracker and maps
 * each row to the shape required by `POST /api/v1/items` and `POST /api/v1/collection`.
 *
 * Source format (header row present):
 *   Platform,Category,UserRecordType,Title,Country,ReleaseType,Publisher,Developer,
 *   Genre,CreatedAt,Ownership,PriceLoose,PriceCIB,PriceNew,YourPrice,PricePaid,
 *   ItemCondition,BoxCondition,ManualCondition,Notes,Tags,metacritic
 */

export interface GameEyeRow {
  externalId: string
  title: string
  platform: string
  type: 'game' | 'console' | 'accessory'
  region: string
  condition: 'loose' | 'cib' | 'new'
  status: 'owned' | 'wishlist'
  /** Raw price columns (already comma-decimal-cleaned, in EUR). */
  priceLoose?: number
  priceCIB?: number
  priceNew?: number
  pricePaid?: number
  developer?: string
  publisher?: string
  genres?: string
}

/* -----------------------------------------------------------------------------
 * Mapping tables
 * --------------------------------------------------------------------------- */

const CATEGORY_TO_TYPE: Record<string, GameEyeRow['type']> = {
  games: 'game',
  consoles: 'console',
  systems: 'console',
  accessories: 'accessory',
  controller: 'accessory',
}

const OWNERSHIP_TO_CONDITION: Record<string, GameEyeRow['condition']> = {
  cib: 'cib',
  'cib+': 'cib',
  new: 'new',
  boxed: 'new',
  loose: 'loose',
  dématérialisé: 'loose',
  digital: 'loose',
}

const COUNTRY_TO_REGION: Record<string, string> = {
  'united states of america': 'USA',
  'united states': 'USA',
  usa: 'USA',
  'united kingdom': 'EUR',
  uk: 'EUR',
  france: 'EUR',
  germany: 'EUR',
  spain: 'EUR',
  italy: 'EUR',
  europe: 'EUR',
  japan: 'JPN',
  australia: 'AUS',
  canada: 'USA',
}

/* -----------------------------------------------------------------------------
 * CSV parsing — RFC 4180 style with quoted fields
 * --------------------------------------------------------------------------- */

function parseLine(line: string): string[] {
  const out: string[] = []
  let buf = ''
  let inQuotes = false
  for (let i = 0; i < line.length; i++) {
    const c = line[i]
    if (c === '"') {
      if (inQuotes && line[i + 1] === '"') {
        buf += '"'
        i++
      } else {
        inQuotes = !inQuotes
      }
    } else if (c === ',' && !inQuotes) {
      out.push(buf.trim())
      buf = ''
    } else {
      buf += c
    }
  }
  out.push(buf.trim())
  return out
}

export function parseCsv(text: string): Record<string, string>[] {
  const lines = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split('\n').filter((l) => l.trim())
  if (lines.length === 0) return []
  const headers = parseLine(lines[0])
  return lines.slice(1).map((line) => {
    const values = parseLine(line)
    const row: Record<string, string> = {}
    headers.forEach((h, i) => {
      row[h] = values[i] ?? ''
    })
    return row
  })
}

/* -----------------------------------------------------------------------------
 * Mapping
 * --------------------------------------------------------------------------- */

function slugify(s: string): string {
  return s
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
}

function parsePrice(s: string | undefined): number | undefined {
  if (!s) return undefined
  // GameEye uses comma decimal, e.g. "13,42" or "1.234,56"
  const cleaned = s.replace(/\./g, '').replace(',', '.').replace(/[^\d.-]/g, '')
  const n = parseFloat(cleaned)
  if (!isFinite(n) || n < 0) return undefined
  return n
}

export function mapRow(row: Record<string, string>): GameEyeRow | null {
  const title = (row.Title ?? '').trim()
  const platform = (row.Platform ?? '').trim()
  if (!title || !platform) return null

  const cat = (row.Category ?? '').toLowerCase()
  const type = CATEGORY_TO_TYPE[cat] ?? 'game'

  const own = (row.Ownership ?? '').toLowerCase()
  const condition = OWNERSHIP_TO_CONDITION[own] ?? 'loose'

  const country = (row.Country ?? '').toLowerCase()
  const region = COUNTRY_TO_REGION[country] ?? row.Country ?? ''

  const userRecord = (row.UserRecordType ?? '').toLowerCase()
  const status = userRecord === 'wishlist' ? 'wishlist' : 'owned'

  return {
    externalId: slugify(`${title}-${platform}`),
    title,
    platform,
    type,
    region,
    condition,
    status,
    priceLoose: parsePrice(row.PriceLoose),
    priceCIB: parsePrice(row.PriceCIB),
    priceNew: parsePrice(row.PriceNew),
    pricePaid: parsePrice(row.PricePaid),
    developer: row.Developer && row.Developer !== 'Missing Field' ? row.Developer : undefined,
    publisher: row.Publisher && row.Publisher !== 'Missing Field' ? row.Publisher : undefined,
    genres: row.Genre && row.Genre !== 'Missing Field' ? row.Genre : undefined,
  }
}

/** Parse a CSV string and return mapped, valid rows. */
export function parseGameEyeCsv(text: string): GameEyeRow[] {
  return parseCsv(text)
    .map(mapRow)
    .filter((r): r is GameEyeRow => r !== null)
}

/** Build a normalised match key (title + platform) for catalogue lookup. */
export function matchKey(title: string, platform: string): string {
  return slugify(`${title}-${platform}`)
}
