import { useState } from 'react'
import { getKnowledgeDocument } from '../api/client'
import { toUserMessage } from '../lib/errors'
import { formatDateTime, formatNumber } from '../lib/labels'
import type { ChatTurn, Evidence, KnowledgeDocumentDetail, KnowledgeReference } from '../api/types'
import { IconBook, IconChevronDown, IconDatabase, IconSparkles } from './icons'

const TOOL_LABEL_TR: Record<string, string> = {
  getOtpMetrics: 'OTP teslim metrikleri',
  getErrorDistribution: 'Hata kodu dağılımı',
  getQueueHealth: 'Kuyruk sağlığı',
  getProviderHealth: 'Operatör sağlığı',
  getRecentChanges: 'Değişiklik kayıtları',
}

const TOOL_TABLE_TR: Record<string, string> = {
  getOtpMetrics: 'otp_delivery_sample',
  getErrorDistribution: 'otp_error_sample',
  getQueueHealth: 'queue_health_sample',
  getProviderHealth: 'provider_health_sample',
  getRecentChanges: 'change_event',
}

function groupByTool(evidence: Evidence[]): [string, Evidence[]][] {
  const groups = new Map<string, Evidence[]>()
  for (const item of evidence) {
    const key = item.sourceReference || 'bilinmeyen araç'
    groups.set(key, [...(groups.get(key) ?? []), item])
  }
  return [...groups.entries()]
}

function KnowledgeChunk({ reference }: { reference: KnowledgeReference }) {
  const [open, setOpen] = useState(false)
  const [detail, setDetail] = useState<KnowledgeDocumentDetail | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function toggle() {
    const next = !open
    setOpen(next)
    if (!next || detail || !reference.version) return
    try {
      setDetail(await getKnowledgeDocument(reference.documentId, reference.version))
    } catch (failure) {
      setError(toUserMessage(failure))
    }
  }

  const chunk = detail?.chunks.find((item) => item.chunkId === reference.chunkId)

  return (
    <li className="source-item">
      <button type="button" onClick={() => void toggle()} className="source-item-head">
        <span className={`source-caret ${open ? 'is-open' : ''}`}><IconChevronDown size={12} /></span>
        <span className="min-w-0 flex-1">
          <strong>{reference.title ?? reference.documentId}</strong>
          <small>
            {reference.documentId}
            {reference.version ? ` · v${reference.version}` : ''}
            {reference.chunkId ? ` · ${reference.chunkId}` : ''}
          </small>
        </span>
        {reference.similarityScore != null && (
          <span className="source-score">benzerlik {formatNumber(reference.similarityScore, 2)}</span>
        )}
      </button>
      {open && (
        <div className="source-item-body">
          {error && <p className="text-danger">{error}</p>}
          {!error && !detail && <p className="text-ink-subtle">Chunk yükleniyor…</p>}
          {chunk && (
            <>
              <p className="mb-1 text-ink-subtle">
                {chunk.sectionTitle || 'Genel bölüm'} · {chunk.tokenCount} token · {chunk.embeddingModel}
              </p>
              <p className="source-chunk-text">{chunk.content}</p>
            </>
          )}
          {detail && !chunk && (
            <p className="text-ink-subtle">
              Bu chunk artık belgede yok; belge güncellenmiş olabilir.
            </p>
          )}
        </div>
      )}
    </li>
  )
}

function DatabaseTool({ tool, evidence }: { tool: string; evidence: Evidence[] }) {
  const [open, setOpen] = useState(false)
  return (
    <li className="source-item">
      <button type="button" onClick={() => setOpen((value) => !value)} className="source-item-head">
        <span className={`source-caret ${open ? 'is-open' : ''}`}><IconChevronDown size={12} /></span>
        <span className="min-w-0 flex-1">
          <strong>{TOOL_LABEL_TR[tool] ?? tool}</strong>
          <small>{tool}{TOOL_TABLE_TR[tool] ? ` · ${TOOL_TABLE_TR[tool]}` : ''}</small>
        </span>
        <span className="source-score">{evidence.length} kanıt</span>
      </button>
      {open && (
        <div className="source-item-body">
          <table className="data-table">
            <thead>
              <tr><th>Kanıt</th><th>Okunan değer</th><th>Zaman</th></tr>
            </thead>
            <tbody>
              {evidence.map((item) => (
                <tr key={item.id}>
                  <td className="font-mono text-[10px]">{item.id}</td>
                  <td className="whitespace-normal">
                    {item.observation}
                    {item.metricValue != null && (
                      <span className="ml-1 text-ink-subtle">
                        ({item.metricName}: {formatNumber(item.metricValue, 2)} {item.metricUnit})
                      </span>
                    )}
                  </td>
                  <td>{formatDateTime(item.observedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </li>
  )
}

/**
 * Every assistant turn says where it got its facts: which operational tables were queried and which
 * knowledge chunks were retrieved — or, for a tool-free reply, that nothing was read at all. Without
 * this the reader cannot tell a grounded answer from a plausible one.
 */
export function SourceTrail({ turn }: { turn: ChatTurn }) {
  const [panel, setPanel] = useState<'database' | 'knowledge' | null>(null)

  if (turn.kind === 'pending' || turn.kind === 'error') return null

  if (turn.kind !== 'investigation') {
    return (
      <div className="source-bar">
        <span className="source-chip is-quiet" title="Bu yanıt için hiçbir araç veya bilgi tabanı sorgusu çalışmadı">
          <IconSparkles size={12} /> Kaynak yok · toolsuz sohbet
        </span>
      </div>
    )
  }

  const evidence = turn.investigation.evidence ?? []
  const knowledge = turn.investigation.knowledgeReferences ?? []
  const toolGroups = groupByTool(evidence)

  return (
    <div className="source-trail">
      <div className="source-bar">
        <button
          type="button"
          onClick={() => setPanel(panel === 'database' ? null : 'database')}
          className={`source-chip ${panel === 'database' ? 'is-active' : ''} ${toolGroups.length === 0 ? 'is-quiet' : ''}`}
          aria-expanded={panel === 'database'}
        >
          <IconDatabase size={12} />
          Veritabanı · {toolGroups.length} sorgu, {evidence.length} kanıt
        </button>
        <button
          type="button"
          onClick={() => setPanel(panel === 'knowledge' ? null : 'knowledge')}
          className={`source-chip ${panel === 'knowledge' ? 'is-active' : ''} ${knowledge.length === 0 ? 'is-quiet' : ''}`}
          aria-expanded={panel === 'knowledge'}
        >
          <IconBook size={12} />
          {knowledge.length === 0 ? 'RAG · eşleşme yok' : `RAG · ${knowledge.length} belge parçası`}
        </button>
      </div>

      {panel === 'database' && (
        <div className="source-panel animate-fade">
          <p className="source-panel-note">
            Aşağıdaki satırlar agent'ın bu yanıt için okuduğu operasyonel kayıtlardır. Aynı sayıları
            Veri gezgininde (Ctrl+D) aynı zaman aralığı için doğrulayabilirsiniz.
          </p>
          {toolGroups.length === 0 ? (
            <p className="empty-note">Bu yanıt için veritabanı sorgusu çalışmadı.</p>
          ) : (
            <ul className="source-list">
              {toolGroups.map(([tool, items]) => (
                <DatabaseTool key={tool} tool={tool} evidence={items} />
              ))}
            </ul>
          )}
        </div>
      )}

      {panel === 'knowledge' && (
        <div className="source-panel animate-fade">
          <p className="source-panel-note">
            Bilgi tabanından getirilen chunk'lar. Başlığa tıklayınca modele gerçekte hangi metnin
            verildiğini görürsünüz.
          </p>
          {knowledge.length === 0 ? (
            <p className="empty-note">
              Bu yanıtta bilgi tabanından hiçbir belge kullanılmadı — sonuç yalnızca canlı operasyon
              verisine dayanıyor.
            </p>
          ) : (
            <ul className="source-list">
              {knowledge.map((reference, index) => (
                <KnowledgeChunk key={`${reference.documentId}-${reference.chunkId ?? index}`} reference={reference} />
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
