-- M12.1: retain only the sanitized canonical body needed by the read-only knowledge explorer.
ALTER TABLE knowledge_document ADD COLUMN sanitized_content TEXT;
