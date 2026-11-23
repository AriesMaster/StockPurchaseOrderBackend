# Backend technical specification

**Status.** Proposed design for the first release. A Java prototype has exercised the purchase order rules with JUnit. Database persistence and HTTP endpoints are planned.

## Purpose and scope

The system lets a small business record what it ordered, approve the purchase, receive deliveries in stages, and review how each receipt changed stock. The first release tracks quantities by SKU at a single stock location.

## Purchase order rules

| Action | Preconditions | Result |
|---|---|---|
| Create | One or more lines with unique SKUs and positive whole quantities | Draft order |
| Approve | Draft order with at least one line | Approved order |
| Receive | Approved or partially received order. Each receipt quantity must be positive and at most the outstanding quantity for that SKU. | Partially received while any quantity remains. Received when every line is complete. |
| Cancel | Draft or approved order | Cancelled order |

An approved order cannot have its ordered quantities changed. A cancelled or fully received order cannot accept a delivery. A partially received order cannot be cancelled. For an order of 20 units, receiving 12 leaves 8 outstanding. A later request for 9 fails without changing stock.

## Backend responsibilities

| Component | Responsibility |
|---|---|
| Spring Boot controllers | Validate request structure, invoke application services, and return JSON response DTOs. |
| `PurchaseOrder` and `PurchaseOrderLine` | Own status transitions and quantity rules. The order contains its lines and exposes actions such as `approve()` and `receive(...)`. |
| Receipt service | Coordinates the order, receipt, stock movement, and inventory balance in one transaction. |
| Spring Data JPA and Hibernate | Load and persist data in PostgreSQL. |

## Logical data model

See the [database design and ERD](database-design.md) for relationships and PostgreSQL constraints.

| Record | Main fields and constraints |
|---|---|
| Product | SKU, name. SKU is unique. |
| Purchase order | ID, status, created timestamp. |
| Purchase order line | Order ID, SKU, ordered quantity, received quantity. A SKU appears once per order. Received quantity stays between zero and ordered quantity. |
| Receipt | ID, order ID, idempotency key, canonical request fingerprint, and recorded timestamp. The order ID and idempotency key are unique together. |
| Receipt line | Receipt ID, SKU, received quantity. A SKU appears once per receipt. |
| Inventory balance | SKU and quantity on hand. One balance per SKU. |
| Stock movement | ID, receipt ID, SKU, positive change in quantity, and recorded timestamp. Each receipt line produces one movement. |

## Receipt transaction

1. Lock the purchase order row and check for an existing receipt with the supplied idempotency key. Return the stored receipt for an equivalent request. Reject a different request with the same key.
2. Validate all receipt lines against the current order. Lock affected inventory balances in SKU order so receipts for different orders can safely update shared inventory.
3. Save the receipt and its lines, update received quantities and stock balances, and append movements. Commit together. Roll back all writes if any part fails.

Provision the inventory balance when creating a product. Use a database uniqueness constraint for receipt keys as a backstop. Store receipt response data needed for replay so a retry can return the original receipt even after a later delivery changes the order.

## Verification

- JUnit domain tests cover approval, partial and complete delivery, over-delivery, and cancellation after receiving stock.
- Planned PostgreSQL integration tests cover duplicate submissions, concurrent receipts against the same order, concurrent receipts affecting the same SKU across orders, and full rollback on failure.
- Planned HTTP contract tests cover validation, status codes, and replay behaviour.

[API contract](api-contract.md)
