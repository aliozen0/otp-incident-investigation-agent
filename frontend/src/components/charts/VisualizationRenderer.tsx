import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { VisualizationSpec, VisualizationUnit } from '../../api/types'
import { formatNumber } from '../../lib/labels'

const COLORS = ['#1859c9', '#167345', '#9a5808', '#6f42c1']
const ALLOWED_TYPES = new Set(['LINE', 'BAR', 'GROUPED_BAR', 'GAUGE', 'TABLE'])

function formatValue(value: number, unit: VisualizationUnit): string {
  const formatted = formatNumber(value, unit === 'COUNT' || unit === 'CONNECTIONS' ? 0 : 2)
  if (unit === 'PERCENT') return `${formatted}%`
  if (unit === 'MILLISECONDS') return `${formatted} ms`
  return formatted
}

export function VisualizationRenderer({ visualization }: { visualization: VisualizationSpec }) {
  if (!ALLOWED_TYPES.has(visualization.type) || !visualization.series?.length || !visualization.points?.length) {
    return <p className="chart-fallback" role="status">Bu analiz görseli güvenli biçimde gösterilemiyor.</p>
  }

  const rows = visualization.points.reduce<Record<string, string | number>[]>((all, point) => {
    let row = all.find((item) => item.label === point.label)
    if (!row) {
      row = { label: point.label }
      all.push(row)
    }
    row[point.seriesKey] = point.value
    return all
  }, [])

  if (visualization.type === 'TABLE') {
    return (
      <figure className="visualization-card">
        <figcaption>{visualization.title}</figcaption>
        <div className="overflow-x-auto"><table className="visualization-table"><thead><tr><th>Ölçüm</th>{visualization.series.map(s => <th key={s.key}>{s.label}</th>)}</tr></thead><tbody>{rows.map(row => <tr key={String(row.label)}><th>{row.label}</th>{visualization.series.map(s => <td key={s.key}>{typeof row[s.key] === 'number' ? formatValue(row[s.key] as number, visualization.unit) : '—'}</td>)}</tr>)}</tbody></table></div>
      </figure>
    )
  }

  if (visualization.type === 'GAUGE') {
    const point = visualization.points[0]
    return <figure className="visualization-card"><figcaption>{visualization.title}</figcaption><div className="dynamic-gauge" role="meter" aria-label={visualization.title} aria-valuenow={point.value}><strong>{formatValue(point.value, visualization.unit)}</strong><span>{point.label}</span></div></figure>
  }

  const common = { data: rows, margin: { top: 8, right: 12, bottom: 8, left: 0 } }
  return (
    <figure className="visualization-card">
      <figcaption>{visualization.title}</figcaption>
      <div className="h-64 w-full" aria-label={visualization.title} role="img">
        <ResponsiveContainer width="100%" height="100%">
          {visualization.type === 'LINE' ? (
            <LineChart {...common}>
              <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" /><YAxis /><Tooltip formatter={(value) => formatValue(Number(value), visualization.unit)} /><Legend />
              {visualization.series.map((series, index) => <Line key={series.key} type="monotone" dataKey={series.key} name={series.label} stroke={COLORS[index]} strokeWidth={2} />)}
            </LineChart>
          ) : (
            <BarChart {...common}>
              <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" /><YAxis /><Tooltip formatter={(value) => formatValue(Number(value), visualization.unit)} /><Legend />
              {visualization.series.map((series, index) => <Bar key={series.key} dataKey={series.key} name={series.label} fill={COLORS[index]} />)}
            </BarChart>
          )}
        </ResponsiveContainer>
      </div>
    </figure>
  )
}
