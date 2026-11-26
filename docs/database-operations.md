# Database setup and operations

## Current migration checkpoint

V1 initial business schema, V2 authentication schema and V3 supplier schema were applied and validated on 26 November 2026. The earlier failed V2 history-hardening attempt described below is historical and distinct from the current V2 authentication migration. Do not modify applied migrations. Docker Desktop with WSL 2 supports isolated PostgreSQL Testcontainers verification; the H2 API tests do not use Supabase credentials.

## Local configuration

StockFlow uses Supabase PostgreSQL 17.12 in the current development environment. Use the session pooler on IPv4 (port 5432) or an appropriate direct connection. Set environment variables outside source control:

| Variable | Value |
|---|---|
| `STOCKFLOW_DB_URL` | `jdbc:postgresql://<pooler-host>:5432/postgres?sslmode=require` |
| `STOCKFLOW_DB_USERNAME` | Session pooler username from Supabase Connect, including project identifier |
| `STOCKFLOW_DB_PASSWORD` | Database password, not an API key |

Restart the IDE after changing persistent Windows user variables. Never print or commit the password. `sslmode=require` encrypts traffic but does not verify server identity; configure certificate verification for production. Use least-privilege runtime credentials before production.

## Startup and migrations

Run `.\mvnw.cmd clean spring-boot:run` on Windows. Flyway applies V1–V3 to a fresh feature-branch database; Hibernate uses `ddl-auto=validate`. Do not edit applied migrations. V1 contains seven business tables; V2 and V3 add authentication and supplier tables. HikariCP maximum pool size is 3, minimum idle 1.

After initial migration, stop the backend and run [`scripts/database/secure_flyway_history.sql`](../scripts/database/secure_flyway_history.sql) as the database owner in a transaction outside Flyway. This enables RLS and revokes direct client privileges. V2 was attempted, timed out and rolled back; it was relocated outside the migration directory in commit `48ca162`. Modifying Flyway's own history during migration likely caused a lock conflict. Do not reintroduce that migration. Repeat the administration step for new databases.

Verify with:

```sql
SELECT version, description, success FROM public.flyway_schema_history ORDER BY installed_rank;
SELECT tablename, rowsecurity FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;
```

Expect successful V1 and RLS enabled on all eight StockFlow tables. Restart and check `http://localhost:8080/actuator/health` for `UP`.

## Verification record — 24 November 2026

- V1 validated; schema version 1 up to date.
- History security SQL executed successfully; RLS confirmed on history table.
- Hibernate initialized and Tomcat started on port 8080 in 42.228 seconds.
- Actuator returned `{"groups":["liveness","readiness"],"status":"UP"}`.
- Earlier 13 tests passed using isolated H2 configuration; PostgreSQL persistence and concurrency tests are pending.

The development database uses a privileged connection. RLS is not a substitute for application authorization. Business authentication, least-privilege roles, production TLS verification and deployment remain pending.
