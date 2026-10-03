# Authentication Boundary

Task 2 implements browser OIDC through `keycloak-js` using Authorization Code with PKCE (`S256`). The Keycloak instance and its bearer tokens remain in memory. Tokens are never written to `localStorage`, URLs, router state, logs, or analytics.

## Verified backend contract

- `GET /api/v1/me` returns `{ id, account_status, identity_status, roles }` when the OIDC subject is mapped to a Wastina user and returns `404` when no mapping exists.
- `POST /api/v1/me/provision` requires `{ email, phoneNumber }` and returns the same account view. The backend derives identity subject from the authenticated JWT; the frontend does not send a user ID.
- `PATCH /api/v1/me` exists but is outside this authentication task.
- Backend roles currently use `ROLE_WORKER`, `ROLE_GUARANTOR`, `ROLE_EMPLOYER`, and `ROLE_ADMIN`.
- The current `/me` response does not contain organization memberships or permissions. Organization context is therefore empty until the backend exposes it. JWT scopes are retained only as capability hints; backend authorization remains authoritative.

## Provider configuration

The local realm defines a public `wastina-api` client with standard flow enabled and an audience mapper. It also enables direct access grants for local testing, but the browser uses standard Authorization Code + PKCE and never submits passwords to Wastina or copies the direct-grant flow.

The committed realm does not define browser redirect URIs, so Keycloak client configuration must allow the configured frontend callback and post-logout URI before manual login can work.

Authentication, role, organization, and permission implementation details must remain behind this feature boundary. Generic API and layout code must not import Keycloak directly.
