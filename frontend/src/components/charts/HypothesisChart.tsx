import { BarChart, Bar, XAxis, YAxis, Cell, ResponsiveContainer, Tooltip } from 'recharts'
import type { Hypothesis } from '../../api/types'
import { PROBABILITY_LABEL_TR } from '../../lib/labels'

const PROBABILITY_VALUE: Record<Hypothesis['probability'], number> = {
  HIGH: 1,
  MEDIUM: 0.66,
  LOW: 0.33,
}

// Ordinal blue ramp validated with scripts/validate_palette.js --ordinal (LOW→HIGH lightness).
// LOW nudged from the #B7CDF3 candidate to #8FAEEA to clear the light-end
// contrast floor (2:1 vs --color-paper #F7F7F4) — see task-4-report.md.
const PROBABILITY_COLOR: Record<Hypothesis['probability'], string> = {
  LOW: '#8FAEEA',
  MEDIUM: '#5B87DE',
  HIGH: '#1D4ED8',
}

export function HypothesisChart({ hypotheses }: { hypotheses: Hypothesis[] }) {
  if (hypotheses.length === 0) return null

  const data = [...hypotheses]
    .sort((a, b) => a.rank - b.rank)
    .map((h) => ({
      name: `#${h.rank} ${h.possibleCause}`,
      value: PROBABILITY_VALUE[h.probability],
      probability: h.probability,
    }))

  return (
    <div style={{ width: '100%', height: Math.max(60, data.length * 40) }}>
      <ResponsiveContainer>
        <BarChart data={data} layout="vertical" margin={{ left: 0, right: 16, top: 4, bottom: 4 }}>
          <XAxis type="number" domain={[0, 1]} hide />
          <YAxis
            type="category"
            dataKey="name"
            width={220}
            tick={{ fontSize: 12, fill: 'var(--color-ink)' }}
            axisLine={false}
            tickLine={false}
          />
          <Tooltip
            formatter={(_value, _key, item) =>
              PROBABILITY_LABEL_TR[item.payload.probability as Hypothesis['probability']]
            }
          />
          <Bar dataKey="value" radius={[0, 4, 4, 0]} barSize={14}>
            {data.map((d, i) => (
              <Cell key={i} fill={PROBABILITY_COLOR[d.probability]} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
