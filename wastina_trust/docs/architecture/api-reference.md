# API Reference

Generated OpenAPI is served at `/v3/api-docs` with Swagger UI at `/swagger-ui.html` in the `local`,
`development` and `docker` profiles. This document is the narrative summary; the OpenAPI document is
authoritative for field-level detail.

Base URL in local development: `http://localhost:8080`

## Authentication

Every endpoint except health checks and the local development token endpoint requires:

```
Authorization: Bearer <access_token>
```

The token is one of:

| Mode | Issued by | How to obtain |
|---|---|---|
| Production / `docker` | Keycloak (OIDC) | Authorization Code + PKCE via the browser |
| `local` / `development` | The API itself | `POST /api/v1/dev/auth/token` |

A missing, malformed, wrongly signed, expired, wrongly issued or wrongly audienced token returns
`401`. A valid token for an account that is not `ACTIVE` returns `403`.

## Error envelope

Every failure uses one shape:

```json
{
  "status": 409,
  "code": "CONFLICT",
  "message": "A verification request for this employment record is already open.",
  "path": "/api/v1/me/employment/…/verification-requests",
  "requestId": "0f8c1c9e-1a2b-4c3d-9e8f-7a6b5c4d3e2f",
  "timestamp": "2026-10-12T08:44:10.318Z",
  "fieldErrors": []
}
```

`code` is stable and machine-readable. Branch on it rather than on the message.

| Status | Codes |
|---|---|
| `400` | `VALIDATION_FAILED`, `MALFORMED_REQUEST`, `INVALID_ARGUMENT`, `UNSUPPORTED_MEDIA_TYPE` |
| `401` | `UNAUTHORIZED` |
| `403` | `FORBIDDEN`, `ORGANIZATION_MEMBERSHIP_REQUIRED` |
| `404` | `NOT_FOUND` |
| `409` | `CONFLICT`, `IDENTITY_ALREADY_VERIFIED`, `INVALID_STATE_TRANSITION`, `INSUFFICIENT_GUARANTEE_CAPACITY`, `ORGANIZATION_NAME_TAKEN`, `MEMBERSHIP_ALREADY_EXISTS`, `ORGANIZATION_NOT_JOINABLE`, `CONCURRENT_MODIFICATION` |
| `413` | `PAYLOAD_TOO_LARGE` |
| `500` | `INTERNAL_ERROR` (no stack trace; quote `requestId`) |

Every response echoes an `X-Request-Id` header, taken from the inbound `X-Request-Id` or
`X-Correlation-Id` when supplied, otherwise generated.

## Pagination

List endpoints accept `page` (0-based) and `size` and return:

```json
{
  "content": [ … ],
  "page": 0,
  "size": 20,
  "totalElements": 57,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

`size` is clamped to 200.

---

## IAM

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/me` | Current account |
| `POST` | `/api/v1/me/provision` | Create or complete the local account |
| `PATCH` | `/api/v1/me` | Update email / phone |

`GET /api/v1/me` returns both the original snake_case keys and the new camelCase ones, so an
existing client keeps working:

```json
{
  "id": "uuid",
  "account_status": "ACTIVE",
  "identity_status": "UNVERIFIED",
  "roles": ["ROLE_WORKER"],
  "identitySubject": "dev-worker",
  "email": "worker@wastina.local",
  "phoneNumber": "+251900000001",
  "accountStatus": "ACTIVE",
  "identityStatus": "UNVERIFIED",
  "enabled": true,
  "verifiedAt": null,
  "createdAt": "2026-10-12T08:00:00",
  "updatedAt": "2026-10-12T08:00:00"
}
```

`roles` comes from persisted database roles and is the source of truth for route access and
navigation.

**`provision` cannot grant a privileged role.** Requesting `ROLE_ADMIN` is silently ignored; only
`ROLE_WORKER`, `ROLE_GUARANTOR` and `ROLE_EMPLOYER` are self-service. `ROLE_ADMIN` is granted by
Keycloak realm role or an operator.

Request body:

```json
{ "email": "person@example.com", "phoneNumber": "+251...", "roles": ["ROLE_WORKER"] }
```

`email` and `phoneNumber` are required. Email must be RFC-valid; phone must match
`^\+?[0-9 ()-]{7,32}$`.

## Profiles

All are **self-only**: a token for user A cannot read user B's profile (`403`).

| Method | Path | Notes |
|---|---|---|
| `GET`/`PUT`/`DELETE` | `/api/workers/{userId}/profile` | `404` when not created |
| `GET`/`PUT`/`DELETE` | `/api/employers/{userId}/profile` | |
| `GET`/`PUT`/`DELETE` | `/api/guarantors/{userId}/profile` | `kycStatus` is read-only |

```jsonc
// worker
{ "professionName": "Electrician", "locationName": "Addis Ababa", "isAvailable": true }
// employer — subscriptionTier must be FREE or PREMIUM
{ "companyName": "Acme PLC", "locationName": "Addis Ababa", "subscriptionTier": "FREE" }
// guarantor — kycStatus is NOT accepted and is never written
{ "financialLimit": 250000.00, "employerName": "Ministry of Trade" }
```

## Organizations

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/organizations` | Create; caller becomes `OWNER` |
| `GET` | `/api/v1/organizations/{id}` | Read (member only) |
| `POST` | `/api/v1/organizations/join` | Redeem an invitation code |
| `DELETE` | `/api/v1/organizations/{id}/membership` | Leave |
| `GET` | `/api/v1/organizations/{id}/members` | Members (paged, `?sort=role`) |
| `PATCH` | `/api/v1/organizations/{id}/members/{memberUserId}` | Change a member's role |
| `POST` | `/api/v1/organizations/{id}/invitations` | Create an invitation |
| `GET` | `/api/v1/organizations/{id}/invitations` | List invitations |
| `DELETE` | `/api/v1/organizations/{id}/invitations/{invitationId}` | Revoke |

A newly created organization has status `PENDING` and must be verified by an operator before its
employment claims are authoritative.

**Invitations** are single-use and time-limited (72 h). The join code is returned exactly once, at
creation; only its SHA-256 digest is stored, so it cannot be recovered afterwards. Redeeming always
grants the non-privileged `EMPLOYEE` role — never a verifying role. Granting `VERIFIER`,
`HR_MANAGER`, `HR_ADMIN` or `OWNER` requires an existing member who already holds that role.

```jsonc
// POST /api/v1/organizations
{ "name": "Acme PLC", "legalName": "Acme PLC", "description": "Construction" }
// POST /api/v1/organizations/join
{ "joinCode": "the-one-time-code" }
// POST /api/v1/organizations/{id}/invitations  -> returns joinCode once
{}
// PATCH /api/v1/organizations/{id}/members/{userId}
{ "role": "VERIFIER" }
```

`GET /api/v1/me/organizations` returns memberships with server-derived capability flags, so the
frontend never re-implements the role rules:

```json
[{
  "id": "…", "organizationId": "…", "name": "Acme PLC", "legalName": "Acme PLC",
  "organizationStatus": "VERIFIED", "role": "OWNER", "membershipStatus": "ACTIVE",
  "joinedAt": "…", "endedAt": null,
  "canVerifyEmployment": true, "canManageMemberships": true
}]
```

Optional `?status=ACTIVE` filter.

## Employment

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/me/employment` | Caller's records |
| `GET` | `/api/v1/me/employment/{id}` | One record |
| `POST` | `/api/v1/me/employment` | Create a record |
| `GET` | `/api/v1/me/employment-verification-requests` | Requests the caller submitted |
| `POST` | `/api/v1/me/employment/{id}/verification-requests` | Ask the employer to verify |
| `GET` | `/api/v1/organizations/{orgId}/employment-verification-requests` | Organization inbox |
| `POST` | `/api/v1/organizations/{orgId}/employment-verification-requests/{requestId}/review` | Begin review |
| `POST` | `/api/v1/organizations/{orgId}/employment-verification-requests/{requestId}/verify` | Confirm |

**Creating an employment record requires active membership of the target organization.** A caller
who is not a member gets `403`; a member whose membership is inactive gets
`409 ORGANIZATION_MEMBERSHIP_REQUIRED`.

Request body (only this DTO was server-validated before; now all are):

```jsonc
{
  "organizationId": "uuid",        // required
  "jobTitleEn": "Site Engineer",   // required, not blank
  "jobTitleAm": "…",               // optional
  "occupationCode": "…",           // optional
  "department": "…",               // optional
  "location": "…",                 // optional
  "employmentType": "FULL_TIME",   // required enum
  "workRelationshipType": "EMPLOYEE", // required enum
  "sector": "PRIVATE",             // required enum
  "startDate": "2023-01-01",       // required
  "endDate": null,                 // optional
  "currentEmployment": true,       // optional
  "description": "…"               // optional, max 2000
}
```

Enums: `employmentType` = `FULL_TIME|PART_TIME|TEMPORARY|CONTRACT|CASUAL|INTERNSHIP|APPRENTICESHIP|CONSULTANT|SEASONAL|OTHER`;
`workRelationshipType` = `EMPLOYEE|CONTRACTOR|CONSULTANT|SELF_EMPLOYED|VOLUNTEER|INTERN|OTHER`;
`sector` = `PUBLIC|PRIVATE|NGO|OTHER`.

Cross-field rules (`400`):
- `endDate` must not precede `startDate`.
- `currentEmployment: true` with a non-null `endDate` is rejected.

A duplicate open verification request is refused with `409`.

Listing the organization inbox requires an `ACTIVE` membership whose role includes verification
authority (`OWNER`, `HR_ADMIN`, `HR_MANAGER`, `MANAGER`, `VERIFIER`). Anyone else gets `403`.

## Identity

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/identity-verifications` | Caller's attempts |
| `POST` | `/api/v1/identity-verifications` | Start an attempt |
| `GET` | `/api/v1/identity-verifications/{id}` | One attempt |
| `POST` | `/api/v1/identity-verifications/{id}/complete` | Complete it with the provider |

```jsonc
// POST body
{ "purpose": "ACCOUNT_OPENING", "policyVersion": "v1", "locale": "en" }
```

`locale` must be BCP-47 (`en`, `am-ET`). Starting returns the provider `redirectUrl` and
`actionRequired`, so a client can send the user onward — this is the step that was previously
missing. `POST /{id}/complete` is owner-scoped and idempotent.

## Trust score

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/trust-score/me` | Caller's stored score |
| `GET` | `/api/v1/trust-score/me/history` | Caller's history, newest first |
| `GET` | `/api/v1/trust-score/{userId}` | Self, or administrator |
| `GET` | `/api/v1/trust-score/{userId}/history` | Self, or administrator |
| `POST` | `/api/v1/trust-score/me/recalculate` | Recalculate and persist |

**`GET` is side-effect free.** It returns `404` if no score has been calculated. Recalculation is
an explicit `POST`.

```jsonc
{ "userId": "…", "score": 72, "internalScore": 72.0000, "confidence": "MEDIUM",
  "evidenceStatus": "VERIFIED", "policyVersion": "v1", "calculatedAt": "…" }
```

## Guarantees

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/guarantees` | Create; caller is always the principal |
| `GET` | `/api/v1/guarantees` | Guarantees the caller is party to |
| `GET` | `/api/v1/guarantees/{publicReference}` | Read (parties only) |
| `POST` | `/api/v1/guarantees/{publicReference}/accept` | Guarantor only |
| `POST` | `/api/v1/guarantees/{publicReference}/reject` | Guarantor only |

```jsonc
// POST body
{ "guarantorId": "uuid", "beneficiaryId": null, "amount": 25000.00,
  "currency": "ETB", "type": "RENT", "purpose": "April rent", "description": "" }
```

Returns `201 Created` with a real, resolvable `publicReference` (`WST-XXXXXXXX`) and a
server-computed `contractHash`. A body-supplied `principalId` is ignored. Guarantor capacity
(≤5 active guarantees, ≤500,000 total liability) is evaluated against real outstanding exposure;
exceeding it returns `409 INSUFFICIENT_GUARANTEE_CAPACITY`.

`publicReference` resolves via `GET /api/v1/guarantees/WST-XXXXXXXX`; the internal numeric id also
works.

## Guarantee requests (SMS flow)

| Method | Path |
|---|---|
| `POST` | `/api/v1/me/guarantee-requests` |
| `GET` | `/api/v1/me/guarantee-requests` (paged) |
| `GET` | `/api/v1/me/guarantee-requests/{id}` |

Another account's request returns `404`, not `403`, so existence is not disclosed.

## Contracts

| Method | Path |
|---|---|
| `POST` | `/api/v1/contracts` |
| `GET` | `/api/v1/contracts` (paged; caller is a party) |
| `GET` | `/api/v1/contracts/{id}` (parties only) |

```jsonc
{ "guarantorId": "uuid", "employerId": null, "liabilityAmount": 5000.00 }
```

The worker is always the caller and `termsHash` is computed server-side from the contract terms — a
client-supplied hash is ignored.

## Documents

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/documents/upload-sessions` | Open a session |
| `POST` | `/api/v1/documents/upload-sessions/{id}/complete` | Upload the file (multipart) |
| `GET` | `/api/v1/documents` | Caller's documents (paged) |
| `GET` | `/api/v1/documents/{id}` | One document |
| `GET` | `/api/v1/documents/{id}/download-url` | Short-lived download URL |
| `DELETE` | `/api/v1/documents/{id}` | Mark for deletion |

Upload is `multipart/form-data`:

```jsonc
// POST /api/v1/documents/upload-sessions
{ "type": "IDENTITY_DOCUMENT", "purpose": "Onboarding", "declaredMimeType": "application/pdf" }
```

Response tells the client the contract: `allowedSize` (20 MiB) and `allowedMimeTypes`
(`application/pdf`, `image/jpeg`, `image/png`, `text/plain`).

```bash
curl -X POST "$BASE/api/v1/documents/upload-sessions/$SESSION/complete" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@report.pdf;type=application/pdf" \
  -F "detectedMimeType=application/pdf"
```

The file is quarantined, scanned, and only published if clean. A content type outside the session's
allow-list is rejected with `400` before anything is stored.

Download authorization is derived from ownership and document state. There is no `authorized` query
parameter; a non-owner gets `403`. The URL is valid for 300 seconds.

`GET /api/v1/documents/{id}/download-url` returns `{"url": "...", "expiresInSeconds": 300}`.

## Payments

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/me/payments` | Record a payment |
| `GET` | `/api/v1/me/payments` | Caller's payments (paged) |
| `GET` | `/api/v1/me/payments/{id}` | One of the caller's payments |
| `GET` | `/api/payments/{id}` | Any payment — **administrator only** |
| `POST` | `/api/v1/payments/intents` | Create an intent |
| `POST` | `/api/v1/payments/intents/{intentId}/initialize` | Initialize the provider transaction |
| `POST` | `/api/v1/payments/transactions/{transactionId}/verify` | Verify with the provider |
| `POST` | `/api/v1/webhooks/payment` | Provider callback |

The payer is always the caller; `payerId` in a body is ignored.

Idempotency is preserved: replaying an `idempotencyKey` with the same amount and currency returns
the existing intent; the same key with different terms returns `400`.

A non-payer cannot initialize or verify a transaction (`403`).

The webhook is provider-authenticated, not browser-authenticated. A callback with an invalid
signature is recorded as `SIGNATURE_REJECTED` and never processed; a callback with no
`X-Provider-Event-Id` is refused with `400 INVALID_WEBHOOK`.

## Compliance

All actor identifiers come from the JWT. The compliance role and case scope are derived
server-side — a client cannot claim authority.

| Method | Path | Access |
|---|---|---|
| `POST` | `/api/v1/compliance/complaints` | Any authenticated user |
| `GET` | `/api/v1/compliance/complaints` | Administrator (paged) |
| `POST` | `/api/v1/compliance/complaints/{id}/cases` | Administrator |
| `POST` | `/api/v1/compliance/cases/{id}/investigation` | Administrator |
| `POST` | `/api/v1/compliance/cases/{id}/decision` | Administrator |
| `POST` | `/api/v1/compliance/cases/{id}/appeals` | Party to the case |
| `POST` | `/api/v1/compliance/cases/{id}/actions` | Administrator |
| `GET` | `/api/v1/compliance/cases` | Administrator (paged) |
| `GET` | `/api/v1/compliance/cases/{id}` | Administrator |
| `GET` | `/api/v1/compliance/cases/{id}/appeals` | Administrator |
| `GET` | `/api/v1/compliance/cases/{id}/actions` | Administrator |

```jsonc
// POST /complaints  (complainantId comes from the token)
{ "subjectId": "uuid", "category": "PAYMENT_DEFAULT",
  "description": "Employer has not paid the agreed salary for three months.", // min 20 chars
  "relationshipType": "EMPLOYMENT", "sourceEntityType": null, "sourceEntityId": null }
// POST /cases/{id}/decision  (role and caseScopeGranted are NOT accepted)
{ "decisionType": "UPHELD", "reason": "…" }
// POST /cases/{id}/appeals  (appellantId comes from the token)
{ "grounds": "PROCESS_ERROR" }
// POST /cases/{id}/actions  (actorId comes from the token)
{ "type": "DOCUMENT_ACCESS_RESTRICTION", "reason": "…", "expiresAt": "2026-12-01T00:00:00Z" }
```

Responses are view DTOs with opaque party identifiers. Nested user data — email and phone number —
is never serialized.

## CORS

Applies in **all** profiles, not just development.

| Header | Direction |
|---|---|
| `X-Request-Id`, `X-Correlation-Id` | request and response |
| `X-Detected-Mime-Type` | request |
| `Authorization`, `Content-Type`, `Accept`, `Accept-Language` | request |

Methods: `GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD`. Credentials are never allowed
(authentication is a bearer header). Preflight is permitted without a token.

## Removed endpoints

These existed before and are gone. A client that calls them gets `404`.

| Removed | Reason |
|---|---|
| `POST /api/v1/workers/{id}/employment` | Accepted caller-supplied actor IDs; bypassed validation |
| `POST /api/v1/employment/{id}/verification-requests` | Duplicate of `/me/...`, accepted `requesterId` |
| `POST /api/v1/organizations/{id}/verification-requests/{id}/verify` | Renamed for consistency with `/review` |
| `GET /api/contracts` | Returned every contract to any caller |
| `GET /api/contracts/worker/{id}` | Any caller could read any worker's contracts |
| `POST /api/payments`, `GET /api/payments/payer/{id}` | Accepted arbitrary `payerId` |
| `GET /api/guarantee-requests/requester/{id}` | Accepted arbitrary `requesterId` |
| `GET /api/v1/documents/{id}/download-url?requesterId&authorized` | Client-asserted authorization |
| `GET /api/trust-engine/{id}/score` | Recalculated on read; readable by anyone |
| `POST /api/v1/documents/upload-sessions/{id}/complete` (JSON `byte[]`) | Replaced by multipart |