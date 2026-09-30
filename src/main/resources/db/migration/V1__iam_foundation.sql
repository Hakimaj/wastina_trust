ALTER TABLE users ADD COLUMN IF NOT EXISTS identity_subject VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_status VARCHAR(30) NOT NULL DEFAULT 'PENDING';
ALTER TABLE users ADD COLUMN IF NOT EXISTS identity_status VARCHAR(30) NOT NULL DEFAULT 'UNVERIFIED';
ALTER TABLE users ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_identity_subject
    ON users (identity_subject)
    WHERE identity_subject IS NOT NULL;

CREATE TABLE IF NOT EXISTS iam_consents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    purpose VARCHAR(100) NOT NULL,
    policy_version VARCHAR(100) NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL,
    withdrawn_at TIMESTAMPTZ,
    source VARCHAR(30) NOT NULL,
    locale VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS identity_verifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    provider VARCHAR(40) NOT NULL,
    provider_reference VARCHAR(200) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    identity_status VARCHAR(30) NOT NULL,
    assurance_level VARCHAR(50),
    failure_reason_code VARCHAR(80),
    consent_id UUID NOT NULL,
    initiated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_identity_verifications_user
    ON identity_verifications (user_id, initiated_at DESC);

