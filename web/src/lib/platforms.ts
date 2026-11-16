/** Manufacturer family of a platform — drives the colour band of covers and tags. */
export type Family = 'nintendo' | 'sony' | 'sega' | 'xbox' | 'other'

const NINTENDO = /nintendo|\bnes\b|famicom|snes|\bn64\b|gamecube|\bwii\b|wii u|switch|game ?boy|\bgb[ac]?\b|\bds\b|\b3ds\b|virtual boy/i
const XBOX = /xbox|microsoft/i
const SONY = /playstation|sony|\bps[1-5]\b|\bpsp\b|vita/i
const SEGA = /sega|mega ?drive|genesis|dreamcast|saturn|game gear|master system/i
const COMPUTER = /^(pc|windows|pc \(microsoft windows\)|mac|macos|linux)$/i

/** Same rules as `manufacturerOf` in composeApp/.../component/Platforms.kt. */
export function familyOf(platform: string | null | undefined): Family {
  const p = (platform ?? '').trim()
  if (!p || COMPUTER.test(p)) return 'other'
  if (XBOX.test(p)) return 'xbox'
  if (SONY.test(p)) return 'sony'
  if (SEGA.test(p)) return 'sega'
  if (NINTENDO.test(p)) return 'nintendo'
  return 'other'
}

export const FAMILY_LABEL: Record<Family, string> = {
  nintendo: 'Nintendo',
  xbox: 'Microsoft',
  sony: 'Sony',
  sega: 'Sega',
  other: 'Autres',
}

/** Display order of manufacturers, as on mobile. */
export const FAMILY_ORDER: Family[] = ['nintendo', 'xbox', 'sony', 'sega', 'other']

/** Manufacturer logo (monochrome, tinted with the ink colour), when one is bundled. */
export function familyLogo(family: Family): string | undefined {
  switch (family) {
    case 'nintendo':
      return '/logos/nintendo.svg'
    case 'sony':
      return '/logos/playstation.svg'
    case 'xbox':
      return '/logos/xbox.svg'
    default:
      return undefined
  }
}

const SHORT_NAMES: Record<string, string> = {
  'nes/famicom': 'NES',
  'nintendo entertainment system': 'NES',
  'super nintendo entertainment system': 'SNES',
  'super nintendo': 'SNES',
  'nintendo 64': 'N64',
  'game boy advance': 'GBA',
  'game boy color': 'GBC',
  'nintendo game boy advance': 'GBA',
  'nintendo game boy color': 'GBC',
  playstation: 'PS1',
  'sony playstation': 'PS1',
  'playstation portable': 'PSP',
  'playstation vita': 'PS Vita',
}

/** Short platform name for tags (`PS2`, `GameCube`) — same as mobile `shortPlatformName`. */
export function shortPlatform(platform: string): string {
  const trimmed = platform.trim()
  const known = SHORT_NAMES[trimmed.toLowerCase()]
  if (known) return known
  const ps = /^(?:sony\s+)?playstation\s+(\d)$/i.exec(trimmed)
  if (ps) return `PS${ps[1]}`
  return trimmed.replace(/^(nintendo|microsoft|sony|sega)\s+/i, '') || trimmed
}
