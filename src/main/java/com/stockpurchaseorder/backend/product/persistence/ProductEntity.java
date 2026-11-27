package com.stockpurchaseorder.backend.product.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class ProductEntity {
    @Id
    @Column(name = "sku", nullable = false, updatable = false, columnDefinition = "text")
    private String sku;
    @Column(name = "name", nullable = false, columnDefinition = "text")
    private String name;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    @Column(nullable = false)
    private boolean active = true;
    // A null version identifies a new assigned-ID entity to Spring Data.
    @Version
    private Long version;

    protected ProductEntity() {}

    public ProductEntity(String sku, String name) {
        this(sku, name, BigDecimal.ZERO);
    }

    public ProductEntity(String sku, String name, BigDecimal unitPrice) {
        if (sku == null || sku.isBlank()) throw new IllegalArgumentException("SKU must not be blank.");
        this.sku = sku;
        updateDetails(name, unitPrice);
    }

    public void updateDetails(String name, BigDecimal unitPrice) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Product name must not be blank.");
        if (unitPrice == null || unitPrice.signum() < 0 || unitPrice.scale() > 2
                || unitPrice.compareTo(new BigDecimal("9999999999.99")) > 0)
            throw new IllegalArgumentException("Invalid unit price.");
        this.name = name.strip();
        this.unitPrice = unitPrice;
    }

    public void changeStatus(boolean active) { this.active = active; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public boolean isActive() { return active; }
}
