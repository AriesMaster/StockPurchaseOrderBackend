# Stock and Purchase Order System Design

This guide describes StockFlow on [`feature/auth-persistence`](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/feature/auth-persistence/docs/system-design.md). For the original `main` code and design, see the [baseline version](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/46754bb0c9ab96d6708aa9bb2e3341d6729a8c17/docs/system-design.md).

## Implementation checkpoint — 27 November 2026

The [Angular development branch](https://github.com/AriesMaster/StockPurchaseOrderFrontend/tree/feature/auth-ui) and [Spring Boot development branch](https://github.com/AriesMaster/StockPurchaseOrderBackend/tree/feature/auth-persistence) implement authentication and employee administration, supplier and product management, supplier-specific product catalogues, and draft purchase orders in Angular and Spring Boot. Administrators can maintain suppliers, products and supplier offers. Administrators and purchasing officers can create draft orders; all active roles can filter the order list and read order details. Draft lines save product-name and supplier-price snapshots.

On 27 November 2026, `mvnw clean verify` passed 122 backend tests and packaged the executable JAR. The Angular production build succeeded with 123 passing Vitest tests across 16 files. Supabase’s development database recorded successful migrations V1–V6.

**Next milestones** are order approval and cancellation, followed by receiving, stock updates and end-to-end verification.

## Purpose and scope

StockFlow is being built for a small hardware store that needs to record what it wants to buy, which supplier offers each item, what has arrived, what remains due and how deliveries change stock. The current implementation covers product and supplier setup, supplier-specific prices and draft purchase orders. Approval, goods receiving and inventory tracking are the next workflow stages.

## Architecture

| Component | Current responsibility |
|---|---|
| Angular frontend | Authentication and protected workspace; employee, supplier and product directories; supplier catalogue management; draft order creation, order listing/filtering and details. |
| Spring Boot API | Login/session endpoints; employee and catalogue administration; product/supplier APIs; draft purchase-order validation and persistence. |
| Spring Data JPA and Hibernate | Map users, products, suppliers, supplier offers and orders to PostgreSQL. |
| PostgreSQL | Store account/session data, product and supplier catalogues, purchase orders and the schema for future receipts/inventory. |

## Current purchase-order workflow

1. An administrator creates a product master record with a SKU and name, then adds it to one or more supplier catalogues with a supplier-specific unit price.
2. An administrator or purchasing officer chooses one active supplier. The interface shows only that supplier's active product offers.
3. The user adds one or more products, enters positive whole-number quantities and creates a draft order.
4. The API validates the active supplier, products and offers. It saves the product name and unit price on the order line, so later catalogue changes do not alter the draft.
5. All active roles can list orders, filter by status and view order details.

A purchase order is one request to one supplier. It can have several product lines. A product from one supplier cannot be added to a different supplier's order unless that supplier also has an active catalogue offer for it.

## Future approval, receiving and inventory flow

The intended next workflow is: purchasing officer creates a draft → approver approves → warehouse officer records deliveries → inventory balances and movements update. These later endpoints and screens are not implemented.

The target receiving example is an approved order for 20 units. Recording 12 would leave 8 outstanding. A receipt that tries to add 9 would be rejected. Receipt idempotency, database locking and atomic inventory changes are still design work, not current behavior.

## Verification and next steps

**Verified locally on 27 November 2026**

- Backend: 122 tests passed; Maven verification and JAR packaging succeeded.
- Frontend: production build succeeded; 123 Vitest tests passed across 16 files.
- Database: V1–V6 migrations validated on the development PostgreSQL database.
- Browser evidence: the local order directory displayed two saved drafts before the supplier picker was changed. A complete walkthrough with the new catalogue picker remains outstanding.

**Next:** verify the new flows in a browser, add dedicated purchase-order UI/end-to-end tests, implement order approval and cancellation, then add transactional receipts and inventory movement processing.

## Detailed designs

- [Physical data flow diagram](physical-dfd.md) for the implemented and planned browser/API/database paths.
- [Backend technical specification](technical-specification.md) for current API responsibilities and future receipt behavior.
- [Spring Boot API contract](api-contract.md) for current routes, role rules and planned endpoints.
- [Database design and ERD](database-design.md) for migrations V1–V6 and the target receipt/inventory schema.
- [Security design](security-design.md) for sessions, CSRF and current endpoint permissions.
- [Angular frontend](https://github.com/AriesMaster/StockPurchaseOrderFrontend/tree/feature/auth-ui)

## Authentication and authorization

Spring Security validates short-lived JWT access tokens against persisted sessions. Refresh tokens rotate through a secure cookie, and unsafe requests require CSRF. The server restricts employee, supplier, product, supplier-catalogue and purchase-order operations by role. See [security design](security-design.md).

## Cloud topology (deployed preview)

Browser → Netlify Angular SPA → HTTPS API proxy → Render Docker Spring Boot → JDBC/TLS → Supabase PostgreSQL.

Render health returned UP and the Netlify CSRF proxy response was verified on 26 November 2026. The latest product/catalogue/order changes passed local verification on 27 November; their deployment and full cloud browser workflows remain unverified. See [cloud deployment and operations](cloud-deployment.md).
