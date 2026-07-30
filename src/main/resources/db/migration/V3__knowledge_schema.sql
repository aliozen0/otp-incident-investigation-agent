-- M4: RAG knowledge schema (docs/08-rag-spec.md, FR-008, DATA-003/004).

CREATE TABLE knowledge_document (
    document_id     TEXT NOT NULL,
    version         TEXT NOT NULL,
    document_type   TEXT NOT NULL,
    provider        TEXT,
    effective_from  DATE NOT NULL,
    effective_to    DATE,
    language        TEXT NOT NULL,
    tags            JSONB NOT NULL DEFAULT '[]'::jsonb,
    title           TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (document_id, version)
);

CREATE TABLE knowledge_chunk (
    chunk_id         TEXT PRIMARY KEY,
    document_id      TEXT NOT NULL,
    version          TEXT NOT NULL,
    section_title    TEXT,
    content          TEXT NOT NULL,
    token_count      INTEGER NOT NULL,
    embedding_model  TEXT NOT NULL,
    -- DATA-004: dimension pinned to nvidia/nv-embedqa-e5-v5 (docs/16 ADR-015, docs/19). A model
    -- change with a different dimension requires a new migration and full re-ingestion.
    embedding        vector(1024) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (document_id, version) REFERENCES knowledge_document (document_id, version)
);

CREATE INDEX idx_knowledge_chunk_embedding ON knowledge_chunk
    USING hnsw (embedding vector_cosine_ops);
