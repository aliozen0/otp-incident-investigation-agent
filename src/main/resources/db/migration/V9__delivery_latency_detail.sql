-- Tail latency and retry volume: an average alone hides the shape of a provider degradation, and
-- the console's data explorer needs the same columns the tools reason about.
ALTER TABLE otp_delivery_sample
    ADD COLUMN p95_delivery_seconds NUMERIC(10, 3) NOT NULL DEFAULT 0,
    ADD COLUMN retries              BIGINT         NOT NULL DEFAULT 0;
