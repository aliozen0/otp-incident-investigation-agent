-- M3: Investigation, IncidentDraft and audit_event schema (docs/05, FR-015/016/017, DATA-005).

CREATE TABLE investigation (
    id                    UUID PRIMARY KEY,
    question              TEXT NOT NULL,
    time_window_start     TIMESTAMPTZ NOT NULL,
    time_window_end       TIMESTAMPTZ NOT NULL,
    prompt_version        TEXT NOT NULL,
    schema_version        TEXT NOT NULL,
    phase                 TEXT NOT NULL,
    result_status         TEXT,
    severity              TEXT,
    confidence            DOUBLE PRECISION,
    validation_report     JSONB,
    evidence              JSONB NOT NULL DEFAULT '[]'::jsonb,
    hypotheses            JSONB NOT NULL DEFAULT '[]'::jsonb,
    recommended_actions   JSONB NOT NULL DEFAULT '[]'::jsonb,
    knowledge_references  JSONB NOT NULL DEFAULT '[]'::jsonb,
    tool_executions       JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE incident_draft (
    id                    UUID PRIMARY KEY,
    investigation_id      UUID NOT NULL,
    payload               TEXT NOT NULL,
    idempotency_key       TEXT NOT NULL,
    status                TEXT NOT NULL,
    approval              JSONB,
    external_incident_id  TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_incident_draft_idempotency_key UNIQUE (idempotency_key)
);

-- SEC-006/AC-014: idempotency is guaranteed by this DB constraint, not application logic alone.

CREATE TABLE audit_event (
    id                UUID PRIMARY KEY,
    occurred_at       TIMESTAMPTZ NOT NULL,
    actor             TEXT NOT NULL,
    action            TEXT NOT NULL,
    investigation_id  UUID,
    approval_id       UUID,
    correlation_id    TEXT,
    result            TEXT NOT NULL,
    policy_version    TEXT,
    details           JSONB
);

-- DATA-005: audit is append-only; reject UPDATE/DELETE at the database level.
CREATE FUNCTION reject_audit_event_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_event is append-only: % not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_event_no_update
    BEFORE UPDATE ON audit_event
    FOR EACH ROW EXECUTE FUNCTION reject_audit_event_mutation();

CREATE TRIGGER audit_event_no_delete
    BEFORE DELETE ON audit_event
    FOR EACH ROW EXECUTE FUNCTION reject_audit_event_mutation();
