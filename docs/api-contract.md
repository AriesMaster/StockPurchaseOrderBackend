# StockFlow API contract

This guide describes StockFlow on [`feature/auth-persistence`](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/feature/auth-persistence/docs/api-contract.md). For the original `main` code and design, see the [baseline version](https://github.com/AriesMaster/StockPurchaseOrderBackend/blob/46754bb0c9ab96d6708aa9bb2e3341d6729a8c17/docs/api-contract.md).

**Current implementation: feature/auth-persistence, 27 November 2026.** Business requests use JSON. Protected API calls require a Bearer access token. Unsafe methods, including login and business mutations, require a CSRF token. The Angular app calls the API through the same-origin /api/* proxy.

## Authentication

| Method and path | Access | Purpose | Success |
|---|---|---|---|
| `GET /api/auth/csrf` | Public | Set/return the CSRF cookie used by subsequent unsafe requests. | 204 |
| `POST /api/auth/login` | Public + CSRF | Validate credentials and create a session. | 200 |
| `POST /api/auth/refresh` | Refresh cookie + CSRF | Rotate the refresh token and return a new access token. | 200 |
| `POST /api/auth/logout` | Refresh cookie + CSRF | Revoke the session and clear the cookie. | 204 |
| `GET /api/auth/me` | Authenticated | Return the current user and role. | 200 |

Invitation acceptance, password reset, public registration and self-service account creation are not currently exposed.

## Employee administration

All employee endpoints require ADMIN.

| Method and path | Purpose | Success |
|---|---|---|
| `GET /api/employees?page=0&size=20` | Paginated employee directory | 200 |
| `GET /api/employees/{id}` | Employee details | 200 |
| `POST /api/employees` | Create an employee account | 201 |
| `PATCH /api/employees/{id}/role` | Change a role | 200 |
| `PATCH /api/employees/{id}/status` | Enable or disable an account | 200 |

## Suppliers and supplier catalogues

All active roles can read suppliers and their catalogues. Only `ADMIN` can change them. Supplier requests accept `name` (required, up to 150 characters), `contactName`, `email`, `phone` and `address`. The optional `active` query parameter filters the supplier directory.

| Method and path | Purpose | Success |
|---|---|---|
| `GET /api/suppliers?page=0&size=20&search={text}&active=true` | Search/paginate suppliers | 200 |
| `GET /api/suppliers/{id}` | Supplier details | 200 |
| `POST /api/suppliers` | Create a supplier | 201 |
| `PUT /api/suppliers/{id}` | Update supplier details | 200 |
| `PATCH /api/suppliers/{id}/status` | Activate/deactivate a supplier | 200 |
| `GET /api/suppliers/{id}/products?available=true&search={text}&page=0&size=10` | List the supplier's product offers | 200 |
| `PUT /api/suppliers/{id}/products/{sku}` | Create/update an offer's price and active status | 200 |

A catalogue response contains the SKU, product name, supplier unit price, offer status and product status. An offer requires a non-negative price with up to two decimals. `available=true` returns offers where the supplier, product and offer are all active. Use `available=false` to include inactive entries. The update body is, for example, `{"unitPrice":12.50,"active":true}`.

## Products

All active roles can read products. Only ADMIN can create or change products.

| Method and path | Purpose | Success |
|---|---|---|
| `GET /api/products?page=0&size=20&search={text}&active=true` | Search/paginate products | 200 |
| `GET /api/products/{sku}` | Product details | 200 |
| `POST /api/products` | Create a SKU, name and base unit price | 201 |
| `PUT /api/products/{sku}` | Update name and base price | 200 |
| `PATCH /api/products/{sku}/status` | Activate/deactivate a product | 200 |

The product base price is separate from each supplier's catalogue price. The SKU uses letters, digits, dots, underscores or hyphens and is immutable after creation.

## Purchase orders

All active roles can list and read orders. ADMIN and PURCHASING_OFFICER can create draft orders. List pagination defaults to page 0 and size 20; size is limited to 100. The optional status filter accepts DRAFT, APPROVED, PARTIALLY_RECEIVED, RECEIVED or CANCELLED.

| Method and path | Purpose | Success |
|---|---|---|
| `GET /api/purchase-orders?page=0&size=20&status=DRAFT` | List/filter orders | 200 |
| `GET /api/purchase-orders/{id}` | Order details | 200 |
| `POST /api/purchase-orders` | Create a draft order | 201 |

Example request:

```json
{
  "supplierId": 3,
  "lines": [
    { "sku": "SCREW-BOX", "orderedQuantity": 20 },
    { "sku": "PAINT-20L", "orderedQuantity": 4 }
  ]
}
```

The request requires one positive `supplierId`, 1–100 lines with distinct SKUs, a nonblank SKU (maximum 80 characters) and a positive whole-number quantity for each line. The server rejects an inactive/missing supplier, unknown/inactive product or product without an active offer in that supplier's catalogue. The client cannot set the order-line prices. The server saves the current product name and supplier unit price and calculates line totals and the order total. The response includes `id`, `supplierId`, `supplierName`, `status`, `createdAt`, `lines` and `total`. Each line includes `sku`, `productName`, `unitPrice`, `orderedQuantity`, `receivedQuantity`, `outstandingQuantity` and `lineTotal`. New drafts have zero received and the full ordered quantity outstanding.

## Common errors

| Status | Meaning |
|---|---|
| 400 | Invalid input, pagination or query |
| 401 | Missing or invalid authentication |
| 403 | Insufficient role or missing/invalid CSRF token on a write |
| 404 | Requested employee, supplier, product or order does not exist |
| 409 | Duplicate/conflicting state, such as an inactive supplier, product or offer |
| 422 | A purchase order references a supplier, SKU or supplier offer that cannot be used |

Responses use Spring's API error/problem response format. Exact error details depend on the endpoint.

## Planned procurement endpoints

Approval, cancellation, delivery receiving and inventory APIs are not yet implemented.

| Planned method and path | Purpose |
|---|---|
| `POST /api/purchase-orders/{id}/approval` | Approve a draft |
| `POST /api/purchase-orders/{id}/cancellation` | Cancel an eligible order |
| `POST /api/purchase-orders/{id}/receipts` | Record partial/full quantities received |
| `GET /api/inventory/{sku}` | Read current stock |
| `GET /api/stock-movements?sku={sku}` | Read inventory change history |

The receipt design calls for an `Idempotency-Key` header on each delivery. Retrying the same key with the same SKU quantities would return the stored receipt with `200 OK`, even after a later delivery. Reusing the key with different lines would return `409 Conflict`. A receipt for an approved order of 20 units could add 12 and leave 8 outstanding. A request to receive 9 more should be rejected atomically. This behavior is a target design only.
