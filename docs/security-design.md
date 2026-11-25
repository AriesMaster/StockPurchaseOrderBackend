# Authentication and authorization design

Status: approved direction, implementation pending. StockFlow is an internal business system; no public self-registration.

## Identity and permissions

One role per account initially. ADMIN manages users and products and can perform business operations. PURCHASING_OFFICER creates and cancels eligible orders; APPROVER approves and cancels eligible orders; WAREHOUSE_OFFICER records deliveries. All active roles may read orders, products, inventory and movements. Domain state rules apply even to administrators. Server-side authorization is authoritative; Angular guards are for navigation only.

## Passwords and sessions

Use Spring Security with Argon2id, never BCrypt. Start with 19 MiB memory, 2 iterations, parallelism 1, random 16-byte salt and 32-byte hash; benchmark on deployment hardware. Store encoded hashes only. Rate-limit authentication by account and source; use generic invalid-credentials responses. Admin creates accounts through a single-use invitation; bootstrap the first admin through an explicit one-time operator command, no default credentials.

Signed JWT access tokens expire after 15 minutes. Validate signature, issuer, audience and expiry; pin signing algorithm and keep keys outside Git. Include subject, session ID and role; check active account, session and current role on protected requests so deactivation, logout and role changes take effect immediately. Use opaque random refresh tokens, store only SHA-256 digests, rotate atomically, revoke session on reuse. Absolute session lifetime 7 days. Angular keeps access token in memory; refresh token in Secure HttpOnly SameSite cookie. Prefer same-origin deployment/proxy; protect cookie-authenticated endpoints with CSRF checks and origin validation. Never store tokens in localStorage or log them.

Password reset uses short-lived single-use random tokens, stored as digests, delivered by email; generic response regardless of account existence. Reset revokes all sessions. Audit user management, authentication events and business actions without secrets. TLS required outside local development.

## Planned schema

app_user: identity bigint ID, normalized unique email, password_hash (nullable until invitation accepted), role constrained to four values, enabled flag, created_at.
auth_session: UUID ID, user FK, expiry and revoked_at.
refresh_token: digest PK, session FK, expiry and consumed_at; retain consumed records until session expiry for replay detection.
account_token: digest PK, user FK, purpose (INVITE or RESET), expiry and consumed_at.
Use Flyway V2 and later migrations, RLS and revoked direct client grants on new tables. Never alter Flyway history inside a migration.

## Verification

Test correct/incorrect passwords, unique salts, invalid/expired JWTs, role restrictions, disabled users, session revocation, refresh replay/concurrency, reset expiry and single use, CSRF and generic error responses. Use isolated test databases, never destructive tests against development Supabase.

Implement in stages: schema and user mapping, password encoder, login and access-token validation, refresh/logout, invitation/reset, authorization and audit tests, Angular integration. No authentication endpoints are implemented yet.
