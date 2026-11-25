package com.stockpurchaseorder.backend.product.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product")
public class ProductEntity {

    @Id
    @Column(name = "sku", nullable = false, updatable = false, columnDefinition = "text")
    private String sku;

    @Column(name = "name", nullable = false, columnDefinition = "text")
    private String name;

    protected ProductEntity() {
        // Required by JPA.
    }

    public ProductEntity(String sku, String name) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU must not be blank.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name must not be blank.");
        }
        this.sku = sku;
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }
}
