# StockFlow — Backend

Java and Spring Boot REST API for purchase order approval, delivery recording and inventory tracking for a small business.

**Status:** Active development. The Spring Boot starter, Maven wrapper, validation, Actuator and OpenAPI dependencies are committed. The initial application-context test and local Maven build passed, and Tomcat started on port 8080. Domain logic, REST controllers and database persistence are not yet implemented.

## Technology

| Component | Technology | Status |
|---|---|---|
| Language | Java 21 compilation target | Configured |
| Framework | Spring Boot 4.0.10, Spring Web MVC | Installed |
| Build | Maven Wrapper | Configured |
| Validation | Jakarta Bean Validation | Installed |
| API documentation | SpringDoc OpenAPI 3.0.3 | Installed |
| Monitoring | Spring Boot Actuator | Installed |
| Testing | JUnit, Spring Boot Test | Starter test |
| Persistence | Spring Data JPA, Hibernate, PostgreSQL | Planned |
| Migrations | Flyway | Proposed |

## Getting started

Use JDK 21 and Git. The Maven wrapper manages Maven.

```powershell
git clone https://github.com/AriesMaster/StockPurchaseOrderBackend.git
cd StockPurchaseOrderBackend
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

On Linux/macOS use `./mvnw` instead of `.\mvnw.cmd`.

| Local endpoint | Purpose |
|---|---|
| http://localhost:8080/actuator/health | Health check |
| http://localhost:8080/swagger-ui/index.html | Swagger UI |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |

These endpoints are expected from the configured dependencies; browser responses have not yet been independently verified. Swagger will have no business operations until controllers are implemented.

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
│   │   │   └── StockPurchaseOrderBackendApplication.java
│   │   └── resources/application.properties
│   └── test/java/com/stockpurchaseorder/backend/
│       └── StockPurchaseOrderBackendApplicationTests.java
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
- [ ] Domain models and JUnit business-rule tests
- [ ] PostgreSQL persistence, repositories and migrations
- [ ] Transactional receipt processing and idempotency
- [ ] REST controllers and problem responses
- [ ] Integration and concurrency tests
- [ ] Angular integration
- [ ] CI/CD and deployment

Authentication and permission rules remain open design decisions. Do not commit database credentials or other secrets.

## Documentation

- [System design](docs/system-design.md)
- [Technical specification](docs/technical-specification.md)
- [API contract](docs/api-contract.md)
- [Database design and ERD](docs/database-design.md)
- [Physical DFD](docs/physical-dfd.md)
- [Angular frontend](https://github.com/AriesMaster/StockPurchaseOrderFrontend)

See [LICENSE](LICENSE).
