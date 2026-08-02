import { useEffect, useState } from 'react'
import {
  getKnowledgeDocument,
  getModelCatalog,
  listKnowledgeDocuments,
  previewKnowledgeSearch,
  uploadKnowledgeDocument,
} from '../api/client'
import { toUserMessage } from '../lib/errors'
import { DOCUMENT_TYPE_LABEL_TR, MODE_LABEL_TR, UI_TEXT, formatNumber } from '../lib/labels'
import type {
  KnowledgeDocumentDetail,
  KnowledgeDocumentSummary,
  KnowledgeSearchResult,
  ModelOption,
} from '../api/types'

const DOCUMENT_TYPES = Object.keys(DOCUMENT_TYPE_LABEL_TR)

interface Props {
  modelId: string | null
  onModelChange: (modelId: string) => void
  mode: 'quick' | 'thorough'
  onModeChange: (mode: 'quick' | 'thorough') => void
  onClose: () => void
  embedded?: boolean
  interactionMode?: 'AUTO' | 'CHAT' | 'INVESTIGATION'
}

export function SettingsPanel({
  modelId,
  onModelChange,
  mode,
  onModeChange,
  onClose,
  embedded = false,
  interactionMode = 'AUTO',
}: Props) {
  const [models, setModels] = useState<ModelOption[]>([])
  const [documents, setDocuments] = useState<KnowledgeDocumentSummary[]>([])
  const [selectedDocument, setSelectedDocument] = useState<KnowledgeDocumentDetail | null>(null)
  const [searchResults, setSearchResults] = useState<KnowledgeSearchResult[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [uploadError, setUploadError] = useState<string | null>(null)
  const [uploadOk, setUploadOk] = useState(false)
  const [searching, setSearching] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [showUpload, setShowUpload] = useState(false)
  const [title, setTitle] = useState('')
  const [documentType, setDocumentType] = useState(DOCUMENT_TYPES[0])
  const [provider, setProvider] = useState('')
  const [tags, setTags] = useState('')
  const [language, setLanguage] = useState('tr')
  const [effectiveFrom, setEffectiveFrom] = useState('')
  const [effectiveTo, setEffectiveTo] = useState('')
  const [content, setContent] = useState('')

  useEffect(() => {
    getModelCatalog()
      .then((catalog) => {
        setModels(catalog.options)
        if (modelId === null && catalog.defaultModelId) onModelChange(catalog.defaultModelId)
      })
      .catch((error) => setLoadError(toUserMessage(error)))
    refreshDocuments()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function refreshDocuments() {
    listKnowledgeDocuments()
      .then((items) => {
        setDocuments(items)
        setLoadError(null)
      })
      .catch((error) => {
        setDocuments([])
        setLoadError(toUserMessage(error))
      })
  }

  async function openDocument(document: KnowledgeDocumentSummary) {
    try {
      setSelectedDocument(await getKnowledgeDocument(document.documentId, document.version))
    } catch (error) {
      setLoadError(toUserMessage(error))
    }
  }

  async function handleSearch(event: React.FormEvent) {
    event.preventDefault()
    if (searchQuery.trim().length < 3) return
    setSearching(true)
    setLoadError(null)
    try {
      setSearchResults(await previewKnowledgeSearch(searchQuery.trim()))
    } catch (error) {
      setLoadError(toUserMessage(error))
    } finally {
      setSearching(false)
    }
  }

  async function handleUpload(event: React.FormEvent) {
    event.preventDefault()
    setUploadError(null)
    setUploadOk(false)
    try {
      await uploadKnowledgeDocument({
        title,
        documentType,
        provider: provider || undefined,
        tags: tags.split(',').map((tag) => tag.trim()).filter(Boolean),
        language: language || 'tr',
        effectiveFrom,
        effectiveTo: effectiveTo || undefined,
        content,
      })
      setUploadOk(true)
      setTitle('')
      setProvider('')
      setTags('')
      setContent('')
      setShowUpload(false)
      refreshDocuments()
    } catch (error) {
      setUploadError(toUserMessage(error))
    }
  }

  const panel = (
    <aside className={embedded ? 'settings-rail hidden xl:flex' : 'settings-drawer'} aria-label="Bilgi tabanı ve ayarlar">
      <div className="flex items-start justify-between border-b border-line px-5 py-5">
        <div>
          <p className="eyebrow">CONTEXT CONTROL</p>
          <h2 className="mt-1 font-display text-lg font-semibold text-ink">Bilgi tabanı</h2>
        </div>
        {!embedded && (
          <button type="button" onClick={onClose} className="icon-button" aria-label={UI_TEXT.closeSettings}>×</button>
        )}
      </div>

      <div className="border-b border-line px-5 py-4">
        <label htmlFor={embedded ? 'rail-model' : 'drawer-model'} className="field-label">Varsayılan model</label>
        <select
          id={embedded ? 'rail-model' : 'drawer-model'}
          value={modelId ?? ''}
          onChange={(event) => onModelChange(event.target.value)}
          className="field-control"
        >
          {models.map((model) => (
            <option key={model.id} value={model.id}>{model.id}</option>
          ))}
        </select>
        {interactionMode !== 'CHAT' && <div className="mt-3 flex gap-2">
          {(['quick', 'thorough'] as const).map((value) => (
            <label key={value} className={`mode-option ${mode === value ? 'mode-option-active' : ''}`}>
              <input
                type="radio"
                name={embedded ? 'rail-mode' : 'drawer-mode'}
                checked={mode === value}
                onChange={() => onModeChange(value)}
                className="sr-only"
              />
              {MODE_LABEL_TR[value]}
            </label>
          ))}
        </div>}
      </div>

      <div className="flex-1 overflow-y-auto px-5 py-5">
        {loadError && <p className="mb-4 border-l-2 border-danger bg-danger-soft px-3 py-2 text-xs text-danger">{loadError}</p>}

        <div className="mb-5 grid grid-cols-2 gap-2">
          <div className="knowledge-stat"><strong>{documents.length}</strong><span>Belge</span></div>
          <div className="knowledge-stat"><strong>{documents.reduce((total, document) => total + (document.chunkCount ?? 0), 0)}</strong><span>Chunk</span></div>
        </div>

        <form onSubmit={handleSearch} className="mb-5">
          <label htmlFor={embedded ? 'rail-rag-query' : 'drawer-rag-query'} className="field-label">RAG retrieval testi</label>
          <div className="flex gap-2">
            <input
              id={embedded ? 'rail-rag-query' : 'drawer-rag-query'}
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="Örn. connection pool timeout"
              className="field-control min-w-0"
            />
            <button type="submit" disabled={searching || searchQuery.trim().length < 3} className="secondary-button px-3">{searching ? '…' : 'Ara'}</button>
          </div>
        </form>

        {searchResults.length > 0 && (
          <section className="mb-6">
            <div className="section-heading"><span>Eşleşmeler</span><span>{searchResults.length}/5</span></div>
            <div className="space-y-2">
              {searchResults.map((result) => (
                <div key={result.chunkId} className="search-result-card">
                  <div className="flex items-start justify-between gap-2">
                    <strong>{result.title}</strong>
                    <span>{formatNumber(result.similarityScore, 2)}</span>
                  </div>
                  <p>{result.contentExcerpt}</p>
                  <code>{result.chunkId}</code>
                </div>
              ))}
            </div>
          </section>
        )}

        <section>
          <div className="section-heading">
            <span>İndekslenen belgeler</span>
            <button type="button" onClick={() => setShowUpload((value) => !value)}>+ Belge ekle</button>
          </div>
          {documents.length === 0 && <p className="empty-note">{UI_TEXT.knowledgeListEmpty}</p>}
          <div className="space-y-2">
            {documents.map((document) => (
              <button
                key={`${document.documentId}-${document.version}`}
                type="button"
                onClick={() => openDocument(document)}
                className="document-row"
              >
                <span className="document-type-mark">{document.documentType.slice(0, 2)}</span>
                <span className="min-w-0 flex-1">
                  <strong>{document.title}</strong>
                  <small>{DOCUMENT_TYPE_LABEL_TR[document.documentType] ?? document.documentType} · v{document.version}</small>
                </span>
                <span aria-hidden="true">›</span>
              </button>
            ))}
          </div>
        </section>

        {showUpload && (
          <form onSubmit={handleUpload} className="upload-form">
            <div className="section-heading"><span>{UI_TEXT.uploadTitle}</span><button type="button" onClick={() => setShowUpload(false)}>Kapat</button></div>
            <input required placeholder={UI_TEXT.uploadTitleField} value={title} onChange={(event) => setTitle(event.target.value)} className="field-control" />
            <div className="grid grid-cols-2 gap-2">
              <select value={documentType} onChange={(event) => setDocumentType(event.target.value)} className="field-control">
                {DOCUMENT_TYPES.map((type) => <option key={type} value={type}>{DOCUMENT_TYPE_LABEL_TR[type]}</option>)}
              </select>
              <input placeholder={UI_TEXT.uploadLanguageField} value={language} onChange={(event) => setLanguage(event.target.value)} className="field-control" />
            </div>
            <input placeholder={UI_TEXT.uploadProviderField} value={provider} onChange={(event) => setProvider(event.target.value)} className="field-control" />
            <input placeholder={UI_TEXT.uploadTagsField} value={tags} onChange={(event) => setTags(event.target.value)} className="field-control" />
            <div className="grid grid-cols-2 gap-2">
              <label className="field-label">Başlangıç<input required type="date" value={effectiveFrom} onChange={(event) => setEffectiveFrom(event.target.value)} className="field-control mt-1" /></label>
              <label className="field-label">Bitiş<input type="date" value={effectiveTo} onChange={(event) => setEffectiveTo(event.target.value)} className="field-control mt-1" /></label>
            </div>
            <textarea required placeholder={UI_TEXT.uploadContentField} value={content} onChange={(event) => setContent(event.target.value)} rows={6} className="field-control resize-y" />
            <button type="submit" className="primary-button w-full">{UI_TEXT.uploadSubmit}</button>
            {uploadError && <p className="text-xs text-danger">{uploadError}</p>}
          </form>
        )}
        {uploadOk && <p className="mt-3 text-xs text-confirm">{UI_TEXT.uploadSuccess}</p>}
      </div>

      {selectedDocument && (
        <div className="document-detail-overlay">
          <div className="flex items-start justify-between gap-3 border-b border-line p-5">
            <div><p className="eyebrow">{selectedDocument.documentId} · v{selectedDocument.version}</p><h3 className="mt-1 font-display font-semibold">{selectedDocument.title}</h3></div>
            <button type="button" onClick={() => setSelectedDocument(null)} className="icon-button" aria-label="Belge detayını kapat">×</button>
          </div>
          <div className="flex-1 overflow-y-auto p-5">
            <div className="metadata-grid">
              <span>Tür<strong>{DOCUMENT_TYPE_LABEL_TR[selectedDocument.documentType] ?? selectedDocument.documentType}</strong></span>
              <span>Provider<strong>{selectedDocument.provider || 'Genel'}</strong></span>
              <span>Dil<strong>{selectedDocument.language || 'tr'}</strong></span>
              <span>Chunk<strong>{selectedDocument.chunks.length}</strong></span>
            </div>
            {selectedDocument.tags && selectedDocument.tags.length > 0 && <div className="mt-4 flex flex-wrap gap-1">{selectedDocument.tags.map((tag) => <span key={tag} className="tag">#{tag}</span>)}</div>}
            <h4 className="detail-title">Sanitize edilmiş içerik</h4>
            <pre className="document-content">{selectedDocument.sanitizedContent}</pre>
            <h4 className="detail-title">Chunk ayrıntıları</h4>
            <div className="space-y-3">{selectedDocument.chunks.map((chunk) => <details key={chunk.chunkId} className="chunk-card"><summary><span>{chunk.sectionTitle || 'Genel bölüm'}</span><code>{chunk.tokenCount} token</code></summary><p>{chunk.content}</p><code>{chunk.chunkId} · {chunk.embeddingModel}</code></details>)}</div>
          </div>
        </div>
      )}
    </aside>
  )

  if (embedded) return panel
  return <div className="settings-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>{panel}</div>
}
