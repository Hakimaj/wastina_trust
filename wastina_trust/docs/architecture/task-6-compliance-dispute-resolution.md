# Task 6 — Compliance, Complaints & Dispute Resolution

**Status:** Foundation implemented; production governance and legal review required
**Date:** 2026-09-30

## 1. Purpose and boundary

Compliance owns platform complaints, cases, investigations, evidence references, decisions, appeals, temporary actions, and canonical compliance events. It does not own identity, binary document storage, employment state, guarantee state, payment state, or Trust Score calculation.

The workflow is explicitly:

`Allegation -> Complaint -> Triage/Case -> Investigation -> Evidence -> Decision -> Appeal -> Final outcome -> Trust handoff`

A complaint is not a finding. A platform finding is not a criminal conviction, court judgment, labour determination, or regulator decision.

## 2. Repository audit

Existing foundations reused:

- IAM `User` and existing role/authentication boundary
- Trust `TrustEvent`, deterministic calculator, score snapshot/history
- Guarantee and Employment entities as referenced source records
- Flyway/PostgreSQL persistence
- Spring service/controller conventions

No competing identity, file store, Trust calculator, or authentication subsystem was added. Compliance stores only `document_reference` values for evidence; Document Service remains responsible for binary objects, scanning, secure downloads, versions, and retention.

## 3. Domain model

### Complaint

A relationship-aware intake object containing complainant, subject, controlled category, factual description, relationship/source reference, severity, status, and immutable submission timing. It begins in `DRAFT` and can be submitted only with meaningful narrative. Submission never establishes a finding and never changes Trust Score.

### ComplianceCase

A structured matter created from a complaint. It has a public reference, subject, severity, lifecycle status, decision type/reason, policy version, timestamps, and optimistic version. State commands are explicit:

`OPEN -> INVESTIGATION -> DECISION_PENDING -> DECIDED -> APPEALED -> FINAL`

Cases may be placed on hold or closed by a later controlled command. Generic client status PATCH is intentionally not part of the API.

### Investigation

A separately attributable investigator assignment with methodology, findings, start/completion timestamps, and an explicit `ASSIGNED -> IN_PROGRESS -> COMPLETED` lifecycle. A missed information request is not automatically guilt.

### CaseEvidence

Evidence is independent of the case row. It stores a Document Service reference, type, source, uploader, hash metadata, scan/review status, confidentiality classification, and optimistic version. Raw binary content is never stored in Compliance.

`SCAN_PENDING` evidence cannot be treated as available until the Document Service confirms safe publication. A hash detects later file-byte changes; it does not prove authorship or truthfulness.

### Decision

Decisions use controlled outcomes: `UPHELD`, `PARTIALLY_UPHELD`, `DISMISSED`, `UNSUBSTANTIATED`, `WITHDRAWN`, `NO_ACTION`, or `REFERRED`. A final decision is not silently overwritten; later appeal/reversal is a new historical action.

### Appeal

Appeals are separate records with grounds, appellant, state, and outcome. Appeal review must use a separate authorized reviewer in production policy. An overturned finding creates a corrective event; it does not delete the original decision.

### ComplianceAction

Temporary platform safeguards are separate from findings. Actions require a reason and an expiry or review date, and can be released. They are not direct Trust penalties.

## 4. Authorization model

Every operation must enforce authentication, permission, case scope, relationship, and current state. Roles are intentionally distinct:

| Action | Support | Case reviewer | Compliance manager | Auditor | Admin |
|---|---:|---:|---:|---:|---:|
| View case status | limited | yes | yes | read-only | controlled |
| View sensitive evidence | no | assigned scope | authorized scope | controlled read | controlled |
| Investigate | no | assigned scope | yes | no | explicit grant |
| Issue decision | no | assigned scope | yes | no | separate authorization |
| Review appeal | no | no by default | separate reviewer | no | controlled |
| Modify IAM | no | no | no | no | separate security role |

No generic `ADMIN` capability should imply unrestricted evidence or decision access. Conflict-of-interest checks, assignment history, privileged MFA, and audit logging remain mandatory production controls.

## 5. Visibility model

| Data | Subject | Complainant | Reviewer | Auditor | External user |
|---|---:|---:|---:|---:|---:|
| Category/status | own case | own submission/allowed outcome | yes | controlled | no |
| Narrative | authorized view | own | yes | controlled | no |
| Evidence | policy-scoped | own/allowed | yes | controlled | no |
| Internal notes | no | no | yes | controlled | no |
| Decision summary | yes | limited | yes | yes | limited |
| Appeal status | yes | limited | yes | yes | no |

Complainant identity, third-party evidence, internal notes, and raw document contents are not exposed by default. Public views may show only neutral states such as `Under Review` when product policy permits.

## 6. Evidence lifecycle

`UPLOADED -> SCAN_PENDING -> AVAILABLE -> UNDER_REVIEW -> ACCEPTED|PARTIALLY_ACCEPTED|NOT_RELIED_UPON`

Alternative controlled states include `CHALLENGED`, `REJECTED`, `SUPERSEDED`, `REDACTED`, and `WITHDRAWN`. Original evidence must remain preserved when a redacted derivative or replacement is created. Document upload controls belong to Document Service: content validation, malware scanning, size/type limits, safe object naming, quarantine, and signed-download authorization.

## 7. Trust integration contract

Compliance emits canonical facts only after the decision transaction commits through the eventual transactional outbox:

- `COMPLAINT_SUBMITTED`
- `CASE_OPENED`
- `INVESTIGATION_STARTED`
- `DECISION_ISSUED`
- `CASE_UPHELD`
- `CASE_PARTIALLY_UPHELD`
- `CASE_DISMISSED`
- `APPEAL_SUBMITTED`
- `APPEAL_RESOLVED`
- `CASE_OVERTURNED`
- `TEMPORARY_RESTRICTION_APPLIED`
- `TEMPORARY_RESTRICTION_RELEASED`
- `CASE_FINALIZED`

The event payload contains event ID/version, source service, case ID, subject ID, occurred/effective times, and minimal outcome metadata. It does not contain complaint narrative, evidence bytes, national ID, or private notes.

The Trust Service decides whether and how a finalized eligible outcome affects score. `COMPLAINT_SUBMITTED` is informational/review state and cannot create a permanent negative score effect. `CASE_OVERTURNED` is a corrective fact. Compliance never emits a score delta.

Duplicate event delivery is safe through unique event identity and idempotent consumers. The current `compliance_events` table is the durable event record; transactional outbox publication is the next infrastructure integration required before production.

## 8. Failure behavior

- Document Service unavailable: evidence remains pending; case does not fail open.
- Notification unavailable: case/decision commits; delivery retries independently.
- Trust Service unavailable: decision and compliance event remain durable for later delivery.
- Database unavailable: command fails without a partial success response.
- Event publisher unavailable: committed decision remains and unpublished event is retryable.

## 9. API foundation

The initial explicit-command endpoints are:

- `POST /api/v1/complaints`
- `POST /api/v1/compliance/complaints/{complaintId}/cases`
- `POST /api/v1/compliance/cases/{caseId}/investigation`
- `POST /api/v1/compliance/cases/{caseId}/decision`
- `POST /api/v1/compliance/cases/{caseId}/appeals`
- `POST /api/v1/compliance/cases/{caseId}/actions`

These are domain-foundation endpoints. Before production, actor identity must be derived from the authenticated principal rather than client-provided IDs/roles, and each endpoint must add relationship-based case scope, idempotency keys, rate limits, safe error codes, pagination, audit ports, and notification/outbox adapters.

## 10. Database ownership

Migration `V3__compliance_foundation.sql` creates:

- `complaints`
- `compliance_cases`
- `case_evidence`
- `investigations`
- `compliance_appeals`
- `compliance_actions`
- `compliance_events`

Constraints cover distinct complainant/subject, foreign-key ownership, unique public/event references, optimistic versions, temporary-action expiry/review requirements, and action date ordering. Compliance does not duplicate Document, Trust, Employment, Guarantee, or Payment tables.

## 11. Threat model and controls

| Threat | Control | Detection/response |
|---|---|---|
| Arbitrary accusation | relationship/source references and server validation | rate/duplicate review |
| Complaint treated as guilt | separate complaint/case/decision states | Trust event eligibility checks |
| Evidence tampering | Document quarantine, hash metadata, immutable references | access/audit alerts |
| Cross-case access | assigned case scope and confidentiality matrix | authorization-failure logs |
| Reviewer abuse | role separation, MFA, assignment history, conflict declaration | privileged monitoring |
| Decision forgery | explicit commands, authorized role, optimistic version, durable event | duplicate/concurrency tests |
| Appeal self-review | separate appeal authority requirement | policy enforcement |
| Evidence leak | classification, scoped signed downloads, access audit | export/download monitoring |
| Complaint flooding | rate limits, duplicate detection, abuse flags | operational review |
| Trust event forgery | internal producer boundary and unique event IDs | event validation/rejection |

Risk flags are review signals, not findings or automatic Trust penalties.

## 12. Legal classification

The following classifications are intentionally conservative:

- Ethiopian proclamation status and substantive legal applicability: **UNKNOWN / REQUIRES AUTHORITATIVE CONFIRMATION** until verified against current primary sources.
- Platform decision is not a judicial or regulatory finding: **ENGINEERING INTERPRETATION; LEGAL REVIEW REQUIRED** for user-facing wording.
- Data minimization, scoped access, auditability, and secure upload are **INTERNATIONAL BEST PRACTICE** and **ENGINEERING INTERPRETATION**, not a claim of Ethiopian legal compliance.
- Retention, legal holds, disclosure, appeal rights, external referrals, and evidentiary effect: **LEGAL REVIEW REQUIRED**.
- This system does not replace courts, labour authorities, regulators, payment providers, or statutory dispute mechanisms.

## 13. Functional requirements

- `COMP-FR-001`: accepted complaints require an authenticated eligible relationship and distinct subject.
- `COMP-FR-002`: complaint, case, investigation, evidence, decision, and appeal are separate concepts.
- `COMP-FR-003`: complaint submission never directly changes Trust Score.
- `COMP-FR-004`: case transitions are explicit and invalid transitions fail.
- `COMP-FR-005`: evidence references Document Service objects and stores no binary content.
- `COMP-FR-006`: evidence access is confidentiality-scoped and auditable.
- `COMP-FR-007`: only authorized scoped reviewers can issue decisions.
- `COMP-FR-008`: final decisions are historical and cannot be silently rewritten.
- `COMP-FR-009`: appeals preserve original decisions and support corrective outcomes.
- `COMP-FR-010`: temporary actions require reason and expiry/review date.
- `COMP-FR-011`: only canonical eligible outcomes are handed to Trust.
- `COMP-FR-012`: duplicate event delivery has one business effect.

## 14. Non-functional requirements

- All case mutations use authenticated actor context, authorization checks, transaction boundaries, and optimistic concurrency.
- Sensitive evidence access produces an audit event with actor, case, evidence, purpose, and timestamp.
- Decision commands are idempotent or reject duplicate state transitions without a second outcome.
- Case history remains reconstructable after appeal, reversal, or offboarding.
- Notification and Trust outages cannot erase committed case decisions.
- User-facing statuses and notifications use localized neutral terminology; no unsupported criminal labels.
- Case lists, evidence lists, and timelines are paginated and authorization-filtered.
- Production targets for latency, availability, retention, and RTO/RPO require business and legal approval before being frozen.

## 15. Test strategy and current evidence

Implemented focused tests cover:

- complaint submission is not a finding
- invalid decision rewrite is rejected
- appeal overturn preserves the original decision
- temporary action requires expiry/review
- support role cannot decide; scoped compliance manager can

Required next tests include BOLA/IDOR, evidence upload/scanning, assigned-reviewer checks, concurrent decisions, duplicate appeals/events, outbox retry, notification failure, Trust outage, exports, legal hold, and authenticated-principal enforcement.

## 16. Human/legal review checklist

- complaint eligibility by relationship/category
- Ethiopian data-processing basis and data-subject rights
- employment/payment/guarantee sector rules
- notification timing and exceptions
- platform terminology and decision disclosures
- evidence disclosure/redaction and retention
- appeal windows and late appeals
- temporary restrictions and emergency handling
- legal holds and external referrals
- reviewer independence, MFA, and two-person approval

## 17. Handoff contract and freeze

Compliance owns complaints, cases, investigations, evidence references, decisions, appeals, actions, referrals, risk flags, and compliance event production.

Document owns binaries/scanning/storage/downloads. Trust owns score/policy/history/calculation. Guarantee and Employment own their domain records. IAM owns identity/authentication/roles/permissions.

Any future change to the complaint, case, investigation, evidence, decision, appeal, action, privacy, retention, event, API, or ownership model requires an ADR.
