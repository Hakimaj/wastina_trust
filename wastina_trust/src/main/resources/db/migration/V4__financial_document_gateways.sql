CREATE TABLE IF NOT EXISTS payment_intents (
    id BIGSERIAL PRIMARY KEY,
    payer_id UUID NOT NULL REFERENCES users(id),
    guarantee_id BIGINT,
    amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(6) NOT NULL,
    purpose VARCHAR(120) NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_reference VARCHAR(160),
    idempotency_key VARCHAR(160) NOT NULL UNIQUE,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    verification_status VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS payment_transactions (
    id UUID PRIMARY KEY,
    payment_intent_id BIGINT NOT NULL REFERENCES payment_intents(id),
    provider VARCHAR(30) NOT NULL,
    provider_transaction_id VARCHAR(160) NOT NULL,
    merchant_reference VARCHAR(160) NOT NULL,
    amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(6) NOT NULL,
    status VARCHAR(35) NOT NULL,
    provider_status VARCHAR(80),
    server_verified BOOLEAN NOT NULL,
    verified_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_payment_provider_transaction UNIQUE (provider, provider_transaction_id)
);

CREATE TABLE IF NOT EXISTS payment_webhooks (
    id UUID PRIMARY KEY,
    provider VARCHAR(30) NOT NULL,
    provider_event_id VARCHAR(160) NOT NULL,
    provider_transaction_id VARCHAR(160),
    payload_digest VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL,
    signature_valid BOOLEAN NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    retry_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_payment_provider_event UNIQUE (provider, provider_event_id)
);

CREATE TABLE IF NOT EXISTS payment_refunds (
    id UUID PRIMARY KEY,
    payment_transaction_id UUID NOT NULL REFERENCES payment_transactions(id),
    amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(6) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL UNIQUE,
    provider_reference VARCHAR(160),
    status VARCHAR(30) NOT NULL,
    requested_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS payment_reconciliations (
    id UUID PRIMARY KEY,
    payment_transaction_id UUID NOT NULL REFERENCES payment_transactions(id),
    expected_amount NUMERIC(18,2) NOT NULL,
    provider_amount NUMERIC(18,2),
    expected_currency VARCHAR(6) NOT NULL,
    provider_currency VARCHAR(6),
    status VARCHAR(30) NOT NULL,
    discrepancy VARCHAR(1000),
    detected_at TIMESTAMPTZ NOT NULL,
    resolved_by UUID REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    public_reference VARCHAR(40) NOT NULL UNIQUE,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    document_type VARCHAR(35) NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    classification VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    scan_status VARCHAR(20) NOT NULL,
    storage_provider VARCHAR(40) NOT NULL,
    bucket VARCHAR(120) NOT NULL,
    object_key VARCHAR(240) NOT NULL,
    object_version VARCHAR(160) NOT NULL,
    content_hash VARCHAR(128),
    hash_algorithm VARCHAR(20),
    content_size BIGINT,
    detected_mime_type VARCHAR(100),
    declared_mime_type VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    uploaded_at TIMESTAMPTZ,
    retention_until TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_document_object_version UNIQUE (bucket, object_key, object_version)
);

CREATE INDEX IF NOT EXISTS idx_documents_owner ON documents(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);

CREATE TABLE IF NOT EXISTS document_upload_sessions (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL UNIQUE REFERENCES documents(id),
    owner_user_id UUID NOT NULL REFERENCES users(id),
    allowed_size BIGINT NOT NULL CHECK (allowed_size > 0),
    allowed_mime_types VARCHAR(300) NOT NULL,
    upload_object_key VARCHAR(240) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
