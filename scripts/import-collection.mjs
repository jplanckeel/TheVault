#!/usr/bin/env node
/**
 * Import a GE (Game Eye / ge-tracker) CSV collection export into TheVaultBO.
 *
 * Usage:
 *   node scripts/import-collection.mjs [csv-file] [api-url]
 *
 * Defaults:
 *   csv-file  ./2026_04_14_ge_collection.csv
 *   api-url   http://localhost:8080
 *
 * Example:
 *   node scripts/import-collection.mjs ./my-collection.csv http://localhost:8080
 */

import fs from 'fs'
import path from 'path'

// ─── Config ──────────────────────────────────────────────────────────────────

const CSV_FILE = process.argv[2] ?? './2026_04_14_ge_collection.csv'
const API_URL  = (process.argv[3] ?? 'http://localhost:8080').replace(/\/$/, '')
const ENDPOINT = `${API_URL}/api/v1/items`
const DELAY_MS = 50  // pause entre chaque requête pour ne pas surcharger l'API

// ─── Mappings ────────────────────────────────────────────────────────────────

const CATEGORY_TO_TYPE = {
  'games':       'game',
  'consoles':    'console',
  'systems':     'console',
  'accessories': 'accessory',
  'controller':  'accessory',
}

const OWNERSHIP_TO_CONDITION = {
  'cib':            'cib',
  'cib+':           'cib',
  'new':            'new',
  'boxed':          'new',
  'loose':          'loose',
  'dématérialisé':  'loose',
}

const COUNTRY_TO_REGION = {
  'united states of america': 'USA',
  'united states':            'USA',
  'usa':                      'USA',
  'united kingdom':           'EUR',
  'uk':                       'EUR',
  'france':                   'EUR',
  'germany':                  'EUR',
  'spain':                    'EUR',
  'italy':                    'EUR',
  'europe':                   'EUR',
  'japan':                    'JPN',
  'australia':                'AUS',
  'canada':                   'USA',
}

// ─── CSV Parser ──────────────────────────────────────────────────────────────

function parseCsvLine(line) {
  const fields = []
  let current = ''
  let inQuotes = false

  for (let i = 0; i < line.length; i++) {
    const char = line[i]
    if (char === '"') {
      if (inQuotes && line[i + 1] === '"') {
        current += '"'
        i++
      } else {
        inQuotes = !inQuotes
      }
    } else if (char === ',' && !inQuotes) {
      fields.push(current.trim())
      current = ''
    } else {
      current += char
    }
  }
  fields.push(current.trim())
  return fields
}

function parseCsv(content) {
  const lines = content.replace(/\r\n/g, '\n').replace(/\r/g, '\n').trim().split('\n')
  const headers = parseCsvLine(lines[0])
  return lines.slice(1).filter(l => l.trim()).map(line => {
    const values = parseCsvLine(line)
    const row = {}
    headers.forEach((h, i) => { row[h] = values[i] ?? '' })
    return row
  })
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

function slugify(str) {
  return str
    .toLowerCase()
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
}

function mapRow(row) {
  const type = CATEGORY_TO_TYPE[row.Category?.toLowerCase()] ?? 'game'
  const condition = OWNERSHIP_TO_CONDITION[row.Ownership?.toLowerCase()] ?? 'loose'
  const region = COUNTRY_TO_REGION[row.Country?.toLowerCase()] ?? row.Country ?? ''
  const externalId = slugify(`${row.Title}-${row.Platform}`)

  return {
    external_id: externalId,
    title:       row.Title ?? '',
    platform:    row.Platform ?? '',
    type,
    region,
    condition,
    status:      row.UserRecordType?.toLowerCase() ?? '',
  }
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

// ─── Main ────────────────────────────────────────────────────────────────────

async function main() {
  const csvPath = path.resolve(CSV_FILE)
  if (!fs.existsSync(csvPath)) {
    console.error(`❌  Fichier introuvable : ${csvPath}`)
    process.exit(1)
  }

  const content = fs.readFileSync(csvPath, 'utf8')
  const rows = parseCsv(content)

  console.log(`📂  Fichier    : ${csvPath}`)
  console.log(`🌐  API        : ${ENDPOINT}`)
  console.log(`📦  Items      : ${rows.length}`)
  console.log('')

  let success = 0
  let skipped = 0
  let failed  = 0
  const errors = []

  for (let i = 0; i < rows.length; i++) {
    const row = rows[i]
    const item = mapRow(row)

    if (!item.title || !item.external_id) {
      skipped++
      continue
    }

    process.stdout.write(`[${String(i + 1).padStart(3)}/${rows.length}] ${item.title.padEnd(45).slice(0, 45)} `)

    try {
      const res = await fetch(ENDPOINT, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify(item),
      })

      if (res.ok) {
        process.stdout.write('✅\n')
        success++
      } else {
        const body = await res.text()
        process.stdout.write(`❌  HTTP ${res.status}: ${body}\n`)
        failed++
        errors.push({ title: item.title, status: res.status, body })
      }
    } catch (err) {
      process.stdout.write(`❌  ${err.message}\n`)
      failed++
      errors.push({ title: item.title, error: err.message })
    }

    if (DELAY_MS > 0) await sleep(DELAY_MS)
  }

  console.log('')
  console.log('─'.repeat(55))
  console.log(`✅  Importés   : ${success}`)
  console.log(`⏭️   Ignorés    : ${skipped}`)
  console.log(`❌  Échoués    : ${failed}`)

  if (errors.length > 0) {
    console.log('\nErreurs :')
    errors.forEach(e => console.log(`  - ${e.title}: ${e.body ?? e.error}`))
  }
}

main().catch(err => {
  console.error('Erreur fatale :', err)
  process.exit(1)
})
