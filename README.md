# StockFlow — Backend

StockFlow’s Java 21 and Spring Boot API manages a hardware store’s suppliers, product catalogues and draft purchase orders. Each order records one supplier and the agreed price of every item.

The feature and test descriptions below refer to [feature/auth-persistence](https://github.com/AriesMaster/StockPurchaseOrderBackend/tree/feature/auth-persistence). The `main` branch contains the initial order domain and V1 database schema.

[Portfolio case study](https://thabo.netlify.app/stockflow.html) · [Development preview](https://stockflow-management.netlify.app/) · [API health](https://stockflow-backend-5cke.onrender.com/actuator/health) · [Angular workspace](https://github.com/AriesMaster/StockPurchaseOrderFrontend/tree/feature/auth-ui)

## How draft orders work

A product has a SKU and a base price. Each supplier can offer the same product at a different price. An administrator links the product to a supplier in `supplier_product` and sets its purchasing price. For example, 20 boxes at R12.50 each gives a draft line worth **R250.00**.

A draft accepts one active supplier and 1–100 distinct SKUs with positive whole quantities. The API checks each product and offer, reads the price from that supplier’s catalogue and stores the product name and price on the order line. Later catalogue edits leave the saved total intact. Draft creation has no effect on inventory.

## Implemented API

| Resource | Operations | Permissions |
|---|---|---|
| `/api/auth` | CSRF, login, refresh, logout and current user | JWT access tokens, sessions and rotating refresh cookies |
| `/api/employees` | Create, list, read, change role or enabled status | Administrator |
| `/api/suppliers`, `/api/products` | List, search, read, create, edit, activate or deactivate | All active roles read. Administrator writes. |
| `/api/suppliers/{id}/products` | Read offers, assign an SKU or change its price and availability | All active roles read. Administrator writes. |
| `/api/purchase-orders` | Create a draft, list orders with a status filter and read one by ID | All active roles read. Administrator and purchasing officer create. |

The four roles are `ADMIN`, `PURCHASING_OFFICER`, `APPROVER` and `WAREHOUSE_OFFICER`. Passwords use Argon2id. Spring Security validates token and session state and checks CSRF on writes. Employee role and status changes revoke affected sessions. [API contract](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/feature/auth-persistence/docs/api-contract.md) · [Security design](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/feature/auth-persistence/docs/security-design.md)

## Getting started

Use JDK 21 and the Maven wrapper. For a local application run, set `STOCKFLOW_DB_URL`, `STOCKFLOW_DB_USERNAME`, `STOCKFLOW_DB_PASSWORD` and `STOCKFLOW_JWT_SECRET_BASE64`. The signing key is Base64-encoded and must decode to at least 32 bytes. The test suite uses isolated H2 and Docker-backed PostgreSQL 17. Start Docker before running Maven verification. [Database setup](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/feature/auth-persistence/docs/database-operations.md)

```powershell
git clone https://github.com/AriesMaster/StockPurchaseOrderBackend.git
cd StockPurchaseOrderBackend
git switch feature/auth-persistence
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

On Linux or macOS, use `./mvnw`. The local health endpoint is `http://localhost:8080/actuator/health`. The cloud profile disables public OpenAPI. See the [documented routes and payloads](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/feature/auth-persistence/docs/api-contract.md).

## Repository structure

```text
src/main/java/com/stockpurchaseorder/backend/
├── auth/             # Credentials, sessions and employees
├── common/           # Paging and error responses
├── product/          # Product directory
├── supplier/         # Suppliers and supplier-specific offers
└── purchaseorder/    # Domain rules, draft persistence, DTOs and controller
src/main/resources/db/migration/  # Flyway V1–V6
src/test/                        # JUnit, MockMvc and PostgreSQL integration tests
docs/                            # Design, API contract, ERD and operations
```

## Verification and next work

On 27 November 2026 local Maven verification packaged the JAR. **122 backend tests passed with zero failures, errors or skips** at [6ee2a52](https://github.com/AriesMaster/StockPurchaseOrderBackend/commit/6ee2a5239d1df2c7fc6c59ce1595e2f16552385c). The matching Angular production build succeeded with **123 passing tests**. Supabase’s development migration history recorded **V1–V6** successfully.

Order approval and cancellation are the next API stages. Domain tests already exercise state transitions and quantity rules. Implementing the receiving workflow will let a warehouse officer record a partial delivery, update stock and see what is still due. For an order of 20, receiving 12 would leave 8 outstanding. The design covers receipt retries, locking and transaction rollback. [Technical specification](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/feature/auth-persistence/docs/technical-specification.md)

## Cloud deployment and containerization

GitHub Actions runs Maven verification. A multi-stage Dockerfile builds the JAR with JDK 21 and runs it with a non-root Java 21 runtime on Render. Netlify serves Angular and proxies API traffic to Render. Spring Data JPA and Hibernate connect the API to Supabase PostgreSQL, with Flyway managing the schema. Local Docker Desktop uses WSL 2 on Windows.

Render uses `SPRING_PROFILES_ACTIVE=cloud`, its provided `PORT`, and the database and signing-key variables above. The published API returned `UP` and the Netlify CSRF proxy responded on 26 November 2026. The new catalogue-to-draft workflow still needs a hosted browser check. [Cloud deployment](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/feature/auth-persistence/docs/cloud-deployment.md)

## Documentation

- [Security design](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/feature/auth-persistence/docs/security-design.md)
- [System design](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/feature/auth-persistence/docs/system-design.md)
- [Technical specification](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/feature/auth-persistence/docs/technical-specification.md)
- [API contract](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/feature/auth-persistence/docs/api-contract.md)
- [Database design and ERD](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/feature/auth-persistence/docs/database-design.md)
- [Database setup and operations](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/feature/auth-persistence/docs/database-operations.md)
- [Physical DFD](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/feature/auth-persistence/docs/physical-dfd.md)
- [Angular frontend](https://github.com/AriesMaster/StockPurchaseOrderFrontend/tree/feature/auth-ui)

See [LICENSE](LICENSE).
