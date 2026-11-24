# Spring Boot API contract

**Status.** Proposed business endpoints and payloads for Angular integration. Global domain exception mapping is implemented and unit-tested; business controllers and HTTP contract tests are pending.

## Endpoints

| Method and path | Purpose | Success |
|---|---|---|
| `POST /api/purchase-orders` | Create a draft order with an array of lines containing SKU and ordered quantity. | `201 Created` |
| `GET /api/purchase-orders/{id}` | Read order status and each line's ordered, received, and outstanding quantities. | `200 OK` |
| `POST /api/purchase-orders/{id}/approval` | Approve a draft order. | `200 OK` |
| `POST /api/purchase-orders/{id}/cancellation` | Cancel a draft or approved order. | `200 OK` |
| `POST /api/purchase-orders/{id}/receipts` | Record one delivery against the order. | `201 Created` for a new receipt, `200 OK` for a replay |
| `GET /api/inventory/{sku}` | Read current stock on hand for one SKU. | `200 OK` |
| `GET /api/stock-movements?sku={sku}` | Read recorded movements, optionally filtered by SKU. | `200 OK` |

## Record a delivery

Send a non-empty `Idempotency-Key` header that identifies this submission for the order. Generate a new key for each new delivery. Reuse the same key and quantities when retrying. A receipt must contain at least one line, with each SKU appearing once.

```http
POST /api/purchase-orders/42/receipts
Content-Type: application/json
Idempotency-Key: 0816f101-c3bc-4b77-b82f-14df884a1364
```

```json
{
  "lines": [
    { "sku": "PAINT-20L", "quantity": 12 }
  ]
}
```

For an approved order of 20 units, the response describes the receipt and the state immediately after applying it.

```json
{
  "id": 101,
  "purchaseOrderId": 42,
  "orderStatus": "PARTIALLY_RECEIVED",
  "lines": [
    { "sku": "PAINT-20L", "quantity": 12, "outstandingQuantity": 8 }
  ]
}
```

The same key with equivalent SKU quantities returns the original receipt body with `200 OK`. Input line order is irrelevant. The same key with different quantities or SKUs returns `409 Conflict`.

## Errors

| Status | Example |
|---|---|
| `400 Bad Request` | Missing receipt key, empty lines, duplicate SKU, or non-positive quantity. |
| `404 Not Found` | Order or requested inventory SKU does not exist. |
| `409 Conflict` | Order cannot accept a receipt in its current state, quantity exceeds what is outstanding, or an idempotency key is reused with different data. |
| `422 Unprocessable Entity` | A purchase order references an unknown SKU or a receipt references a SKU absent from the order. |

Errors use `application/problem+json` with a human-readable detail and a stable code such as `QUANTITY_EXCEEDS_OUTSTANDING` for Angular to handle.

[Backend technical specification](technical-specification.md)
