# Task 8 Technical Debt Register

| ID | Priority | Problem | Impact | Workaround | Owner/status |
|---|---|---|---|---|---|
| TD-001 | Critical | Controllers still accept actor/user IDs from request input in several foundation APIs | BOLA and impersonation risk | Keep endpoints non-production; replace with authenticated principal context | Security hardening required |
| TD-002 | Critical | Outbox persistence exists but RabbitMQ publisher/inbox consumers are not wired | Events can remain unpublished | Monitor pending rows and publish manually only under controlled operations | Integration required |
| TD-003 | High | Payment provider integrations are mock-only except public Chapa documentation boundary | No production financial operation | Mock provider and feature gates | Provider onboarding required |
| TD-004 | High | Document storage/scanning/KMS adapters are mock-only | No production document durability or malware assurance | Local mock storage/scanner | Security/operations implementation required |
| TD-005 | High | No full integration/E2E or restore/DR drill has been executed | Cross-domain behavior is not proven | Unit/domain tests only | Test infrastructure required |
| TD-006 | Medium | No centralized audit port or immutable audit store | Accountability is fragmented | Domain event metadata and logs | Audit module required |
| TD-007 | Medium | No OpenAPI artifact or generated contract tests | API compatibility is manual | Versioned controller paths | API documentation required |
| TD-008 | Medium | No production IaC, managed database, secret manager, or deployment target | Deployment is not reproducible outside local Compose | Dockerfile and local Compose | Infrastructure decision required |
