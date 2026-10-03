# Authentication Backend Contract

This note records source-verified behavior from `wastina_trust`.

## `/api/v1/me`

`GET /api/v1/me` looks up `User.identitySubject` using the authenticated JWT `sub` claim. A matching user returns:

```json
{
  "id": "uuid",
  "account_status": "ACTIVE",
  "identity_status": "UNVERIFIED",
  "roles": ["ROLE_WORKER"]
}
```

The controller returns `404` when the identity mapping is missing. It does not return email, phone, organization memberships, permissions, or profiles.

## `/api/v1/me/provision`

`POST /api/v1/me/provision` accepts:

```json
{
  "email": "person@example.test",
  "phoneNumber": "+251..."
}
```

The server derives `identitySubject` from the authenticated JWT. No user ID is accepted or sent by the frontend.

## Keycloak

The committed local realm defines public client `wastina-api`, enables standard flow, and adds an API audience mapper. Direct access grants are enabled for local testing but are not used by this browser client. No browser redirect URI is committed in the realm file.

## Current limitations

- `/me` does not expose organization memberships or permissions.
- Most backend controllers still accept caller-supplied actor IDs.
- CORS configuration was not found in the backend source.
- A standardized API error schema and OpenAPI document are not present.