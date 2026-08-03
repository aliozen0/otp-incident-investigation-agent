-- Operational telemetry the read-only agent tools query, and the console's data explorer renders.
-- Until now this lived only as in-code fixtures, so an operator could not check the agent's answer
-- against the underlying rows. One row per minute per provider keeps any time window aggregatable.

CREATE TABLE otp_delivery_sample (
    bucket_at            TIMESTAMPTZ NOT NULL,
    provider             TEXT        NOT NULL,
    attempted            BIGINT      NOT NULL CHECK (attempted >= 0),
    delivered            BIGINT      NOT NULL CHECK (delivered >= 0),
    failed               BIGINT      NOT NULL CHECK (failed >= 0),
    avg_delivery_seconds NUMERIC(10, 3) NOT NULL CHECK (avg_delivery_seconds >= 0),
    PRIMARY KEY (bucket_at, provider),
    CONSTRAINT otp_delivery_sample_total CHECK (delivered + failed = attempted)
);
CREATE INDEX idx_otp_delivery_sample_bucket ON otp_delivery_sample (bucket_at);

CREATE TABLE otp_error_sample (
    bucket_at  TIMESTAMPTZ NOT NULL,
    provider   TEXT        NOT NULL,
    error_code TEXT        NOT NULL,
    failures   BIGINT      NOT NULL CHECK (failures >= 0),
    PRIMARY KEY (bucket_at, provider, error_code)
);
CREATE INDEX idx_otp_error_sample_bucket ON otp_error_sample (bucket_at);

CREATE TABLE provider_health_sample (
    bucket_at                  TIMESTAMPTZ NOT NULL,
    provider                   TEXT        NOT NULL,
    status                     TEXT        NOT NULL,
    avg_response_seconds       NUMERIC(10, 3) NOT NULL,
    timeout_rate               NUMERIC(6, 4)  NOT NULL,
    circuit_breaker_state      TEXT        NOT NULL,
    active_connections         INT         NOT NULL,
    max_connections            INT         NOT NULL,
    last_successful_request_at TIMESTAMPTZ,
    PRIMARY KEY (bucket_at, provider)
);
CREATE INDEX idx_provider_health_sample_bucket ON provider_health_sample (bucket_at);

CREATE TABLE queue_health_sample (
    bucket_at                            TIMESTAMPTZ PRIMARY KEY,
    pending_messages                     BIGINT NOT NULL,
    normal_pending_threshold             BIGINT NOT NULL,
    oldest_message_age_seconds           BIGINT NOT NULL,
    normal_oldest_age_threshold_seconds  BIGINT NOT NULL,
    active_consumers                     INT    NOT NULL,
    expected_consumers                   INT    NOT NULL,
    dead_letter_count                    BIGINT NOT NULL,
    processing_rate_status               TEXT   NOT NULL,
    status                               TEXT   NOT NULL
);

CREATE TABLE change_event (
    change_id   TEXT PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    type        TEXT NOT NULL,
    component   TEXT NOT NULL,
    description TEXT NOT NULL,
    version     TEXT,
    approved    BOOLEAN
);
CREATE INDEX idx_change_event_occurred ON change_event (occurred_at);
