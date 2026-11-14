import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { tokenColor, useTheme } from '../../lib/theme'
import { formatEuro } from '../../lib/format'

export interface ChartPoint {
  label: string
  value: number
}

interface Props {
  data: ChartPoint[]
  height?: number
  className?: string
}

interface TooltipProps {
  active?: boolean
  payload?: Array<{ value?: number }>
  label?: string
}

function ChartTooltip({ active, payload, label }: TooltipProps) {
  const value = payload?.[0]?.value
  if (!active || value == null) return null
  return (
    <div className="rounded-sm bg-keycap px-3 py-2 shadow-float">
      <p className="tv-serial">{label}</p>
      <p className="tv-price text-[15px] text-ink">{formatEuro(value, 2)}</p>
    </div>
  )
}

/** One price series on the recessed chart screen; colours follow the current shell. */
export default function PriceChart({ data, height = 220, className = '' }: Props) {
  useTheme() // re-render with the resolved colours when the shell changes
  const line = tokenColor('indigo-ink')
  const muted = tokenColor('ink-faint')
  const grid = tokenColor('line')
  const accent = tokenColor('snes-a')
  return (
    <div className={className} style={{ height }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 10, right: 12, bottom: 4, left: 4 }}>
          <CartesianGrid stroke={grid} strokeDasharray="2 4" vertical={false} />
          <XAxis dataKey="label" tickLine={false} axisLine={false} tick={{ fill: muted, fontSize: 11, fontFamily: 'IBM Plex Mono' }} minTickGap={24} />
          <YAxis
            tickLine={false}
            axisLine={false}
            width={56}
            tick={{ fill: muted, fontSize: 11, fontFamily: 'IBM Plex Mono' }}
            tickFormatter={(v: number) => formatEuro(v)}
            domain={['auto', 'auto']}
          />
          <Tooltip cursor={{ stroke: grid, strokeWidth: 1 }} content={<ChartTooltip />} />
          <Line
            type="monotone"
            dataKey="value"
            stroke={line}
            strokeWidth={2.5}
            dot={false}
            activeDot={{ r: 5, fill: accent, stroke: 'none' }}
            isAnimationActive={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
