# Task 4: Digital Wastina / Guarantee Management and Payment Integration

**Status:** Initial guarantee/payment model implemented as a secure, auditable foundation for the Wastina modular monolith.
**Date:** 2026-09-30

## 1. Scope and architecture compliance

This module follows the Task 1 architecture freeze and the IAM/Trust boundaries documented in Tasks 1-3.

- Guarantee lifecycle and liability remain under the Guarantee bounded context.
- Payment initiation, verification, and webhook reconciliation remain under the Payment bounded context.
- `User` identity remains canonical and is not duplicated.
- Trust only consumes canonical business outcome events, not raw provider technical details.
- No raw payment credentials, provider secrets, or national-ID values are stored in the application database.
- Human legal and regulatory review remains required for the final enforceability of a Wastina guarantee and any regulated payment activity.

## 2. Guarantee domain design

The guarantee model separates the agreement from the underlying obligation and tracks liability explicitly.

### Core concepts

- Guarantee request: a proposed business arrangement sent to a guarantor.
- Guarantee agreement: the accepted and versioned guarantee contract.
- Underlying obligation: the rent, employment, loan, or service commitment being backed by the guarantee.
- Liability: the current exposure that may become payable.
- Claim: a formal request that the guarantee be triggered.
- Default: a validated failure of the underlying obligation.
- Completion: a legitimate end state after obligations are satisfied or released.

The implementation uses separate domain concepts instead of one overloaded `status` field.

## 3. State machine

Baseline lifecycle:

```text
DRAFT
  -> PENDING_ACCEPTANCE
  -> ACCEPTED
  -> ACTIVE
  -> FULFILLED | DEFAULTED | CANCELLED | EXPIRED
      -> CLAIMED
      -> RESOLVED / PARTIALLY_RESOLVED
```

The Java model uses the `GuaranteeStatus` enum and guards exposure-based transitions with the `GuaranteeCapacityService` policy.

## 4. Party and relationship model

The platform reuses the canonical `User` identity and links all roles through `GuaranteeParty`.

Role taxonomy:

- APPLICANT
- GUARANTOR
- BENEFICIARY
- EMPLOYER
- LENDER
- OTHER

This keeps the guarantee domain explicit without introducing a second person model.

## 5. Contract versioning and integrity

The `Guarantee` entity stores:

- public reference
- type
- principal, guarantor, and beneficiary references
- amount and currency
- liability and status
- contract version and terms version
- contract hash
- signature status

`ContractIntegrityService` provides a canonicalization + SHA-256 digest workflow so contract tampering is detectable.

Important distinction:

- SHA-256 hash = fingerprint / integrity check
- Digital signature = cryptographic proof tied to signing keys
- Electronic signature = legal/functional status under the applicable law

These are intentionally not conflated.

## 6. Capacity and concurrency model

Guarantor exposure is calculated with a deterministic service that measures:

- active guarantee count
- active liability
- pending/accepted exposures
- projected new liability

The implementation enforces a conservative capacity decision before a guarantee is accepted.

This is the foundation for the concurrency requirement described in the Task 4 brief: a capacity decision is evaluated against current exposure and should be protected by transaction boundaries, row locks, or optimistic retries in production.

## 7. Payment architecture

The payment model is intentionally separate from the guarantee lifecycle.

- `PaymentIntent` owns the internal expected payment state.
- `PaymentProviderType` enumerates the provider adapters.
- `PaymentProvider` defines a provider adapter contract.
- `MockPaymentProvider` provides a safe sandbox pattern for local development.

### Provider contract

- `createPaymentIntent`
- `verifyPayment`
- `verifyWebhookSignature`

This keeps the domain logic separate from provider-specific HTTP protocols and lets real provider adapters be added without changing guarantee logic.

## 8. Provider research and legal status

The implementation does not invent provider APIs or claim compliance.

Current status matrix:

- Telebirr: adapter contract ready; official onboarding and API contract required before production use.
- CBE Birr: adapter contract ready; official onboarding and API contract required before production use.
- Chapa: mock + verification pattern included, but official API/webhook contract must be matched before production use.

The code clearly distinguishes between implemented sandbox flows and production-ready provider integrations.

## 9. Payment security rules

The platform intentionally does not trust browser redirects or unsigned/webhook payloads.

The proper workflow is:

1. create the internal intent
2. verify provider-issued details server-to-server
3. confirm amount/currency/reference match expectations
4. mark verification status before business release
5. make state transitions idempotent
6. record the event and audit context

The `PaymentWebhookController` and `MockPaymentProvider` show the verification pattern, but production integrations must use provider-specific signatures, secret-safe verification, and replay protection.

## 10. Database model

This implementation adds the following protective foundation:

- `guarantees`
- `guarantee_parties`
- `payment_intents`

These are the core tables for the domain model required by Task 4.

## 11. Event and trust handoff

The guarantee/payment model emits platform-level outcomes as canonical business artifacts rather than raw technical events.

Examples:

- `GuaranteeCompleted`
- `GuaranteeDefaultConfirmed`
- `PaymentSucceeded`
- `PaymentFailed`
- `PaymentVerificationRequired`

The Trust Engine decides how those canonical outcomes affect score, while Guarantee/Payment remain the owners of business state.

## 12. Human/legal decisions required

The following remain explicit human decisions and cannot be replaced by code alone:

### Legal

- What form of guarantee is legally sufficient under Ethiopian law?
- Which party accepts the contract and by what electronic-signature workflow?
- What notices and remedies are required for default or claim?
- What retention and dispute-hold period is required?

### Regulatory

- Whether Wastina is acting only as a merchant/payment recipient or more deeply as a payment intermediary.
- Whether holding or disbursing funds triggers licensing/consumer protection obligations.
- Whether the guarantee model needs provider licensing or structured financial review.

### Product

- Fee model and refund policy
- Guarantee types and eligibility rules
- Liability caps and capacity rules
- Claim/default review process

### Provider

- Exact production API contract
- Merchant onboarding requirements
- Callback/webhook security specifics
- Refund and reversal handling
- Settlement and reconciliation expectations

## 13. Implementation status

### Implemented

- Guarantee domain entities and statuses
- Guarantee party model
- Basic guarantee capacity evaluation
- Contract hashing and canonicalization service
- Payment intent model and provider interface
- Mock provider adapter
- Payment webhook validation skeleton
- Security-focused testing for tampering and capacity breach cases
- Task 4 architecture document and ADR notes

### Not production-certified

- Real legal decisioning for guarantee enforceability
- Real Ethiopian regulator-approved digital signature process
- Real payment provider credentials or provider contracts
- Full production-grade webhook replay and reconciliation pipeline
- Full financial ledger and refund policy governance

## 14. Definition of done for this repo slice

The repository now contains a hardened base for the next Task 4 implementation slice:

1. Guarantee lifecycle model is explicit.
2. Liability and capacity are separated from raw status.
3. Contract integrity can be verified via hashing.
4. Payment is separate from guarantee state.
5. Provider abstraction exists for real API adoption.
6. Webhook verification is modeled safely.
7. The design respects the earlier architecture boundaries.
8. Remaining legal and regulatory decisions are documented as human review required.
