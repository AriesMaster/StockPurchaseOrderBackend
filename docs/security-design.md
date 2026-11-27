# Authentication and authorization design

## Implementation checkpoint — 27 November 2026

The active backend branch implements Argon2id credential hashing, signed JWT access tokens validated against persisted sessions, rotating refresh tokens, CSRF protection, login/logout/current-user endpoints, administrator bootstrap and employee provisioning. Administrators can change operational employees’ roles and status. The service revokes affected sessions and protects administrator accounts from role and status changes. Invitation acceptance, password reset, rate limiting, audit logging and public demo isolation are not currently implemented.

Backend tests cover credential/session behavior, CSRF and role restrictions. The local 27 November Maven verification passed 122 tests. Cloud login and full browser role workflows still need verification.

## Identity and permissions

One role is assigned per account initially. Server-side authorization is authoritative; Angular guards only control navigation.

| Role | Current API permissions |
|---|---|
| ADMIN | Read/manage employees; read/write suppliers, products and supplier catalogues; read and create purchase orders. |
| PURCHASING_OFFICER | Read suppliers, products, offers and orders; create purchase orders. |
| APPROVER | Read suppliers, products, offers and orders. Approval endpoints are planned. |
| WAREHOUSE_OFFICER | Read suppliers, products, offers and orders. Receipt/inventory endpoints are planned. |

All active roles can read supplier, product, catalogue and purchase-order data. Employee management and catalogue writes are restricted to ADMIN. Business state rules will remain authoritative even for admins.

## Passwords and sessions

Spring Security uses Argon2id with a 16-byte salt, 32-byte output, parallelism 1, 19 MiB memory and 2 iterations. Stored values are encoded hashes. Employee creation requires an initial password; no default public account is provided.

Signed JWT access tokens expire after 15 minutes and sessions last up to seven days. The access tokens identify the account, session and role. Protected requests validate session/account state so logout, deactivation and role changes take effect. Refresh tokens are random, stored as SHA-256 digests and rotated under a session lock. Reuse revokes the session. The refresh cookie is Secure, HttpOnly and SameSite; Angular keeps access tokens in memory. Unsafe requests require a CSRF cookie/header pair.

The login endpoint returns a generic response for invalid credentials. Rate limiting, invitation and password-reset flows, audit logging and isolated demo access are planned.

## Implemented authentication schema

Flyway V2 creates app_user, auth_session, refresh_token and account_token with role constraints and expiry/consumption fields. The schema supports invitation/reset tokens, but those flows are not exposed yet. Row-level security is enabled and direct access is revoked from Supabase client roles.

## Verification and remaining hardening

- 122 backend tests passed locally on 27 November 2026.
- CSRF behavior and API role restrictions are covered by backend tests.
- Employee/session behavior has local UI/API verification.
- Secure public demo access and cloud browser login/refresh/logout verification remain pending.

See [API contract](api-contract.md) and [cloud deployment](cloud-deployment.md).
