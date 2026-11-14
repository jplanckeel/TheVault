interface Props {
  points: number[]
  width?: number
  height?: number
  label?: string
  className?: string
}

/** Value curve drawn on the recessed screen: indigo line, red square on the last point. */
export default function Sparkline({ points, width = 300, height = 72, label = 'Évolution de la valeur', className = '' }: Props) {
  if (points.length < 2) return null
  const pad = 6
  const min = Math.min(...points)
  const max = Math.max(...points)
  const span = max - min || 1
  const coords = points.map((p, i) => [pad + (i * (width - pad * 2)) / (points.length - 1), pad + (1 - (p - min) / span) * (height - pad * 2)])
  const last = coords[coords.length - 1]
  return (
    <svg viewBox={`0 0 ${width} ${height}`} preserveAspectRatio="none" className={`block w-full overflow-visible ${className}`} style={{ height }} role="img" aria-label={label}>
      <line x1={0} x2={width} y1={height - 0.5} y2={height - 0.5} style={{ stroke: 'var(--line)' }} strokeWidth={1} vectorEffect="non-scaling-stroke" />
      <polyline points={coords.map((c) => c.join(',')).join(' ')} fill="none" style={{ stroke: 'var(--indigo-ink)' }} strokeWidth={2.5} strokeLinejoin="round" strokeLinecap="round" vectorEffect="non-scaling-stroke" />
      <rect x={last[0] - 4} y={last[1] - 4} width={8} height={8} rx={2} style={{ fill: 'var(--snes-a)' }} />
    </svg>
  )
}
