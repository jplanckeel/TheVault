import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Item } from '../types'

// ─── Platform → controller SVG mapping ──────────────────────────────────────
// All 30 available SVGs are mapped below.

const PLATFORM_CONTROLLER: Record<string, string> = {
  // ── Nintendo handhelds ───────────────────────────────────────────────────
  'game boy':           'nes',          // no GB-specific icon
  'game boy color':     'nes',
  'game boy advance':   'nes',
  'gba':                'nes',
  'nintendo ds':        'nes',
  'ds':                 'nes',
  'nintendo 3ds':       'nes',
  '3ds':                'nes',
  'new nintendo 3ds':   'nes',

  // ── Nintendo home consoles ───────────────────────────────────────────────
  'nes':                'nes',
  'famicom':            'nes',
  'snes':               'snes',
  'super nintendo':     'snes',
  'super famicom':      'snes',
  'nintendo 64':        'n64',
  'n64':                'n64',
  'gamecube':           'gamecube',
  'nintendo gamecube':  'gamecube',
  'wii':                'wii',
  'wii classic':        'wii-classic',  // wii-classic.svg
  'wii u':              'wii-u-pro',    // wii-u-pro.svg — the Pro Controller
  'nintendo wii u':     'wii-u-pro',
  'wii u pro':          'wii-u-pro',
  'wii u gamepad':      'wii-u',        // wii-u.svg — the Gamepad
  'virtual boy':        'virtual-boy',  // virtual-boy.svg

  // ── Nintendo Switch ──────────────────────────────────────────────────────
  'nintendo switch':    'joy-cons',
  'switch':             'joy-cons',
  'nintendo switch lite':'joy-cons',
  'switch lite':        'joy-cons',
  'nintendo switch 2':  'joy-cons',
  'switch 2':           'joy-cons',
  'joy-con':            'joy-cons',
  'joy-con (l)':        'joy-con-l',
  'joy-con (r)':        'joy-con-r',
  'switch pro':         'switch-pro',
  'switch pro controller': 'switch-pro',

  // ── Sony ─────────────────────────────────────────────────────────────────
  'playstation':        'ps1',
  'ps1':                'ps1',
  'ps one':             'ps1',
  'playstation 2':      'ps2',
  'ps2':                'ps2',
  'playstation 3':      'ps3',
  'ps3':                'ps3',
  'playstation 4':      'ps4',
  'ps4':                'ps4',
  'playstation 5':      'ps5',
  'ps5':                'ps5',
  'psp':                'ps2',          // no PSP icon, closest is ps2
  'ps vita':            'ps4',          // no Vita icon, closest is ps4

  // ── Sega ─────────────────────────────────────────────────────────────────
  'master system':      'master-system',
  'sega master system': 'master-system',
  'mega drive':         'mega-drive',
  'genesis':            'mega-drive',
  'sega genesis':       'mega-drive',
  'sega mega drive':    'mega-drive',
  'saturn':             'sega-saturn',
  'sega saturn':        'sega-saturn',
  'dreamcast':          'dreamcast',
  'sega dreamcast':     'dreamcast',

  // ── Microsoft Xbox ───────────────────────────────────────────────────────
  'xbox':               'xbox',
  'xbox original':      'xbox',
  'microsoft xbox':     'xbox',
  'xbox (original)':    'xbox',
  'xbox controller s':  'xbox-controller-s',
  'xbox s':             'xbox-controller-s',
  'xbox 360':           'xbox-360',
  'xbox one':           'xbox-one',
  'xbox series x':      'xbox-series-x',
  'xbox series s':      'xbox-series-x',
  'xbox series':        'xbox-series-x',

  // ── Atari ────────────────────────────────────────────────────────────────
  'atari 2600':         'atari-2600',
  'atari':              'atari-2600',
  'atari jaguar':       'atari-jaguar',

  // ── Google ───────────────────────────────────────────────────────────────
  'stadia':             'stadia',            // stadia.svg
  'google stadia':      'stadia',
}

function controllerFor(platform: string): string | null {
  return PLATFORM_CONTROLLER[platform.toLowerCase()] ?? null
}

// ─── Platform ordering (most popular first) ──────────────────────────────────

const PLATFORM_ORDER = [
  'PlayStation 2', 'PlayStation', 'PlayStation 3', 'PlayStation 4', 'PlayStation 5',
  'GameCube', 'Nintendo 64', 'SNES', 'NES', 'Wii', 'Wii U', 'Nintendo Switch',
  'Game Boy Advance', 'Game Boy Color', 'Game Boy', 'Nintendo DS', 'Nintendo 3DS',
  'Dreamcast', 'Sega Saturn', 'Mega Drive', 'Sega Mega Drive', 'Master System',
  'Xbox', 'Xbox 360', 'Xbox One', 'Xbox Series X',
  'Atari 2600', 'Atari Jaguar', 'Virtual Boy',
]

function sortPlatforms(platforms: string[]): string[] {
  return [...platforms].sort((a, b) => {
    const ia = PLATFORM_ORDER.indexOf(a)
    const ib = PLATFORM_ORDER.indexOf(b)
    if (ia === -1 && ib === -1) return a.localeCompare(b)
    if (ia === -1) return 1
    if (ib === -1) return -1
    return ia - ib
  })
}

// ─── Sub-components ──────────────────────────────────────────────────────────

function ControllerIcon({ platform, className }: { platform: string; className?: string }) {
  const icon = controllerFor(platform)
  if (!icon) return <span className={`text-4xl opacity-30 ${className}`}>🎮</span>
  return (
    <img
      src={`/controllercons/${icon}.svg`}
      alt={platform}
      className={className}
      style={{ filter: 'invert(1) opacity(0.9)' }}
    />
  )
}

function ConsoleCard({
  platform, count, onClick, active,
}: {
  platform: string; count: number; onClick: () => void; active: boolean
}) {
  return (
    <button
      onClick={onClick}
      className={`relative overflow-hidden rounded-xl border p-4 flex flex-col items-center gap-2 transition-all text-center w-full ${
        active
          ? 'border-vault-primary bg-vault-surface shadow-[0_0_0_1px_#BA9EFF]'
          : 'border-vault-outline-variant bg-vault-surface hover:border-vault-primary/60'
      }`}
    >
      <ControllerIcon platform={platform} className="w-12 h-12 object-contain" />
      <p className="text-xs font-semibold text-vault-on-surface leading-tight">{platform}</p>
      <span className="text-xs px-2 py-0.5 rounded-full bg-vault-surface-highest text-vault-on-surface-variant">
        {count}
      </span>
    </button>
  )
}

function GameCard({ item, onClick }: { item: Item; onClick: () => void }) {
  return (
    <div
      onClick={onClick}
      className="bg-vault-surface-high border border-vault-outline-variant rounded-xl overflow-hidden cursor-pointer transition-all hover:border-vault-primary hover:shadow-[0_0_0_1px_#BA9EFF] flex gap-3 p-3"
    >
      <div className="shrink-0 w-12 h-16 rounded-lg overflow-hidden bg-vault-surface flex items-center justify-center">
        {item.cover_url ? (
          <img src={item.cover_url} alt={item.title} className="w-full h-full object-cover" />
        ) : (
          <span className="text-xl opacity-20">🎮</span>
        )}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs font-semibold text-vault-on-surface leading-snug line-clamp-2 mb-1">
          {item.title}
        </p>
        {item.release_year > 0 && (
          <p className="text-xs text-vault-on-surface-variant">{item.release_year}</p>
        )}
        {item.genres && (
          <p className="text-xs text-vault-on-surface-variant truncate mt-0.5">{item.genres}</p>
        )}
      </div>
    </div>
  )
}

// ─── Page ────────────────────────────────────────────────────────────────────

export default function HomePage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<Item[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activePlatform, setActivePlatform] = useState<string | null>(null)
  const sectionRefs = useRef<Record<string, HTMLDivElement | null>>({})

  useEffect(() => {
    api.items.list()
      .then((all) => {
        setItems(all)
        // Pre-select the platform with the most games
        const counts: Record<string, number> = {}
        for (const item of all) {
          if (item.type === 'game') counts[item.platform] = (counts[item.platform] ?? 0) + 1
        }
        const top = Object.entries(counts).sort((a, b) => b[1] - a[1])[0]
        if (top) setActivePlatform(top[0])
      })
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false))
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

  // Group games by platform (only type=game)
  const games = items.filter((i) => i.type === 'game')
  const byPlatform: Record<string, Item[]> = {}
  for (const item of games) {
    if (!byPlatform[item.platform]) byPlatform[item.platform] = []
    byPlatform[item.platform].push(item)
  }
  const platforms = sortPlatforms(Object.keys(byPlatform))

  function selectPlatform(platform: string) {
    setActivePlatform(platform)
    setTimeout(() => {
      sectionRefs.current[platform]?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 50)
  }

  const activeGames = activePlatform ? (byPlatform[activePlatform] ?? []) : []

  return (
    <div>
      {/* Header */}
      <div className="mb-8">
        <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-2">
          Backoffice
        </p>
        <h1 className="text-2xl font-semibold text-vault-on-surface">Ma Collection</h1>
        <p className="text-sm text-vault-on-surface-variant mt-1">
          {games.length} jeu{games.length > 1 ? 'x' : ''} · {platforms.length} console{platforms.length > 1 ? 's' : ''}
        </p>
      </div>

      <div className="flex gap-8">
        {/* Left: console selector */}
        <div className="w-64 shrink-0">
          <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-3">
            Consoles
          </p>
          <div className="grid grid-cols-2 gap-2">
            {platforms.map((platform) => (
              <ConsoleCard
                key={platform}
                platform={platform}
                count={byPlatform[platform].length}
                active={activePlatform === platform}
                onClick={() => selectPlatform(platform)}
              />
            ))}
          </div>
        </div>

        {/* Right: games for selected platform */}
        <div className="flex-1 min-w-0">
          {activePlatform && (
            <div ref={(el) => { sectionRefs.current[activePlatform] = el }}>
              {/* Console header */}
              <div className="flex items-center gap-4 mb-6 pb-4 border-b border-vault-outline-variant">
                <div className="w-16 h-16 rounded-xl bg-vault-surface border border-vault-outline-variant flex items-center justify-center p-3">
                  <ControllerIcon platform={activePlatform} className="w-full h-full object-contain" />
                </div>
                <div>
                  <h2 className="text-xl font-semibold text-vault-on-surface">{activePlatform}</h2>
                  <p className="text-sm text-vault-on-surface-variant">
                    {activeGames.length} jeu{activeGames.length > 1 ? 'x' : ''}
                  </p>
                </div>
              </div>

              {/* Games grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
                {activeGames.map((item) => (
                  <GameCard
                    key={item.id}
                    item={item}
                    onClick={() => navigate(`/items/${item.id}`)}
                  />
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
