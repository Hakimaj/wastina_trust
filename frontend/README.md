# Wastina Frontend Foundation

Task 1 establishes the React frontend boundary for the Wastina Digital Trust Platform. Business modules, Keycloak authentication, and domain API hooks are intentionally deferred to later tasks.

## Prerequisites

- Node.js 22 or newer
- npm
- The Wastina backend is optional for foundation development. Its local API is expected at `http://localhost:8080` when used.

## Commands

```text
npm install
npm run dev
npm run build
npm run typecheck
npm run lint
npm run test
npm run format:check
```

## Environment

Copy `.env.example` to `.env.local` for local development:

- `VITE_API_BASE_URL`: browser-safe backend origin. The backend currently has mixed `/api` and `/api/v1` routes and no OpenAPI contract.
- `VITE_ENVIRONMENT`: non-secret environment label.
- `VITE_DEFAULT_LOCALE`: `en` or `am`.

Anything prefixed with `VITE_` can be exposed to the browser. Never place client secrets, payment secrets, database credentials, encryption keys, signing keys, webhook secrets, cloud credentials, or service credentials in these files.

## Architecture

- `src/app`: application bootstrap, providers, configuration, and router.
- `src/api`: the only HTTP boundary; manual contracts are temporary until the backend publishes OpenAPI.
- `src/auth`: authentication implementation boundary; Task 1 does not implement OIDC.
- `src/features`: domain ownership. Features may consume API, lib, shared components, and public feature interfaces, but not another feature's internals.
- `src/components`: generic UI primitives with no Wastina business rules.
- `src/layouts`: public and authenticated route layout boundaries.
- `src/types`: small shared foundational types, not a domain model dump.
- `src/styles`: global tokens and predictable base styles.
- `src/tests`: test setup and foundation smoke tests.

Server state belongs in TanStack Query. Local UI state belongs in React state/hooks. Global state should remain limited to session, active role/organization, locale, safe feature flags, and notification count when those features are implemented.

## Security rules

The frontend does not implement authorization. Backend authorization is authoritative. The API client does not log tokens, PII, documents, complaint narratives, or payment credentials. It supports cancellation and structured errors, and it does not retry mutations automatically. Authentication/token storage belongs to Task 2 and must not be added to generic components or `localStorage`.

The backend currently requires JWTs, uses Keycloak/OIDC configuration, has no standardized error schema or OpenAPI artifact, and has incomplete CORS/resource authorization. Those are backend integration dependencies, not frontend workarounds.
