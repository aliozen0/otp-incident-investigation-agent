-- M12.1: preserve the validated natural-language answer and application-owned rich RAG citations.
ALTER TABLE investigation ADD COLUMN analysis_summary TEXT;
ALTER TABLE investigation
    ADD COLUMN knowledge_citations JSONB NOT NULL DEFAULT '[]'::jsonb;
