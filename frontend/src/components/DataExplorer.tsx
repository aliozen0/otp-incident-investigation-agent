import { useCallback, useEffect, useState } from 'react'
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { getOperationsOverview, getOperationsSamples } from '../api/client'
import { toUserMessage } from '../lib/errors'
import { formatNumber } from '../lib/labels'
import type { OperationsOverview, OperationsSampleRow } from '../api/types'
import {
  IconActivity,
  IconAlert,
  IconClose,
  IconLayers,
  IconRefresh,
  IconShield,
} from './icons'

// Ordinal blue ramp, same family the analysis charts use (scripts/validate_palette.js --ordinal).
const ERROR_COLORS = ['#1D4ED8', '#2F63D6', '#5B87DE', '#8FAEEA', '#B9CDF2']

const RANGES = [
  { label: '15 dk', minutes: 15 },
  { label: '1 saat', minutes: 60 },
  { label: '6 saat', minutes: 360 },
  { label: '24 saat', minutes: 1440 },
] as const

function windowFor(minutes: number): { startAt: string; endAt: string } {
  const end = new Date()
  end.setSeconds(0, 0)
  const start = new Date(end.getTime() - minutes * 60_000)
  return { startAt: start.toISOString(), endAt: end.toISOString() }
}

function clockLabel(iso: string): string {
  return new Intl.DateTimeFormat('tr-TR', { hour: '2-digit', minute: '2-digit' }).format(new Date(iso))
}

function fullLabel(iso: string): string {
  return new Intl.DateTimeFormat('tr-TR', { dateStyle: 'short', timeStyle: 'short' }).format(
    new Date(iso)
  )
}

export function DataExplorer({ onClose }: { onClose: () => void }) {
  const [minutes, setMinutes] = useState<number>(60)
  const [overview, setOverview] = useState<OperationsOverview | null>(null)
  const [samples, setSamples] = useState<OperationsSampleRow[]>([])
  const [providerFilter, setProviderFilter] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    const range = windowFor(minutes)
    try {
      const [nextOverview, nextSamples] = await Promise.all([
        getOperationsOverview(range.startAt, range.endAt),
        getOperationsSamples(range.startAt, range.endAt, providerFilter || undefined),
      ])
      setOverview(nextOverview)
      setSamples(nextSamples)
    } catch (failure) {
      setError(toUserMessage(failure))
    } finally {
      setLoading(false)
    }
  }, [minutes, providerFilter])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [onClose])

  const totals = overview?.totals
  const degradedProvider = overview?.providers.find((provider) => provider.status === 'DEGRADED')

  return (
    <div
      className="modal-backdrop"
      onMouseDown={(event) => event.target === event.currentTarget && onClose()}
    >
      <div className="modal-shell modal-shell-wide" role="dialog" aria-modal="true" aria-label="Operasyon verileri">
        <div className="modal-head">
          <div>
            <p className="eyebrow">OPERATIONAL DATA</p>
            <h2 className="mt-1 font-display text-lg font-semibold tracking-tight text-ink">
              Veri gezgini
            </h2>
            <p className="mt-1 text-xs text-ink-muted">
              Agent'ın araçlarının okuduğu ham satırlar. Buradaki sayılarla modelin cevabını yan yana
              koyup doğrulayabilirsiniz.
            </p>
          </div>
          <button type="button" onClick={onClose} className="icon-button" aria-label="Kapat">
            <IconClose size={18} />
          </button>
        </div>

        <div className="flex flex-wrap items-center gap-2 border-b border-line px-6 py-3">
          <div className="flex gap-1.5">
            {RANGES.map((range) => (
              <button
                key={range.minutes}
                type="button"
                onClick={() => setMinutes(range.minutes)}
                className={`range-chip ${minutes === range.minutes ? 'is-active' : ''}`}
              >
                {range.label}
              </button>
            ))}
          </div>
          <label htmlFor="provider-filter" className="sr-only">Operatör filtresi</label>
          <select
            id="provider-filter"
            value={providerFilter}
            onChange={(event) => setProviderFilter(event.target.value)}
            className="field-control ml-1 w-auto"
          >
            <option value="">Tüm operatörler</option>
            {(overview?.providers ?? []).map((provider) => (
              <option key={provider.provider} value={provider.provider}>{provider.provider}</option>
            ))}
          </select>
          <button type="button" onClick={() => void load()} disabled={loading} className="secondary-button ml-auto">
            <IconRefresh size={14} /> {loading ? 'Yükleniyor…' : 'Yenile'}
          </button>
          {overview && (
            <span className="text-[11px] text-ink-subtle">
              {fullLabel(overview.startAt)} — {fullLabel(overview.endAt)}
            </span>
          )}
        </div>

        <div className="modal-panel scroll-thin">
          {error && (
            <p className="mb-4 flex items-center gap-2 rounded-lg border border-danger/25 bg-danger-soft px-3 py-2 text-xs text-danger">
              <IconAlert size={14} /> {error}
            </p>
          )}

          {totals && (
            <div className="grid grid-cols-2 gap-2 lg:grid-cols-4">
              <div className="knowledge-stat">
                <strong>{formatNumber(totals.successRate, 2)}%</strong>
                <span>Başarı oranı</span>
              </div>
              <div className="knowledge-stat">
                <strong>{totals.attempted.toLocaleString('tr-TR')}</strong>
                <span>Deneme</span>
              </div>
              <div className="knowledge-stat">
                <strong>{totals.failed.toLocaleString('tr-TR')}</strong>
                <span>Başarısız · {totals.retries.toLocaleString('tr-TR')} yeniden deneme</span>
              </div>
              <div className="knowledge-stat">
                <strong>{formatNumber(totals.averageDeliverySeconds, 2)} sn</strong>
                <span>Ort. teslim · p95 {formatNumber(totals.p95DeliverySeconds, 1)} sn</span>
              </div>
            </div>
          )}

          {degradedProvider && (
            <p className="mt-4 flex items-center gap-2 rounded-xl border border-alert/25 bg-alert-soft px-3 py-2 text-xs text-alert">
              <IconAlert size={14} />
              {degradedProvider.provider} bozulmuş durumda — timeout oranı{' '}
              {formatNumber(degradedProvider.timeoutRate, 2)}, devre kesici{' '}
              {degradedProvider.circuitBreakerState}.
            </p>
          )}

          <section className="mt-6">
            <div className="section-heading"><span>Dakikalık başarı oranı</span><span>{overview?.series.length ?? 0} nokta</span></div>
            <div className="visualization-card">
              <ResponsiveContainer width="100%" height={220}>
                <AreaChart data={overview?.series ?? []} margin={{ top: 6, right: 8, left: -18, bottom: 0 }}>
                  <CartesianGrid stroke="var(--color-line)" vertical={false} />
                  <XAxis
                    dataKey="bucketAt"
                    tickFormatter={clockLabel}
                    tick={{ fontSize: 10, fill: 'var(--color-ink-subtle)' }}
                    minTickGap={28}
                  />
                  <YAxis
                    domain={[0, 100]}
                    tick={{ fontSize: 10, fill: 'var(--color-ink-subtle)' }}
                    unit="%"
                  />
                  <Tooltip
                    labelFormatter={(value) => fullLabel(String(value))}
                    formatter={(value: number) => [`${formatNumber(value, 2)}%`, 'Başarı oranı']}
                  />
                  <Area
                    type="monotone"
                    dataKey="successRate"
                    stroke="#1D4ED8"
                    fill="#8FAEEA"
                    fillOpacity={0.35}
                    strokeWidth={2}
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </section>

          <div className="mt-6 grid gap-4 lg:grid-cols-2">
            <section>
              <div className="section-heading"><span>Hata kodu dağılımı</span></div>
              <div className="visualization-card">
                <ResponsiveContainer width="100%" height={200}>
                  <BarChart data={overview?.errors ?? []} margin={{ top: 6, right: 8, left: -18, bottom: 0 }}>
                    <CartesianGrid stroke="var(--color-line)" vertical={false} />
                    <XAxis dataKey="errorCode" tick={{ fontSize: 9, fill: 'var(--color-ink-subtle)' }} interval={0} angle={-12} textAnchor="end" height={44} />
                    <YAxis tick={{ fontSize: 10, fill: 'var(--color-ink-subtle)' }} />
                    <Tooltip formatter={(value: number, _name, entry) => [
                      `${value.toLocaleString('tr-TR')} hata (%${formatNumber((entry?.payload?.share ?? 0) as number, 2)})`,
                      'Adet',
                    ]} />
                    <Bar dataKey="failures" radius={[4, 4, 0, 0]}>
                      {(overview?.errors ?? []).map((row, index) => (
                        <Cell key={row.errorCode} fill={ERROR_COLORS[index % ERROR_COLORS.length]} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </section>

            <section>
              <div className="section-heading"><span>Operatör kırılımı</span></div>
              <div className="overflow-x-auto rounded-xl border border-line bg-surface">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Operatör</th><th>Deneme</th><th>Başarı</th><th>Durum</th><th>Timeout</th><th>Bağlantı</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(overview?.providers ?? []).map((provider) => (
                      <tr key={provider.provider}>
                        <td className="font-medium text-ink">{provider.provider}</td>
                        <td>{provider.attempted.toLocaleString('tr-TR')}</td>
                        <td>{formatNumber(provider.successRate, 2)}%</td>
                        <td>
                          <span className={provider.status === 'DEGRADED' ? 'pill pill-alert' : 'pill pill-ok'}>
                            {provider.status === 'DEGRADED' ? 'Bozulmuş' : 'Sağlıklı'}
                          </span>
                        </td>
                        <td>{formatNumber(provider.timeoutRate, 2)}</td>
                        <td>{provider.activeConnections}/{provider.maxConnections}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          </div>

          <div className="mt-6 grid gap-4 lg:grid-cols-2">
            <section>
              <div className="section-heading"><span>Kuyruk sağlığı</span></div>
              <div className="visualization-card text-xs text-ink-muted">
                {overview?.queue ? (
                  <ul className="space-y-1.5">
                    <li className="flex items-center gap-2">
                      <IconShield size={13} />
                      Durum: <strong className="text-ink">{overview.queue.status}</strong> ·{' '}
                      {overview.queue.processingRateStatus}
                    </li>
                    <li>Bekleyen mesaj: <strong className="text-ink">{overview.queue.pendingMessages}</strong> / eşik {overview.queue.normalPendingThreshold}</li>
                    <li>En eski mesaj yaşı: {overview.queue.oldestMessageAgeSeconds} sn</li>
                    <li>Tüketici: {overview.queue.activeConsumers}/{overview.queue.expectedConsumers} · DLQ {overview.queue.deadLetterCount}</li>
                    <li className="text-ink-subtle">Ölçüm: {fullLabel(overview.queue.bucketAt)}</li>
                  </ul>
                ) : (
                  <p className="empty-note">Bu aralıkta kuyruk ölçümü yok.</p>
                )}
              </div>
            </section>

            <section>
              <div className="section-heading"><span>Değişiklik ve gözlemler</span></div>
              <div className="visualization-card">
                {overview && overview.changes.length > 0 ? (
                  <ol className="space-y-3">
                    {overview.changes.map((change) => (
                      <li key={change.changeId} className="flex gap-3 text-xs">
                        <span className="change-mark"><IconLayers size={13} /></span>
                        <span className="min-w-0">
                          <strong className="text-ink">{change.description}</strong>
                          <span className="mt-0.5 block text-ink-subtle">
                            {change.changeId} · {change.type} · {change.component} ·{' '}
                            {fullLabel(change.occurredAt)}
                            {change.version ? ` · ${change.version}` : ''}
                          </span>
                        </span>
                      </li>
                    ))}
                  </ol>
                ) : (
                  <p className="empty-note">Bu aralıkta değişiklik kaydı yok.</p>
                )}
              </div>
            </section>
          </div>

          <section className="mt-6">
            <div className="section-heading">
              <span>Ham dakikalık kayıtlar</span>
              <span>{samples.length} satır</span>
            </div>
            <div className="max-h-[380px] overflow-auto rounded-xl border border-line bg-surface scroll-thin">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Dakika</th><th>Operatör</th><th>Deneme</th><th>Teslim</th><th>Hata</th>
                    <th>Retry</th><th>Ort. sn</th><th>p95 sn</th><th>Timeout</th><th>Durum</th><th>Hata kırılımı</th>
                  </tr>
                </thead>
                <tbody>
                  {samples.map((row) => (
                    <tr key={`${row.bucketAt}-${row.provider}`}>
                      <td className="font-mono text-[10.5px]">{fullLabel(row.bucketAt)}</td>
                      <td>{row.provider}</td>
                      <td>{row.attempted}</td>
                      <td>{row.delivered}</td>
                      <td className={row.failed > row.attempted * 0.1 ? 'text-danger' : ''}>{row.failed}</td>
                      <td>{row.retries}</td>
                      <td>{formatNumber(row.averageDeliverySeconds, 1)}</td>
                      <td>{formatNumber(row.p95DeliverySeconds, 1)}</td>
                      <td>{formatNumber(row.timeoutRate, 3)}</td>
                      <td>
                        <span className={row.providerStatus === 'DEGRADED' ? 'pill pill-alert' : 'pill pill-ok'}>
                          {row.providerStatus === 'DEGRADED' ? 'Bozulmuş' : 'Sağlıklı'}
                        </span>
                      </td>
                      <td className="text-[10.5px] text-ink-subtle">{row.errors ?? '—'}</td>
                    </tr>
                  ))}
                  {samples.length === 0 && !loading && (
                    <tr>
                      <td colSpan={11} className="py-6 text-center text-ink-subtle">
                        <IconActivity size={14} /> Bu aralıkta kayıt yok.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </div>
    </div>
  )
}
