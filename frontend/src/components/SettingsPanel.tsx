import { useEffect, useRef, useState } from 'react'
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
import {
  IconAlert,
  IconBook,
  IconCheck,
  IconChevronRight,
  IconClose,
  IconKeyboard,
  IconLayers,
  IconRefresh,
  IconSearch,
  IconSparkles,
  IconUpload,
} from './icons'

const DOCUMENT_TYPES = Object.keys(DOCUMENT_TYPE_LABEL_TR)

const SECTIONS = [
  { id: 'section-model', label: 'Model ve mod', icon: IconSparkles },
  { id: 'section-knowledge', label: UI_TEXT.knowledgeSectionTitle, icon: IconBook },
  { id: 'section-shortcuts', label: 'Kısayollar', icon: IconKeyboard },
] as const

const SHORTCUTS: [string, string][] = [
  ['Ctrl + K', 'Sohbetlerde ara'],
  ['Ctrl + Shift + O', 'Yeni sohbet'],
  ['Ctrl + B', 'Yan paneli aç/kapat'],
  ['Ctrl + ,', 'Ayarları aç'],
  ['Enter', 'Mesajı gönder'],
  ['Shift + Enter', 'Yeni satır'],
  ['Esc', 'Pencereyi kapat'],
]

interface Props {
  modelId: string | null
  onModelChange: (modelId: string) => void
  mode: 'quick' | 'thorough'
  onModeChange: (mode: 'quick' | 'thorough') => void
  onClose: () => void
  interactionMode?: 'AUTO' | 'CHAT' | 'INVESTIGATION'
}

export function SettingsPanel({
  modelId,
  onModelChange,
  mode,
  onModeChange,
  onClose,
  interactionMode = 'AUTO',
}: Props) {
  const [models, setModels] = useState<ModelOption[]>([])
  const [documents, setDocuments] = useState<KnowledgeDocumentSummary[]>([])
  const [selectedDocument, setSelectedDocument] = useState<KnowledgeDocumentDetail | null>(null)
  const [searchResults, setSearchResults] = useState<KnowledgeSearchResult[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [uploadError, setUploadError] = useState<string | null>(null)
  const [uploadOk, setUploadOk] = useState(false)
  const [uploadedDetail, setUploadedDetail] = useState<KnowledgeDocumentDetail | null>(null)
  const [uploading, setUploading] = useState(false)
  const [searching, setSearching] = useState(false)
  const [searched, setSearched] = useState(false)
  const [refreshing, setRefreshing] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [showUpload, setShowUpload] = useState(false)
  const [activeSection, setActiveSection] = useState<string>(SECTIONS[0].id)
  const [title, setTitle] = useState('')
  const [documentType, setDocumentType] = useState(DOCUMENT_TYPES[0])
  const [provider, setProvider] = useState('')
  const [tags, setTags] = useState('')
  const [language, setLanguage] = useState('tr')
  const [effectiveFrom, setEffectiveFrom] = useState('')
  const [effectiveTo, setEffectiveTo] = useState('')
  const [content, setContent] = useState('')
  const panelRef = useRef<HTMLDivElement>(null)

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

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.key !== 'Escape') return
      if (selectedDocument) setSelectedDocument(null)
      else onClose()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [onClose, selectedDocument])

  function refreshDocuments() {
    setRefreshing(true)
    listKnowledgeDocuments()
      .then((items) => {
        setDocuments(items)
        setLoadError(null)
      })
      .catch((error) => {
        setDocuments([])
        setLoadError(toUserMessage(error))
      })
      .finally(() => setRefreshing(false))
  }

  function scrollToSection(id: string) {
    setActiveSection(id)
    const target = panelRef.current?.querySelector(`#${id}`)
    target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
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
      setSearched(true)
    } catch (error) {
      setSearchResults([])
      setSearched(true)
      setLoadError(toUserMessage(error))
    } finally {
      setSearching(false)
    }
  }

  async function handleUpload(event: React.FormEvent) {
    event.preventDefault()
    setUploadError(null)
    setUploadOk(false)
    setUploading(true)
    try {
      const created = await uploadKnowledgeDocument({
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
      // Show what actually happened to the document: how it was chunked and with which embedding
      // model. "Yüklendi" alone leaves the operator guessing whether RAG can even use it.
      try {
        setUploadedDetail(await getKnowledgeDocument(created.documentId, created.version))
      } catch {
        setUploadedDetail(null)
      }
      setTitle('')
      setProvider('')
      setTags('')
      setContent('')
      setShowUpload(false)
      refreshDocuments()
    } catch (error) {
      setUploadError(toUserMessage(error))
    } finally {
      setUploading(false)
    }
  }

  const chunkTotal = documents.reduce((total, document) => total + (document.chunkCount ?? 0), 0)

  return (
    <div
      className="modal-backdrop"
      onMouseDown={(event) => event.target === event.currentTarget && onClose()}
    >
      <div className="modal-shell" role="dialog" aria-modal="true" aria-label="Bilgi tabanı ve ayarlar">
        <div className="modal-head">
          <div>
            <p className="eyebrow">CONTEXT CONTROL</p>
            <h2 className="mt-1 font-display text-lg font-semibold tracking-tight text-ink">
              Çalışma alanı ayarları
            </h2>
            <p className="mt-1 text-xs text-ink-muted">
              Analiz modeli, inceleme derinliği ve agent'ın eriştiği bilgi tabanı.
            </p>
          </div>
          <button type="button" onClick={onClose} className="icon-button" aria-label={UI_TEXT.closeSettings}>
            <IconClose size={18} />
          </button>
        </div>

        <div className="modal-body">
          <nav className="modal-nav scroll-thin" aria-label="Ayar bölümleri">
            <div className="flex gap-1 sm:block sm:space-y-1">
              {SECTIONS.map((section) => (
                <button
                  key={section.id}
                  type="button"
                  onClick={() => scrollToSection(section.id)}
                  className={`modal-tab ${activeSection === section.id ? 'is-active' : ''}`}
                >
                  <section.icon size={15} />
                  {section.label}
                </button>
              ))}
            </div>
          </nav>

          <div ref={panelRef} className="modal-panel scroll-thin">
            {loadError && (
              <p className="mb-4 flex items-center gap-2 rounded-lg border border-danger/25 bg-danger-soft px-3 py-2 text-xs text-danger">
                <IconAlert size={14} /> {loadError}
              </p>
            )}

            <section id="section-model" className="scroll-mt-4">
              <div className="section-heading"><span>Analiz modeli</span><span>{models.length} model</span></div>
              <div className="grid gap-2 sm:grid-cols-2">
                {models.map((model) => (
                  <button
                    key={model.id}
                    type="button"
                    onClick={() => onModelChange(model.id)}
                    className={`model-option ${modelId === model.id ? 'is-active' : ''}`}
                    aria-pressed={modelId === model.id}
                  >
                    <span className={modelId === model.id ? 'text-signal' : 'text-ink-subtle'}>
                      {modelId === model.id ? <IconCheck size={15} /> : <IconLayers size={15} />}
                    </span>
                    <span className="min-w-0 flex-1">
                      <strong>{model.label}</strong>
                      <small>{model.description || model.provider}</small>
                      <code>{model.id}</code>
                    </span>
                  </button>
                ))}
                {models.length === 0 && <p className="empty-note sm:col-span-2">Model kataloğu yüklenemedi.</p>}
              </div>

              {interactionMode !== 'CHAT' && (
                <div className="mt-5">
                  <div className="section-heading"><span>İnceleme derinliği</span></div>
                  <div className="flex gap-2">
                    {(['quick', 'thorough'] as const).map((value) => (
                      <label key={value} className={`mode-option ${mode === value ? 'mode-option-active' : ''}`}>
                        <input
                          type="radio"
                          name="settings-mode"
                          checked={mode === value}
                          onChange={() => onModeChange(value)}
                          className="sr-only"
                        />
                        {MODE_LABEL_TR[value]}
                      </label>
                    ))}
                  </div>
                  <p className="mt-2 text-[11px] leading-5 text-ink-subtle">
                    Hızlı mod daha az araç çağrısı yapar; detaylı mod RAG dahil tam kanıt toplar.
                  </p>
                </div>
              )}
            </section>

            <section id="section-knowledge" className="mt-8 scroll-mt-4">
              <div className="mb-4 grid grid-cols-2 gap-2 sm:grid-cols-3">
                <div className="knowledge-stat"><strong>{documents.length}</strong><span>Belge</span></div>
                <div className="knowledge-stat"><strong>{chunkTotal}</strong><span>Chunk</span></div>
                <div className="knowledge-stat"><strong>{searchResults.length}</strong><span>Son eşleşme</span></div>
              </div>

              <form onSubmit={handleSearch} className="mb-5">
                <label htmlFor="rag-query" className="field-label">RAG retrieval testi</label>
                <div className="flex gap-2">
                  <input
                    id="rag-query"
                    value={searchQuery}
                    onChange={(event) => setSearchQuery(event.target.value)}
                    placeholder="Örn. connection pool timeout"
                    className="field-control min-w-0"
                  />
                  <button type="submit" disabled={searching || searchQuery.trim().length < 3} className="secondary-button">
                    <IconSearch size={14} />
                    {searching ? '…' : 'Ara'}
                  </button>
                </div>
                <p className="mt-1.5 text-[11px] text-ink-subtle">
                  En az 3 karakter. Anlamsal arama yapılır — birebir kelime eşleşmesi gerekmez,
                  sonuçlar benzerlik skoruyla sıralanır.
                </p>
              </form>

              {searched && searchResults.length === 0 && !searching && (
                <p className="empty-note mb-6">
                  Bu sorgu için eşik üstünde eşleşme bulunamadı. Daha fazla bağlam içeren bir ifade deneyin.
                </p>
              )}

              {searchResults.length > 0 && (
                <div className="mb-6">
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
                </div>
              )}

              <div className="section-heading">
                <span>İndekslenen belgeler</span>
                <span className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={refreshDocuments}
                    disabled={refreshing}
                    className="inline-flex items-center gap-1 disabled:opacity-50"
                  >
                    <IconRefresh size={12} /> {refreshing ? 'Yenileniyor…' : 'Yenile'}
                  </button>
                  <button type="button" onClick={() => setShowUpload((value) => !value)}>+ Belge ekle</button>
                </span>
              </div>
              {documents.length === 0 && <p className="empty-note">{UI_TEXT.knowledgeListEmpty}</p>}
              <div className="grid gap-2 sm:grid-cols-2">
                {documents.map((document) => (
                  <button
                    key={`${document.documentId}-${document.version}`}
                    type="button"
                    onClick={() => openDocument(document)}
                    className="document-row"
                  >
                    <span className="document-type-mark"><IconBook size={15} /></span>
                    <span className="min-w-0 flex-1">
                      <strong>{document.title}</strong>
                      <small>{DOCUMENT_TYPE_LABEL_TR[document.documentType] ?? document.documentType} · v{document.version}</small>
                    </span>
                    <IconChevronRight size={14} />
                  </button>
                ))}
              </div>

              {showUpload && (
                <form onSubmit={handleUpload} className="upload-form">
                  <div className="section-heading">
                    <span>{UI_TEXT.uploadTitle}</span>
                    <button type="button" onClick={() => setShowUpload(false)}>Kapat</button>
                  </div>
                  <input required placeholder={UI_TEXT.uploadTitleField} value={title} onChange={(event) => setTitle(event.target.value)} className="field-control" />
                  <div className="grid gap-2 sm:grid-cols-2">
                    <select value={documentType} onChange={(event) => setDocumentType(event.target.value)} className="field-control" aria-label={UI_TEXT.uploadTypeField}>
                      {DOCUMENT_TYPES.map((type) => <option key={type} value={type}>{DOCUMENT_TYPE_LABEL_TR[type]}</option>)}
                    </select>
                    <input placeholder={UI_TEXT.uploadLanguageField} value={language} onChange={(event) => setLanguage(event.target.value)} className="field-control" />
                  </div>
                  <input placeholder={UI_TEXT.uploadProviderField} value={provider} onChange={(event) => setProvider(event.target.value)} className="field-control" />
                  <input placeholder={UI_TEXT.uploadTagsField} value={tags} onChange={(event) => setTags(event.target.value)} className="field-control" />
                  <div className="grid gap-2 sm:grid-cols-2">
                    <label className="field-label">{UI_TEXT.uploadEffectiveFromField}
                      <input required type="date" value={effectiveFrom} onChange={(event) => setEffectiveFrom(event.target.value)} className="field-control mt-1" />
                    </label>
                    <label className="field-label">{UI_TEXT.uploadEffectiveToField}
                      <input type="date" value={effectiveTo} onChange={(event) => setEffectiveTo(event.target.value)} className="field-control mt-1" />
                    </label>
                  </div>
                  <textarea required placeholder={UI_TEXT.uploadContentField} value={content} onChange={(event) => setContent(event.target.value)} rows={6} className="field-control resize-y" />
                  <button type="submit" disabled={uploading} className="primary-button w-full">
                    <IconUpload size={14} /> {UI_TEXT.uploadSubmit}
                  </button>
                  {uploadError && <p className="text-xs text-danger">{uploadError}</p>}
                </form>
              )}
              {uploadOk && (
                <div className="mt-3">
                  <p className="flex items-center gap-2 text-xs text-confirm">
                    <IconCheck size={14} /> {UI_TEXT.uploadSuccess}
                  </p>
                  {uploadedDetail && (uploadedDetail.chunks ?? []).length > 0 && (
                    <div className="mt-3 rounded-xl border border-line bg-surface-sunken p-3">
                      <div className="section-heading">
                        <span>Belge nasıl indekslendi</span>
                        <button type="button" onClick={() => setUploadedDetail(null)}>Gizle</button>
                      </div>
                      <div className="mb-3 grid grid-cols-3 gap-2">
                        <div className="knowledge-stat">
                          <strong>{uploadedDetail.chunks.length}</strong><span>Chunk</span>
                        </div>
                        <div className="knowledge-stat">
                          <strong>
                            {uploadedDetail.chunks.reduce((total, chunk) => total + chunk.tokenCount, 0)}
                          </strong>
                          <span>Toplam token</span>
                        </div>
                        <div className="knowledge-stat">
                          <strong className="!text-[12px] leading-5">
                            {uploadedDetail.chunks[0]?.embeddingModel ?? '—'}
                          </strong>
                          <span>Embedding modeli</span>
                        </div>
                      </div>
                      <div className="chunk-preview-grid">
                        {uploadedDetail.chunks.map((chunk, index) => (
                          <div key={chunk.chunkId} className="chunk-preview">
                            <header>
                              <span>#{index + 1} · {chunk.sectionTitle || 'Genel bölüm'}</span>
                              <span>{chunk.tokenCount} token · {chunk.chunkId}</span>
                            </header>
                            <p>{chunk.content}</p>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </section>

            <section id="section-shortcuts" className="mt-8 scroll-mt-4">
              <div className="section-heading"><span>Klavye kısayolları</span></div>
              <ul className="overflow-hidden rounded-xl border border-line">
                {SHORTCUTS.map(([keys, description]) => (
                  <li key={keys} className="flex items-center justify-between gap-4 border-b border-line-soft px-3 py-2.5 text-xs text-ink-muted last:border-b-0">
                    <span>{description}</span>
                    <span className="kbd">{keys}</span>
                  </li>
                ))}
              </ul>
            </section>
          </div>
        </div>

        {selectedDocument && (
          <div className="document-detail-overlay">
            <div className="flex items-start justify-between gap-3 border-b border-line p-5">
              <div className="min-w-0">
                <button
                  type="button"
                  onClick={() => setSelectedDocument(null)}
                  className="mb-2 inline-flex items-center gap-1.5 text-xs font-medium text-signal hover:text-signal-strong"
                >
                  <span className="rotate-180"><IconChevronRight size={13} /></span> Bilgi tabanına dön
                </button>
                <p className="eyebrow">{selectedDocument.documentId} · v{selectedDocument.version}</p>
                <h3 className="mt-1 font-display font-semibold">{selectedDocument.title}</h3>
              </div>
              <button type="button" onClick={() => setSelectedDocument(null)} className="icon-button" aria-label="Belge detayını kapat">
                <IconClose size={18} />
              </button>
            </div>
            <div className="scroll-thin flex-1 overflow-y-auto p-5">
              <div className="metadata-grid">
                <span>Tür<strong>{DOCUMENT_TYPE_LABEL_TR[selectedDocument.documentType] ?? selectedDocument.documentType}</strong></span>
                <span>Provider<strong>{selectedDocument.provider || 'Genel'}</strong></span>
                <span>Dil<strong>{selectedDocument.language || 'tr'}</strong></span>
                <span>Chunk<strong>{selectedDocument.chunks.length}</strong></span>
              </div>
              {selectedDocument.tags && selectedDocument.tags.length > 0 && (
                <div className="mt-4 flex flex-wrap gap-1">
                  {selectedDocument.tags.map((tag) => <span key={tag} className="tag">#{tag}</span>)}
                </div>
              )}
              <h4 className="detail-title">Sanitize edilmiş içerik</h4>
              <pre className="document-content scroll-thin">{selectedDocument.sanitizedContent}</pre>
              <h4 className="detail-title">Chunk ayrıntıları</h4>
              <div className="space-y-3">
                {selectedDocument.chunks.map((chunk) => (
                  <details key={chunk.chunkId} className="chunk-card">
                    <summary><span>{chunk.sectionTitle || 'Genel bölüm'}</span><code>{chunk.tokenCount} token</code></summary>
                    <p>{chunk.content}</p>
                    <code>{chunk.chunkId} · {chunk.embeddingModel}</code>
                  </details>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
