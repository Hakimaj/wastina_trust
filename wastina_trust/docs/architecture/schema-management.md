# Schema Management

## Current state

| Profile | `spring.flyway.enabled` | `spring.jpa.hibernate.ddl-auto` | Effect |
|---|---|---|---|
| default (`application.properties`) | `true` | `validate` | Flyway migrates, then Hibernate verifies the mapping matches |
| `local` | `false` | `update` | Hibernate creates and extends the schema |
| `development` | `false` | `update` | same as `local` |
| `docker` | `false` | `update` | same as `local` |
| `test` | `false` | `create-drop` | H2 in PostgreSQL mode, rebuilt per run |

**Important: no runtime profile currently uses `true` + `validate`.** The `default` profile does,
but it is only selected when no other profile is active, which is not how this project is started.

## Why Flyway is disabled outside `default`

The committed migrations (`V1`–`V5`) are not a complete fresh-install baseline. `V1` begins with:

```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS identity_subject VARCHAR(255);
```

It alters a `users` table that no migration creates. On a fresh, empty database this fails, which is
why every runnable profile disables Flyway and lets Hibernate build the schema from the JPA
mappings instead. This is recorded in `application-local.properties` and
`application-docker.properties`.

## Why this is left as-is

Two reasons, both of which need a decision rather than a code change:

1. **Enabling Flyway now would break startup.** A fresh local database cannot run `V1`–`V5`. Doing
   so requires a new `V0`/`V1` baseline migration that creates `users` and every other table from
   scratch, which is a schema-authoring task with real consequences for any database that already
   exists. Doing that unprompted risks data loss, so it is not attempted here.

2. **Hibernate `update` is not reproducible across environments.** Two developers starting from
   empty databases can end up with subtly different column sets. That is a genuine problem, but the
   fix is a reviewed baseline migration, not a configuration flip.

No database was erased, reset or migrated as part of this work.

## What is required to make this consistent

1. Author a complete baseline migration set that creates every table from empty — starting from the
   JPA mappings, which are the current source of truth for the schema.
2. Reconcile the existing `V1`–`V5` against it. The `ALTER TABLE users` statements imply a baseline
   that was never committed.
3. Add `organization_invitations` to the migrations. That table was added by
   `OrganizationInvitation` as part of the organization lifecycle work and currently only exists
   where Hibernate creates it.
4. Switch `local` and `docker` to `flyway.enabled=true` with `ddl-auto=validate`.
5. Verify against a database seeded from the current migrations before removing `ddl-auto=update`
   anywhere.

## Immediate guidance

- **Local development:** leave it as-is. `ddl-auto=update` on an empty database produces a working
  schema.
- **Shared/staging databases:** treat the schema as unmanaged until the baseline exists. Do not point
  two instances at one database expecting either to be authoritative.
- **Production:** this project is not production-ready in this respect. `docker-compose.yml` starts
  with `ddl-auto=update`, which allows an application instance to change the schema of a database
  that may be in use. Fix the baseline migrations first.