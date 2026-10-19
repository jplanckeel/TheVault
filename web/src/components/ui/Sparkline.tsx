import { Area, AreaChart, ResponsiveContainer } from 'recharts'

interface Props {
  data: number[]
  height?: number
  /** Stroke color */
  tone?: 'primary' | 'success' | 'error'
  className?: string
}

const TONES = {
  primary: { stroke: '#d8b9ff', fill: 'rgba(174,114,255,0.25)' },
  success: { stroke: '#34d399', fill: 'rgba(52,211,153,0.2)' },
  error: { stroke: '#ffb4ab', fill: 'rgba(255,180,171,0.2)' },
}

/**
 * Tiny inline chart for cards — no axes, no tooltip.
 * Pass an array of numbers; the chart will autoscale to fit.
 */
export default function Sparkline({ data, height = 60, tone = 'primary', className = '' }: Props) {
  const series = data.map((v, i) => ({ i, v }))
  const { stroke, fill } = TONES[tone]
  const gradId = `spark-${tone}-${Math.random().toString(36).slice(2, 7)}`
  return (
    <div className={className} style={{ height }}>
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={series} margin={{ top: 2, right: 2, bottom: 2, left: 2 }}>
          <defs>
            <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={fill} stopOpacity={1} />
              <stop offset="100%" stopColor={fill} stopOpacity={0} />
            </linearGradient>
          </defs>
          <Area
            type="monotone"
            dataKey="v"
            stroke={stroke}
            strokeWidth={2}
            fill={`url(#${gradId})`}
            isAnimationActive
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
