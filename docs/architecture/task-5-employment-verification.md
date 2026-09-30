# Task 5 — Career & Employment History / Employment Verification

## 1. Purpose

The Employment Service is the authoritative owner of:

- employer organizations
- organization memberships and authority
- employment claims and historical records
- verification requests and assertions
- employment ratings
- correction and dispute workflows
- emission of canonical employment events to the Trust Service

The service does not calculate trust scores. It emits facts; the Trust Engine interprets them under policy.

---

## 2. Architecture boundary

This project remains a modular monolith with strict ownership:

- IAM owns users, authn/authz, roles, sessions, identity proofs, and consent metadata.
- Employment owns organization relationships, employment records, verification requests, ratings, and evidence references.
- Document owns storage, document lifecycle, scanning, retention, and secure access.
- Notification owns message creation and delivery.
- Trust owns score calculation, confidence, and event interpretation.
- Dispute / compliance owns formal compliance decisions.

The Employment domain must not duplicate IAM or Trust logic. It must store only the minimal context required to manage a verified employment workflow.

---

## 3. Core domain model

### 3.1 Person
A Wastina user remains the foundational person identity in IAM. Employment does not create a separate user type.

### 3.2 Employer organization
Organizations are legal or business entities represented in Wastina.

Core model:

- `Organization`
  - id
  - name
  - legalName
  - description
  - status (`PENDING`, `VERIFICATION_REQUIRED`, `VERIFIED`, `SUSPENDED`, `REJECTED`, `DEACTIVATED`)
  - verifiedAt

### 3.3 Organization membership
A user gains authority only through a membership linked to an organization.

Core model:

- `OrganizationMembership`
  - id
  - organization
  - user
  - role
  - status
  - joinedAt
  - endedAt

Roles:

- `OWNER`
- `HR_ADMIN`
- `HR_MANAGER`
- `MANAGER`
- `VERIFIER`
- `VIEWER`
- `EMPLOYEE`

Only active members with verification-capable roles may validate employment records.

### 3.4 Employment relationship
The relationship between worker and employer is distinct from the user and from the organization membership.

`EmploymentRecord` captures this relationship directly:

- worker
- employer organization
- job title in English and Amharic
- occupation classification
- employment type
- work relationship type
- sector
- start date / end date
- current employment flag
- status
- verification status

### 3.5 Employment verification request
A verification request is a business object that asks an authorized organization to validate a claim.

State machine:

`CREATED -> SENT -> DELIVERED -> VIEWED -> IN_REVIEW -> VERIFIED|PARTIALLY_VERIFIED|DISPUTED|REJECTED`

with optional terminal states:

- `EXPIRED`
- `CANCELLED`
- `REVOKED`

### 3.6 Verification assertion
The system separates:

- claimant assertion
- employer verification assertion
- resolved record state

This preserves historical truth and supports corrections and revocations without deleting the original fact.

---

## 4. Verification workflow

1. Worker creates a self-reported employment record.
2. Record begins in `SELF_REPORTED`.
3. Worker requests verification from a target employer organization.
4. Employer receives a request via the platform notification service.
5. Authorized HR/manager/verifier reviews the claim.
6. Verification result is recorded as:
   - `VERIFIED`
   - `PARTIALLY_VERIFIED`
   - `DISPUTED`
   - `REJECTED`
7. The platform preserves the original claim and the employer assertion.
8. Trust receives an event, but it never receives direct score mutations from the Employment Service.

---

## 5. Organization verification workflow

An organization becomes eligible to act on employment claims through a controlled onboarding lifecycle:

`PENDING -> VERIFICATION_REQUIRED -> VERIFIED -> SUSPENDED|REJECTED|DEACTIVATED`

This is a separate authorization model from the employment relationship itself. A user being an employee or HR representative does not imply full verification authority for unrelated organizations.

---

## 6. Authorization model

The core rule is:

`role + organization membership + active status + permission context`

This is enforced in the domain validation layer and the service layer.

Critical checks:

- a user can only verify employment for an organization where they are a valid active member
- a user must hold a verification-capable role
- a request must match the target organization exactly
- revoked or expired membership blocks future verification actions
- historical audit events remain attributable even after membership withdrawal

---

## 7. Occupation and bilingual support

The platform supports:

- `job_title_en`
- `job_title_am`
- optional `occupation_code`
- optional ISO/ISCO-like classification reference
- local occupation extension where an exact mapping is unavailable

The project uses a lightweight occupation classification layer, rather than forcing every Ethiopian title into an imported foreign taxonomy.

This model supports:

- classification
- classification_version
- code
- title_en
- title_am
- description_en
- description_am
- parent_code

The codebase keeps occupational classification as a reference-data layer, not as a substitute for local job meaning.

---

## 8. Privacy and data minimization

Employment data is treated as sensitive. The platform follows these principles:

- Most employment history remains private by default.
- Only relevant data is disclosed to counterparties, landlords, guarantors, or the applicant by policy.
- Evidence documents are referenced by document ID, not embedded in the employment domain.
- Salary, personal IDs, and sensitive HR notes are not added to core employment records unless explicitly reviewed.
- Notifications avoid exposing sensitive conclusions in SMS/email bodies.

### Privacy-by-default examples

- landlord access is limited to approved summary information
- guarantor access requires explicit policy and data-scoping
- internal HR comments remain confidential to the organization
- rating comments are stored separately from the base employment facts and are policy-gated

---

## 9. Rating model

Ratings are intentionally separated from trust-score calculation.

The Employment Service stores recorded ratings and their status, but the Trust Service decides how they are interpreted. The rating model is deliberately limited, checked, and auditable.

Core rules:

- rating values must be between 1 and 5
- only eligible raters may submit under the organization/person relationship policy
- duplicate ratings are restricted
- withdrawn or disputed ratings remain stored as history rather than deleted
- rating comments are not passed directly to trust scoring

---

## 10. Correction, dispute, and revocation

The system preserves historical truth by recording corrections and revocations instead of silently rewriting facts.

Examples:

- a worker requests a correction to dates
- an employer disputes a title or period
- a verifier revokes an earlier assertion
- a dispute is escalated to admin or compliance review

The original claim and verification assertion remain in the audit history. The current record view is a resolved state, not a replacement for the prior fact.

---

## 11. Event contract

The Employment Service emits authoritative canonical employment events only after a transaction is committed.

Examples:

- `OrganizationVerified`
- `OrganizationSuspended`
- `EmploymentCreated`
- `EmploymentVerificationRequested`
- `EmploymentVerified`
- `EmploymentPartiallyVerified`
- `EmploymentDisputed`
- `EmploymentCorrected`
- `EmploymentVerificationRevoked`
- `EmploymentEnded`
- `EmploymentRatingSubmitted`
- `EmploymentRatingWithdrawn`
- `EmploymentRatingDisputed`

These events must not include salary, national ID, private HR notes, or document contents. Trust receives only necessary identifiers and evidence metadata.

---

## 12. API surface

The repository includes a minimal employment API surface aligned to the architecture freeze:

- `POST /api/v1/workers/{workerId}/employment`
- `POST /api/v1/employment/{employmentRecordId}/verification-requests`
- `POST /api/v1/organizations/{organizationId}/verification-requests/{requestId}/verify`

These endpoints are intentionally minimal and should be augmented with authorization, pagination, and scoping before production deployment. The core design keeps the server authoritative and prevents client-supplied verification/organization fields from overriding system state.

---

## 13. Database ownership

The Employment domain owns schema objects for:

- organizations
- organization_memberships
- employment_records
- employment_verification_requests
- employment_verification_assertions
- employment_ratings
- employment_evidence
- occupation_classifications

The implementation is in the `entity` and `repository` packages, separate from IAM, Trust, and Guarantee state.

---

## 14. Legal and human review checklist

The following items remain human/legal review required before production rollout:

- Ethiopian labour-law applicability by worker type and employer status
- personal-data processing basis and consent scope under Proclamation 1321/2024
- retention periods for employment records, verification requests, documents, and audit logs
- whether organization verification should use official registration evidence, domain evidence, or approved onboarding steps
- whether rating identities remain visible, anonymous, or partially anonymized
- whether landlords, guarantors, or counterparties need a lawful disclosure basis beyond account access
- formal document redaction standards and evidence retention rules

---

## 15. Implementation status in this repository

The codebase now contains the core Task 5 domain baseline required for continued implementation:

- `Organization`
- `OrganizationMembership`
- `OrganizationMembershipRole`
- `OrganizationMembershipStatus`
- `OrganizationStatus`
- `EmploymentRecord`
- `EmploymentVerificationRequest`
- `EmploymentVerificationAssertion`
- `EmploymentRating`
- `EmploymentEvidence`
- `OccupationClassification`
- `EmploymentValidationService`
- `EmploymentService`
- `EmploymentController`
- focused domain tests proving the guardrails

This is a safe starting point for the next phase: API authorization hardening, repository expansion, documentation of full visibility matrices, and formal event-outbox integration.
