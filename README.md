# StockFlow — Backend

Java and Spring Boot REST API for purchase order approval, delivery recording and inventory tracking for small hardware stores.

[Portfolio case study](https://thabo.netlify.app/stockflow.html)

**Status:** Active development. `main` includes purchase order domain rules, typed exceptions and problem-detail mapping, the Product JPA entity and repository, and the Flyway V1 business schema. PostgreSQL connectivity and Actuator health have been verified locally. Business REST endpoints and transactional receipt processing are planned.

## Authentication progress

The [authentication feature branch](https://github.com/AriesMaster/StockPurchaseOrderBackend/tree/feature/auth-persistence) adds the Flyway V2 authentication migration, user and session persistence, database-backed credential verification with Argon2id, signed JWT access tokens, and transactional refresh-token rotation. The [Java CI workflow passed](https://github.com/AriesMaster/StockPurchaseOrderBackend/actions/runs/45669864754) on that branch. Login and refresh HTTP endpoints and end-to-end Angular integration remain pending.

## Technology

| Component | Technology | Status |
|---|---|---|
| Language | Java 21 compilation target | Configured |
| Framework | Spring Boot 4.0.10, Spring Web MVC | Installed |
| Build | Maven Wrapper | Configured |
| Validation | Jakarta Bean Validation | Installed |
| API documentation | SpringDoc OpenAPI 3.0.3 | Installed |
| Monitoring | Spring Boot Actuator | Installed |
| Testing | JUnit, Spring Boot Test | 16 tests previously passed |
| Persistence | Spring Data JPA, Hibernate, PostgreSQL | Product mapped on `main`; authentication mappings on the feature branch |
| Migrations | Flyway | V1 applied and validated |

## Getting started

Use JDK 21 and Git. The Maven wrapper manages Maven.

```powershell
git clone https://github.com/AriesMaster/StockPurchaseOrderBackend.git
cd StockPurchaseOrderBackend
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Before starting the application, configure `STOCKFLOW_DB_URL`, `STOCKFLOW_DB_USERNAME` and `STOCKFLOW_DB_PASSWORD` in your local environment. See [database setup and operations](docs/database-operations.md). Tests use an isolated H2 profile; they do not require Supabase credentials.

On Linux/macOS use `./mvnw` instead of `.\mvnw.cmd`.

| Local endpoint | Purpose |
|---|---|
| http://localhost:8080/actuator/health | Health check |
| http://localhost:8080/swagger-ui/index.html | Swagger UI |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |

Actuator returned `{"groups":["liveness","readiness"],"status":"UP"}` on 24 November 2026. Swagger and OpenAPI browser responses have not been independently verified. Swagger will have no business operations until controllers are implemented.

## Current repository structure

```text
├── .mvn/wrapper/
├── docs/
│   ├── api-contract.md
│   ├── database-design.md
│   ├── physical-dfd.md
│   ├── system-design.md
│   └── technical-specification.md
├── src/
│   ├── main/
│   │   ├── java/com/stockpurchaseorder/backend/
│   │   │   ├── StockPurchaseOrderBackendApplication.java
│   │   │   ├── common/exception/
│   │   │   └── purchaseorder/domain/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/V1__initial_schema.sql
│   └── test/
│       ├── java/com/stockpurchaseorder/backend/
│       └── resources/application-test.properties
├── scripts/database/secure_flyway_history.sql
├── pom.xml
├── mvnw
└── mvnw.cmd
```

## Planned architecture and business rules

Angular communicates with Spring MVC controllers over HTTP/JSON. Application services coordinate domain models and transactional persistence through JPA/Hibernate and PostgreSQL.

Orders move from DRAFT to APPROVED, then PARTIALLY_RECEIVED or RECEIVED. Draft or approved orders can be cancelled before receiving stock. A receipt must not exceed outstanding quantities. Receiving 12 of 20 units leaves 8 outstanding; a subsequent receipt of 9 must fail without changing stock.

The receipt, order quantities, inventory balance and stock movements must commit atomically. Idempotency keys prevent duplicate stock updates on retries. Database locking and uniqueness constraints are planned to protect concurrent updates.

## Planned API

| Method | Path |
|---|---|
| POST | /api/purchase-orders |
| GET | /api/purchase-orders/{id} |
| POST | /api/purchase-orders/{id}/approval |
| POST | /api/purchase-orders/{id}/cancellation |
| POST | /api/purchase-orders/{id}/receipts |
| GET | /api/inventory/{sku} |
| GET | /api/stock-movements |

See the API contract for payloads, error codes and replay semantics.

## Roadmap

- [x] Spring Boot scaffold, Maven build and starter context test
- [x] Domain models and JUnit business-rule tests
- [x] Typed domain exceptions and problem-detail mapping
- [x] PostgreSQL connection, Flyway V1, connection pool and local health verification
- [x] Database RLS and history table hardening on the development database
- [x] Product JPA entity and repository; 16 tests passed
- [x] Authentication design
- [x] User schema, credential verification and session services on `feature/auth-persistence`
- [ ] Authentication HTTP endpoints and end-to-end sign-in
- [ ] Remaining JPA entities and repositories
- [ ] Transactional receipt processing and idempotency
- [ ] Business REST controllers and HTTP contract tests
- [ ] Integration and concurrency tests
- [ ] Angular integration
- [x] Automated Java CI on `feature/auth-persistence`
- [ ] Deployment

Authentication and permission rules are described in the [security design](docs/security-design.md). Do not commit database credentials or other secrets.

## Documentation

- [Security design](docs/security-design.md)
- [System design](docs/system-design.md)
- [Technical specification](docs/technical-specification.md)
- [API contract](docs/api-contract.md)
- [Database design and ERD](docs/database-design.md)
- [Database setup and operations](docs/database-operations.md)
- [Physical DFD](docs/physical-dfd.md)
- [Angular frontend](https://github.com/AriesMaster/StockPurchaseOrderFrontend)

See [LICENSE](LICENSE).
