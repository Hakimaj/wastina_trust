# ADR-023: Quarantine uploads and short-lived document access

- Status: Accepted for Task 7
- Date: 2026-09-30

## Decision

User uploads are written to private quarantine storage, scanned, validated, hashed, and promoted to available state only after a clean result. Document downloads use authorization-first short-lived signed URLs.

## Reason

A successful byte upload is not a safe or authorized document. OWASP guidance supports allowlists, content validation, generated filenames, size limits, authorization, and malware analysis. AWS documents presigned URLs as bearer access with explicit expiry and permission requirements.

## Consequences

- unscanned or scanner-failed files are not generally available
- object keys do not contain user-controlled filenames or sensitive identifiers
- URL leakage remains a bounded residual risk until expiry
- storage adapters must provide private buckets, encryption, lifecycle, and audit integration
- production scanner/KMS/storage implementations require security review
