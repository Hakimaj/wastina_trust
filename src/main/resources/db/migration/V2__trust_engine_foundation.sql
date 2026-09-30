CREATE TABLE IF NOT EXISTS trust_events (
    event_id UUID PRIMARY KEY,
    source_event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    source_service VARCHAR(60) NOT NULL,
    subject_user_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    quality NUMERIC(5,4) NOT NULL,
    adjudicated BOOLEAN NOT NULL,
    positive BOOLEAN NOT NULL,
    negative BOOLEAN NOT NULL,
    CONSTRAINT uk_trust_source_event UNIQUE (source_service, source_event_id)
);

CREATE INDEX IF NOT EXISTS idx_trust_events_subject_effective
    ON trust_events (subject_user_id, effective_at);

CREATE TABLE IF NOT EXISTS trust_scores (
    user_id UUID PRIMARY KEY,
    score INTEGER NOT NULL CHECK (score BETWEEN 0 AND 100),
    internal_score NUMERIC(8,4) NOT NULL,
    confidence VARCHAR(20) NOT NULL,
    evidence_status VARCHAR(30) NOT NULL,
    policy_version VARCHAR(30) NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS trust_score_history (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    previous_score INTEGER NOT NULL CHECK (previous_score BETWEEN 0 AND 100),
    new_score INTEGER NOT NULL CHECK (new_score BETWEEN 0 AND 100),
    previous_confidence VARCHAR(20) NOT NULL,
    new_confidence VARCHAR(20) NOT NULL,
    policy_version VARCHAR(30) NOT NULL,
    reason_code VARCHAR(60) NOT NULL,
    trigger_event_id UUID,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_trust_history_user_created
    ON trust_score_history (user_id, created_at DESC);