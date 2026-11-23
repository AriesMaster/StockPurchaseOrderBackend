# Stock and Purchase Order System — Backend

Java and Spring Boot API for managing purchase orders and tracking stock receipts for a small business.

## Project status

This repository is being initialized. The first Java milestone models purchase orders and order lines, with JUnit tests for approval, partial deliveries, and over-delivery. The starter code will be added in a follow-up commit.

## Planned workflow

1. Create a draft purchase order with one or more items.
2. Approve the order before receiving stock.
3. Record deliveries against the outstanding quantity on each item.
4. Update inventory and record a stock movement in the same database transaction.
5. Return the original receipt when the same submission is retried, so stock is counted once.

For example, receiving 12 units against an approved order for 20 leaves 8 outstanding. The system should reject a subsequent delivery of 9 units.

## Technology

The backend uses Java 21, Spring Boot, Maven, and JUnit. Persistence will use PostgreSQL with Spring Data JPA and Hibernate.

## Documentation

- [Backend technical specification](docs/technical-specification.md)
- [Spring Boot API contract](docs/api-contract.md)

[Angular frontend](https://github.com/AriesMaster/stock-purchase-order-frontend)
