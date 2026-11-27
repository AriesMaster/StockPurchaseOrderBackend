package com.stockpurchaseorder.backend.purchaseorder.dto;

import com.stockpurchaseorder.backend.purchaseorder.persistence.PurchaseOrderEntity;
import com.stockpurchaseorder.backend.purchaseorder.domain.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PurchaseOrderResponse(Long id, Long supplierId, String supplierName,
    PurchaseOrderStatus status, Instant createdAt, List<Line> lines, BigDecimal total) {
    public record Line(String sku, String productName, int orderedQuantity, int receivedQuantity,
        int outstandingQuantity, BigDecimal unitPrice, BigDecimal lineTotal) {}

    public static PurchaseOrderResponse from(PurchaseOrderEntity order) {
        List<Line> lines=order.getLines().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).map(e -> {
            var line=e.getValue();
            return new Line(e.getKey(), line.getProductName(), line.getOrderedQuantity(), line.getReceivedQuantity(),
                line.getOrderedQuantity()-line.getReceivedQuantity(), line.getUnitPrice(),
                line.getUnitPrice().multiply(BigDecimal.valueOf(line.getOrderedQuantity())));
        }).toList();
        return new PurchaseOrderResponse(order.getId(),order.getSupplier().getId(),order.getSupplier().getName(),
            order.getStatus(),order.getCreatedAt(),lines,
            lines.stream().map(Line::lineTotal).reduce(BigDecimal.ZERO,BigDecimal::add));
    }
}
