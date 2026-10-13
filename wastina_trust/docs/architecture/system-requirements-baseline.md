# Wastina System Requirements Baseline

| ID | Requirement | Priority | Evidence/status |
|---|---|---|---|
| SYS-FR-001 | IAM remains the source of truth for identity, authentication, roles, and permissions. | Critical | Existing IAM entities/config; principal hardening pending |
| SYS-FR-002 | Trust calculations are deterministic and replayable from versioned facts/policy. | Critical | Calculator tests pass |
| SYS-FR-003 | Guarantee, employment, compliance, payment, and document state remain separately owned. | Critical | Task 1-7 ADRs/docs |
| SYS-FR-004 | State-changing workflows use explicit state guards and optimistic concurrency where needed. | Critical | Domain tests; integration coverage pending |
| SYS-FR-005 | Authoritative cross-module events are persisted transactionally before publication. | Critical | Outbox entity/migration/tests; publisher pending |
| SYS-FR-006 | Event consumers deduplicate by event identity and route poison messages to DLQ. | Critical | Contract requirement; consumer/DLQ implementation pending |
| SYS-FR-007 | Payment success requires server-side verification and amount/currency/reference checks. | Critical | Gateway domain tests pass; real providers pending |
| SYS-FR-008 | Documents use private object storage, quarantine/scanning, generated keys, hashes, and scoped access. | Critical | Mock gateway tests pass; production adapters pending |
| SYS-FR-009 | Complaint filing does not directly reduce Trust Score; only authorized outcomes may be consumed by Trust. | Critical | Task 3/6 design and domain tests |
| SYS-FR-010 | Production releases are tested, scanned, versioned, deployable, observable, and rollback-capable. | Critical | CI/Docker baseline committed; executed release evidence pending |
| SYS-NFR-SEC-001 | Secrets, credentials, tokens, raw documents, and sensitive narratives are excluded from logs/events. | Critical | Design requirement; log scanning pending |
| SYS-NFR-REC-001 | Database, object storage, broker, secrets, and deployment artifacts can be restored through tested procedures. | Critical | NOT IMPLEMENTED; DR drill required |
| SYS-NFR-AUD-001 | High-impact actions have attributable actor, request/correlation/event IDs, timestamps, and immutable audit history. | Critical | Partial metadata; central audit pending |
| SYS-NFR-OBS-001 | Health, metrics, logs, traces, queue/outbox age, provider failures, and storage/scanner failures are observable. | High | Actuator baseline; telemetry backend/alerts pending |
| SYS-NFR-PRIV-001 | Sensitive data is purpose-limited, access-scoped, retained, and deleted/held through approved policy. | Critical | Architecture documented; legal/retention implementation pending |
| SYS-NFR-MAINT-001 | Provider and infrastructure-specific details remain behind replaceable adapters. | High | Gateway ports and mock adapters exist |
