-- M11: optional client-generated session/thread id (docs/16 ADR-017). No FK/uniqueness --
-- client-generated UUID, multiple investigations share one sessionId as a chat thread.

ALTER TABLE investigation ADD COLUMN session_id TEXT;

CREATE INDEX idx_investigation_session_id ON investigation (session_id);
