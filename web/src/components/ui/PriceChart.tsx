import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

export interface ChartPoint {
  /** X axis label (date or period) */
  label: string
  /** Y value */
  value: number
}

export interface MultiSeriesPoint {
  label: string
  [key: string]: string | number | undefined
}

interface Props {
  data: ChartPoint[]
  height?: number
  showXAxis?: boolean
  showYAxis?: boolean
  currency?: string
  className?: string
  /** Multi-series mode: pass series definitions to render multiple lines */
  series?: { key: string; label: string; color: string }[]
  /** Multi-series data (used when series is provided) */
  multiData?: MultiSeriesPoint[]
}

interface TooltipPayload {
  active?: boolean
  payload?: Array<{ value?: number; name?: string; color?: string }>
  label?: string
  currency?: string
}

function CustomTooltip({ active, payload, label, currency = '€' }: TooltipPayload) {
  if (!active || !payload || !payload.length) return null
  return (
    <div className="rounded-lg bg-vault-surface-high border border-vault-outline-variant/30 px-3 py-2 text-xs font-semibold shadow-lg">
      <div className="text-vault-on-surface-variant mb-1">{label}</div>
      {payload.map((entry) => (
        entry.value != null && (
          <div key={entry.name} className="flex items-center gap-2">
            <span className="h-2 w-2 rounded-full" style={{ background: entry.color }} />
            <span className="text-vault-on-surface-variant">{entry.name}</span>
            <span className="text-sm font-extrabold text-vault-on-surface tabular-nums ml-auto">
              {currency}{typeof entry.value === 'number' ? entry.value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : entry.value}
            </span>
          </div>
        )
      ))}
    </div>
  )
}

/**
 * Premium line chart with subtle glow and minimal axes.
 * Supports single series (data + value) or multi-series (multiData + series).
 */
export default function PriceChart({
  data,
  height = 280,
  showXAxis = true,
  showYAxis = false,
  currency = '€',
  className = '',
  series,
  multiData,
}: Props) {
  const isMulti = series && multiData && multiData.length > 0
  const chartData: MultiSeriesPoint[] = isMulti ? multiData : data.map((p) => ({ ...p }))

  return (
    <div className={className} style={{ height }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData} margin={{ top: 8, right: 8, bottom: 8, left: 8 }}>
          <CartesianGrid stroke="rgba(76,67,85,0.25)" strokeDasharray="3 3" vertical={false} />
          <XAxis
            dataKey="label"
            hide={!showXAxis}
            tickLine={false}
            axisLine={false}
            tick={{ fill: '#cec2d7', fontSize: 11 }}
          />
          <YAxis
            hide={!showYAxis}
            tickLine={false}
            axisLine={false}
            tick={{ fill: '#cec2d7', fontSize: 11 }}
          />
          <Tooltip
            cursor={{ stroke: 'rgba(216,185,255,0.3)', strokeWidth: 1 }}
            content={<CustomTooltip currency={currency} />}
          />
          {isMulti ? (
            <>
              <Legend
                verticalAlign="top"
                height={30}
                formatter={(value: string) => <span className="text-xs text-vault-on-surface-variant">{value}</span>}
              />
              {series.map((s) => (
                <Line
                  key={s.key}
                  type="monotone"
                  dataKey={s.key}
                  name={s.label}
                  stroke={s.color}
                  strokeWidth={2}
                  dot={false}
                  activeDot={{ r: 4, fill: s.color, stroke: '#fff', strokeWidth: 2 }}
                  connectNulls
                />
              ))}
            </>
          ) : (
            <Line
              type="monotone"
              dataKey="value"
              stroke="#d8b9ff"
              strokeWidth={2.5}
              dot={false}
              activeDot={{ r: 5, fill: '#d8b9ff', stroke: '#fff', strokeWidth: 2 }}
            />
          )}
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
