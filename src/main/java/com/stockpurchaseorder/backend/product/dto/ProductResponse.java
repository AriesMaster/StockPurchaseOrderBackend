package com.stockpurchaseorder.backend.product.dto;

import com.stockpurchaseorder.backend.product.persistence.ProductEntity;
import java.math.BigDecimal;

public record ProductResponse(String sku, String name, BigDecimal unitPrice, boolean active) {
    public static ProductResponse from(ProductEntity product) {
        return new ProductResponse(product.getSku(), product.getName(), product.getUnitPrice(), product.isActive());
    }
}
