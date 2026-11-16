/** Cartridge mark: a game cartridge whose label carries a safe dial (design system « Logo »). */
const MARK =
  'M16 4H48A6 6 0 0 1 54 10V48L48 58H16L10 48V10A6 6 0 0 1 16 4Z M21 12H43A4 4 0 0 1 47 16V36A4 4 0 0 1 43 40H21A4 4 0 0 1 17 36V16A4 4 0 0 1 21 12Z M23 26a9 9 0 1 0 18 0a9 9 0 1 0 -18 0Z M27 26a5 5 0 1 0 10 0a5 5 0 1 0 -10 0Z M30 26a2 2 0 1 0 4 0a2 2 0 1 0 -4 0Z M21 45h4v6h-4Z M27 45h4v6h-4Z M33 45h4v6h-4Z M39 45h4v6h-4Z'

/** The A/B/X/Y diamond of the SNES PAL pad. Rare: logo, empty states, loading. */
export function SnesDots({ size = 24, className = '' }: { size?: number; className?: string }) {
  const d = Math.round(size * 0.36)
  const m = (size - d) / 2
  const dot = (cls: string, x: number, y: number) => (
    <span key={cls} className={`tv-dots-dot tv-dots-${cls}`} style={{ width: d, height: d, left: x, top: y }} />
  )
  return (
    <span className={`tv-dots ${className}`} style={{ width: size, height: size }} aria-hidden="true">
      {dot('x', m, 0)}
      {dot('y', 0, m)}
      {dot('a', size - d, m)}
      {dot('b', m, size - d)}
    </span>
  )
}

interface LogoProps {
  size?: number
  wordmark?: boolean
  tagline?: boolean
  dots?: boolean
  className?: string
}

/** « TheVault » mark + wordmark + diamond. */
export function Logo({ size = 32, wordmark = true, tagline = false, dots = true, className = '' }: LogoProps) {
  return (
    <span className={`tv-logo ${className}`} role="img" aria-label="TheVault">
      <svg width={size} height={size} viewBox="0 0 64 64" aria-hidden="true" className="tv-logo-mark">
        <path d={MARK} fillRule="evenodd" fill="currentColor" />
      </svg>
      {wordmark && (
        <span className="tv-logo-text" aria-hidden="true">
          <span className="tv-logo-word" style={{ fontSize: Math.round(size * 0.66) }}>
            TheVault
          </span>
          {tagline && <span className="tv-engrave">Collector System</span>}
        </span>
      )}
      {wordmark && dots && <SnesDots size={Math.round(size * 0.5)} />}
    </span>
  )
}

interface LedProps {
  color?: 'sync' | 'power' | 'indigo'
  on?: boolean
  blink?: boolean
  label?: string
  className?: string
}

/** Small console LED, always with a label when it carries meaning. */
export function StatusLed({ color = 'sync', on = true, blink, label, className = '' }: LedProps) {
  return (
    <span className={`tv-ledwrap ${className}`}>
      <span className={`tv-led tv-led-${color} ${blink ? 'is-blink' : ''} ${on ? '' : 'is-off'}`} aria-hidden="true" />
      {label && <span className="tv-led-label">{label}</span>}
    </span>
  )
}
