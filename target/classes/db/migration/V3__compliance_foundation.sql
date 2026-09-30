CREATE TABLE IF NOT EXISTS complaints (
    id UUID PRIMARY KEY,
    public_reference VARCHAR(40) NOT NULL UNIQUE,
    complainant_user_id UUID NOT NULL REFERENCES users(id),
    subject_user_id UUID NOT NULL REFERENCES users(id),
    category VARCHAR(40) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    relationship_type VARCHAR(40) NOT NULL,
    source_entity_type VARCHAR(60) NOT NULL,
    source_entity_id VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    severity VARCHAR(20),
    submitted_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_complaint_distinct_users CHECK (complainant_user_id <> subject_user_id)
);

CREATE INDEX IF NOT EXISTS idx_complaints_subject ON complaints(subject_user_id);
CREATE INDEX IF NOT EXISTS idx_complaints_status ON complaints(status);

CREATE TABLE IF NOT EXISTS compliance_cases (
    id UUID PRIMARY KEY,
    public_reference VARCHAR(40) NOT NULL UNIQUE,
    complaint_id UUID REFERENCES complaints(id),
    subject_user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(30) NOT NULL,
    severity VARCHAR(20),
    decision_type VARCHAR(30),
    decision_reason VARCHAR(4000),
    policy_version VARCHAR(40),
    opened_at TIMESTAMPTZ,
    decided_at TIMESTAMPTZ,
    finalized_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_cases_subject ON compliance_cases(subject_user_id);
CREATE INDEX IF NOT EXISTS idx_cases_status ON compliance_cases(status);

CREATE TABLE IF NOT EXISTS case_evidence (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES compliance_cases(id),
    document_reference VARCHAR(200) NOT NULL,
    evidence_type VARCHAR(40) NOT NULL,
    submitted_by UUID NOT NULL REFERENCES users(id),
    description VARCHAR(1000),
    source VARCHAR(40) NOT NULL,
    integrity_algorithm VARCHAR(30),
    integrity_hash VARCHAR(128),
    status VARCHAR(30) NOT NULL,
    confidentiality VARCHAR(30) NOT NULL,
    review_status VARCHAR(30) NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_case_evidence_case ON case_evidence(case_id);
CREATE INDEX IF NOT EXISTS idx_case_evidence_document ON case_evidence(document_reference);

CREATE TABLE IF NOT EXISTS investigations (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL UNIQUE REFERENCES compliance_cases(id),
    investigator_user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    methodology VARCHAR(4000),
    findings VARCHAR(4000),
    assigned_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS compliance_appeals (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES compliance_cases(id),
    appellant_user_id UUID NOT NULL REFERENCES users(id),
    grounds VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    decision_type VARCHAR(20),
    explanation VARCHAR(4000),
    submitted_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS compliance_actions (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES compliance_cases(id),
    type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    applied_by UUID NOT NULL REFERENCES users(id),
    starts_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    review_at TIMESTAMPTZ,
    CONSTRAINT ck_action_expiry_or_review CHECK (expires_at IS NOT NULL OR review_at IS NOT NULL),
    CONSTRAINT ck_action_dates CHECK (expires_at IS NULL OR starts_at IS NULL OR expires_at >= starts_at)
);

CREATE TABLE IF NOT EXISTS compliance_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(50) NOT NULL,
    case_id UUID REFERENCES compliance_cases(id),
    subject_user_id UUID REFERENCES users(id),
    source_service VARCHAR(60) NOT NULL,
    event_version INTEGER NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    adjudicated BOOLEAN NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_compliance_events_case ON compliance_events(case_id);
