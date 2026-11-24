# Physical data flow diagram

**Status.** Target Level 1 physical DFD for the first release. Angular public shell, Spring Boot runtime and PostgreSQL schema exist; the business data flows shown are not yet operational. It shows the implementation path for order approval, receipt processing, and inventory queries.

```mermaid
flowchart TB
    Staff["Purchasing and receiving staff"]
    Approver["Order approver"]
    Browser("1.0 Angular screens in browser")
    Order("2.0 Spring Boot order API")
    Receipt("3.0 Spring Boot receipt API")
    Inventory("4.0 Spring Boot inventory API")
    DB[("D1 PostgreSQL")]
    
    Staff -->|"Item quantities, delivery details, stock filters"| Browser
    Approver -->|"Order ID and approval action"| Browser
    Browser -->|"Order status, receipt confirmation and stock views"| Staff
    Browser -->|"Order details and approval outcome"| Approver

    Browser -->|"HTTPS: order details and approval requests"| Order
    Order -->|"JSON: order details, status or validation errors"| Browser
    Browser -->|"HTTPS: received quantities and idempotency key"| Receipt
    Receipt -->|"JSON: receipt, outstanding quantities or errors"| Browser
    Browser -->|"HTTPS: SKU and stock movement queries"| Inventory
    Inventory -->|"JSON: stock balances and movements"| Browser

    Order -->|"SQL: order and line writes"| DB
    DB -->|"Order and product rows"| Order
    Receipt -->|"SQL: receipt, order, balance and movement writes"| DB
    DB -->|"Locked order and balance rows, stored receipt"| Receipt
    Inventory -->|"SQL: balance and movement queries"| DB
    DB -->|"Balance and movement rows"| Inventory
```

## Implementation notes

- The three Spring Boot processes represent responsibilities inside one application. Spring Data JPA and Hibernate handle database access.
- A receipt and its inventory changes commit together. A valid retry returns the stored receipt without creating another movement.
- PostgreSQL is the persistent data store. Its tables and keys are defined in the [database design and ERD](database-design.md).
- Staff and approver labels describe who handles each workflow step. Authentication, hosting, and network topology are still open design decisions.

[System design](system-design.md) · [API contract](api-contract.md)
