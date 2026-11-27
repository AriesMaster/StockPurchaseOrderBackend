# Backend technical specification

## Implementation checkpoint — 27 November 2026

Current feature-branch functionality includes Spring Security authentication/session endpoints, employee administration, supplier and product directories with status changes, supplier-product catalogue management, and purchase-order draft creation/list/detail APIs. Order creation validates the supplier's active product offers and persists product-name and unit-price snapshots.

Local Maven verification passed 122 tests and packaged the JAR on 27 November 2026. Flyway V1–V6 validated against the development PostgreSQL database.

Approval and cancellation endpoints, delivery receipt transactions, inventory updates, stock-movement APIs and full cloud-browser verification remain future work.

## Purpose and scope

StockFlow helps a small hardware store prepare supplier orders and keep a record of purchasing work. The current application manages supplier/product records and prices, then saves and reads draft orders. The target system will also approve orders, record staged deliveries and track on-hand stock.

## Implemented behavior

| Area | Current behavior |
|---|---|
| Product catalogue | Administrators create/update products and change active status. All active roles can search and read products. |
| Supplier management | Administrators create/update/deactivate suppliers; active roles can read/search them. |
| Supplier catalogue | Administrators assign a product to a supplier and set that supplier's unit price/status. Active roles can view offers. |
| Draft purchase orders | Admin/purchasing officer creates an order for one active supplier with one or more active offered SKUs and positive quantities. |
| Order read | All active roles can list/filter by status and view order details. |
| Price history | Order lines retain product name and supplier unit price at creation. |

## Target order lifecycle

The domain model and database include statuses DRAFT, APPROVED, PARTIALLY_RECEIVED, RECEIVED and CANCELLED. The current HTTP API creates and reads drafts. Approval, cancellation and receiving transitions are not exposed through REST endpoints yet.

| Stage | Target rule |
|---|---|
| Create | One or more unique SKU lines with positive whole-number quantities; every SKU must be actively offered by the selected supplier. Result: DRAFT. |
| Approve | Only an eligible draft can be approved. |
| Receive | Approved/partially received order only; each quantity is positive and cannot exceed outstanding quantity. |
| Cancel | Only an eligible order that has not begun receiving can be cancelled. |

## Backend responsibilities

| Component | Responsibility |
|---|---|
| Spring Security | Validate JWT/session and enforce endpoint role rules; require CSRF for unsafe methods. |
| Product, supplier and catalogue controllers/services | Validate and persist master data and supplier offers. |
| Purchase-order controller/service | Validate create/list/detail requests and save a draft with immutable name/price snapshots. |
| Domain model | Enforce status and quantity rules; only draft creation is connected to the current order REST API. |
| JPA/Hibernate and Flyway | Map current entities and apply versioned PostgreSQL schema migrations. |
| Receipt/inventory services | Planned: coordinate receipt, order quantities, stock balance and movements atomically. |

## Logical data model

See [database design and ERD](database-design.md). `ProductEntity` and `SupplierEntity` hold master records. `SupplierProduct` holds each offer. `PurchaseOrderEntity` contains `OrderLineSnapshot` values keyed by SKU. The Java `PurchaseOrder` domain model checks the order-line rules.

The initial database schema also defines receipt, receipt-line, inventory-balance and stock-movement tables. The current application does not yet create receipts or update stock.

## Draft creation transaction

1. The controller validates the supplier ID, line count and individual quantities. The service constructs a `PurchaseOrder` to check unique SKUs and the domain rules.
2. Inside one transaction, the service locks the selected supplier and checks that it is active.
3. It processes SKUs in stable order, loads the active products and looks up their active offers for that supplier.
4. It writes a draft with the product name and supplier price on each line, then returns the order details and calculates totals from the saved prices.

The request contains only the supplier ID, SKUs and quantities. Price lookup happens on the server.

## Future receipt transaction design

When receipt support is added, the service should lock the order and affected inventory rows, check outstanding quantities and an idempotency key, then save receipt lines, update order quantities and inventory, and append stock movements inside one PostgreSQL transaction. Lock affected inventory rows in SKU order. Compare the saved request fingerprint on a retry and return the original receipt for matching quantities. A failure must roll back all writes. This is not implemented or verified in the current feature branch.

## Verification

- Backend: 122 tests passed, no failures/errors/skips; Maven verification and JAR packaging succeeded on 27 November 2026.
- Frontend: production build succeeded and 123 Vitest tests passed on 27 November 2026.
- Database: Flyway V1–V6 applied/validated against the development PostgreSQL database.
- Cloud: Render health and Netlify CSRF-proxy response verified on 26 November; newest business workflows need cloud verification.

See [API contract](api-contract.md) and [database operations](database-operations.md).
