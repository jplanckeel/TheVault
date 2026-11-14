/**
 * BrandPlaceholder — affiche le logo constructeur avec fond de couleur de marque
 * quand un item n'a pas de cover_url.
 *
 * Logos attendus dans /public/logos/ :
 *   xbox symbol 9.svg      fond #107C10  (Xbox / Microsoft)
 *   logo_3.svg             fond #E60012  (Nintendo)
 *   playstation logo 3.svg fond #0070CC  (PlayStation / Sony)
 */

interface BrandInfo {
  logo: string
  bg: string
}

const BRAND_MAP: { test: (p: string) => boolean; brand: BrandInfo }[] = [
  {
    test: (p) => /xbox|microsoft/i.test(p),
    brand: { logo: '/logos/xbox.svg', bg: '#107C10' },
  },
  {
    test: (p) => /nintendo|nes|snes|n64|gamecube|wii|switch|game boy|gba|ds|3ds/i.test(p),
    brand: { logo: '/logos/nintendo.svg', bg: '#E60012' },
  },
  {
    test: (p) => /playstation|sony|ps1|ps2|ps3|ps4|ps5|psp|vita/i.test(p),
    brand: { logo: '/logos/playstation.svg', bg: '#0070CC' },
  },
]

function getBrand(platform: string): BrandInfo | null {
  return BRAND_MAP.find(({ test }) => test(platform))?.brand ?? null
}

interface Props {
  platform: string
  className?: string
}

export default function BrandPlaceholder({ platform, className = 'w-full h-full' }: Props) {
  const brand = getBrand(platform)

  if (brand) {
    return (
      <div
        className={`flex items-center justify-center ${className}`}
        style={{ backgroundColor: brand.bg }}
      >
        <img
          src={brand.logo}
          alt={platform}
          className="w-1/2 h-1/2 object-contain"
          style={{ filter: 'brightness(0) invert(1)' }}
        />
      </div>
    )
  }

  // Fallback générique : controllercon SVG
  const slug = platform.toLowerCase().replace(/\s+/g, '-')
  return (
    <div className={`flex items-center justify-center bg-vault-surface-highest ${className}`}>
      <img
        src={`/controllercons/${slug}.svg`}
        alt={platform}
        className="w-2/3 h-2/3 object-contain opacity-60"
        onError={(e) => { (e.target as HTMLImageElement).src = '/controllercons/joy-cons.svg' }}
      />
    </div>
  )
}
