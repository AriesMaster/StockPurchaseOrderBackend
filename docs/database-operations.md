# Database setup and operations

This guide describes StockFlow on [`feature/auth-persistence`](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/feature/auth-persistence/docs/database-operations.md). For the original `main` code and design, see the [baseline version](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/46754bb0c9ab96d6708aa9bb2e3341d6729a8c17/docs/database-operations.md).

## Current migration checkpoint — 27 November 2026

The active backend branch contains Flyway migrations V1–V6. The development Supabase query against `public.flyway_schema_history` showed six successful migrations, including V6 (`supplier product catalogue`). Do not edit or reorder applied migrations.

| Version | Change |
|---|---|
| V1 | Initial product, purchase-order, receipt and inventory schema (seven business tables). |
| V2 | Authentication tables and role constraints. |
| V3 | Supplier table. |
| V4 | Product price, active and version columns. |
| V5 | Supplier reference and name/price snapshots for purchase orders. |
| V6 | Supplier-product catalogue and supplier-specific prices. |

The earlier failed schema-history hardening attempt is historical and separate from the current V2 authentication migration. The Flyway history security script remains an operator-run database administration step; never modify Flyway's history from a migration.

## Local configuration

StockFlow uses Supabase PostgreSQL 17.12 in the current development environment. Use the session pooler on IPv4 (port 5432) or an appropriate direct connection. Set environment variables outside source control:

| Variable | Value |
|---|---|
| STOCKFLOW_DB_URL | `jdbc:postgresql://<pooler-host>:5432/postgres?sslmode=require` |
| STOCKFLOW_DB_USERNAME | Session pooler username from Supabase Connect, including project identifier |
| `STOCKFLOW_DB_PASSWORD` | Database password |
| `STOCKFLOW_JWT_SECRET_BASE64` | Base64-encoded signing key that decodes to at least 32 bytes. Required for API startup. |

Restart the IDE after changing persistent Windows user variables. Never print or commit the password. sslmode=require encrypts traffic but does not verify server identity; configure certificate verification for production. Use least-privilege runtime credentials before production.

## Startup and migrations

Run `.\mvnw.cmd spring-boot:run` on Windows. Flyway applies V1–V6 to a fresh feature-branch database; Hibernate uses ddl-auto=validate. HikariCP maximum pool size is 3, minimum idle 1.

After initial migration, stop the backend and run the [history security script](../scripts/database/secure_flyway_history.sql) as the database owner in a transaction outside Flyway. This enables RLS and revokes direct client privileges on Flyway's history table. The script is separate from schema migrations to avoid migration-history lock conflicts. Repeat the administration step for new databases.

Verify with:

```sql
SELECT version, description, success FROM public.flyway_schema_history ORDER BY installed_rank;
SELECT tablename, rowsecurity FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;
```

Expect V1 through V6 to report success = true. Review the `rowsecurity` flag for the application tables and the Flyway history table. Restart and check http://localhost:8080/actuator/health for UP.

## Verification record — 27 November 2026

- Supabase PostgreSQL 17.12: the database history query showed V1–V6 with `success = true`.
- Backend: Maven clean verify passed 122 tests with zero failures, errors or skips and packaged the JAR.
- The local startup log recorded Hibernate initialization and port 8080 while the schema was at V5. The later Supabase history query confirmed V6.
- Local tests use isolated test configuration; no secrets or production data belong in test fixtures.

The development database connection currently uses a privileged role. RLS is not a substitute for application authorization. Review least-privilege credentials, production TLS verification, latest cloud deployment and browser workflows before a public release.
